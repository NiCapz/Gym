<template>
  <main>
    <div>
      <button @click="startRecording">Start Recording</button>
      <button @click="stopRecording">Stop Recording</button>
      <p>transcription: {{ this.transcription }}</p>
      <a>recording: {{ this.isRecording }}</a>
    </div>
  </main>
</template>



<script>
import { Client } from "@stomp/stompjs"
import { MediaRecorder, register } from 'extendable-media-recorder'
import { connect } from 'extendable-media-recorder-wav-encoder'

export default {
  data() {
    return {
      mediaRecorder: null,
      audioChunks: [],
      isRecording: false,

      sessionId: '1',
      client: null,
      transcription: '',
      socket: null,

      audioContext: null,

      apiLink: 'http://localhost:8080/transcribe/complete',
    }
  },

  created() {
    this.client = new Client({
      webSocketFactory: () => new WebSocket("ws://localhost:8080/transcription-websocket"),
      reconnectDelay: 5000,
      onConnect: () => {
        this.subscribeToTranscriptions();
      }
    });
    this.client.activate();

    if (!this.audioContext) {
      this.audioContext = new (window.AudioContext)();
    }
  },

  beforeUnMount() {
    this.disconnectWebSocket();
  },

  methods: {

    async downmixToMono(audioChunk) {
      const arrayBuffer = await audioChunk.arrayBuffer();
      const audioBuffer = await this.audioContext.decodeAudioData(arrayBuffer);

      const monoBuffer = this.audioContext.createBuffer(
        1,
        audioBuffer.length,
        48000
      );
      console.log(audioBuffer.numberOfChannels);
      const inputChannel = audioBuffer.getChannelData(0);
      const outputChannel = monoBuffer.getChannelData(0);

      for (let i = 0; i < inputChannel.length; i += 2) {
        outputChannel[i] = (inputChannel[i] + inputChannel[i + 1]) / 2
      }

      return monoBuffer;
    },

    subscribeToTranscriptions() {
      this.client.subscribe(`/topic/transcriptions/${this.sessionId}`, (message) => {
        const result = message.body;
        this.transcription += `${result} `;
      });
    },

    disconnectWebSocket() {
      if (this.client) {
        this.client.deactivate();
      }
    },

    async startRecording() {

      await register(await connect());

      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });

      //const audioContext = new(window.AudioContext)();
      //const source = this.audioContext.createMediaStreamSource(stream);

      this.mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/wav' });

      this.mediaRecorder.ondataavailable = (event) => {
        //let downmixedChunk = this.downmixToMono(event.data);
        //this.audioChunks.push(downmixedChunk);
        this.audioChunks.push(event.data);
        // we actually use this (hopefully)
        //this.sendChunk(event.data);
      }

      this.mediaRecorder.start();
      this.isRecording = true;
    },

    async stopRecording() {
      this.mediaRecorder.stop();
      this.isRecording = false;

      // tf is a Blob
      const audioBlob = new Blob(this.audioChunks, { mimeType: 'audio/wav' });
      this.sendCompleteAudio(audioBlob);
    },

    async sendChunk(chunk) {
      const formData = new FormData();
      formData.append('audioChunk', chunk);
      formData.append('sessionId', '1');

      await fetch('http://localhost:8080/transcribe/chunk', {
        method: 'POST',
        body: formData,
      });
    },

    async sendCompleteAudio(audioBlob) {
      const formData = new FormData();
      formData.append('audio', audioBlob, 'audio.wav'); // Use a descriptive name
      formData.append('sessionId', '1');

      await fetch(this.apiLink, {
        method: 'POST',
        body: formData,
      })
    }
  }
}
</script>