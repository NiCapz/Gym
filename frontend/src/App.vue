<template>
  <main>
    <div>
      <button @click="toggleRecording">{{ recordButtonText }}</button><br>
      <hr class="ruler">
      <li v-for="transcriptionInList in transcriptionList">
        <button class="playButton" @click="playAudio(transcriptionInList)">
          <span>{{ transcriptionInList }}</span>
        </button>
      </li><br>
      <div class="liveTranscript" v-if="transcription">
        <button class="playButton" @click="playAudio(transcriptionInList)">
          <span>{{ transcription }}</span>
        </button>
      </div>
    </div>
  </main>
</template>



<script setup>
// extandable media Recorder is required since the normal one doesnt support wav
// this is easier than conversion from Webm
import { MediaRecorder, register } from 'extendable-media-recorder'
import { connect } from 'extendable-media-recorder-wav-encoder'
import { Client } from '@stomp/stompjs'
import { Icon } from '@iconify/vue';
import { Speechify } from '@speechify/api-sdk'
</script>



<script>
export default {
  data() {
    return {
      mediaRecorder: null,
      isRecording: false,
      audioChunks: [],
      capturedStream: null,
      transcription: '',
      transcriptionList: [],
      stream: null,
      sampleRate: null,
      transcribeURL: 'http://localhost:8080/transcribeFull',

      client: null,
      socket: null,
      sessionId: '1',
      connected: false,

      speech: null,

      recordButtonText: 'Start Recording',

      ttsSessionId: '',
    }
  },

  created() {

    // creating Websocket
    this.client = new Client({
      webSocketFactory: () => new WebSocket('ws:localhost:8080/transcription-websocket'),
      reconnectDelay: 5000,
      onConnect: () => {
        this.subscribeToTranscriptions();
      }
    });
    this.client.activate();

    this.speech = new SpeechSynthesisUtterance();
    this.speech.lang = "de-DE";
  },

  methods: {



    playAudio(text) {
      this.speech.text = text;
      window.speechSynthesis.speak(this.speech);
    },

    toggleRecording() {
      if (!this.isRecording) {
        this.startRecording();
      }
      else {
        this.stopRecording();
      }
    },

    async toMono(blob) {
      const audioContext = new (window.AudioContext || window.webkitAudioContext)();
      this.sampleRate = audioContext.sampleRate;
      console.log(`Samplerate: ${this.sampleRate}`);

      // Decode audio data from the blob
      const arrayBuffer = await blob.arrayBuffer();
      const audioBuffer = await audioContext.decodeAudioData(arrayBuffer);

      // Create a mono buffer and mix channels
      const monoBuffer = audioContext.createBuffer(
        1, // Mono
        audioBuffer.length,
        audioBuffer.sampleRate
      );
      const monoData = monoBuffer.getChannelData(0);

      // Mix all channels into the mono channel
      for (let channel = 0; channel < audioBuffer.numberOfChannels; channel++) {
        const channelData = audioBuffer.getChannelData(channel);
        for (let i = 0; i < channelData.length; i++) {
          monoData[i] += channelData[i] / audioBuffer.numberOfChannels;
        }
      }

      // Encode the mono buffer back into a WAV Blob
      return this.audioBufferToWavBlob(monoBuffer);
    },

    audioBufferToWavBlob(buffer) {
      const length = buffer.length * 2 + 44;
      const wav = new DataView(new ArrayBuffer(length));

      // WAV header
      const writeString = (offset, str) =>
        [...str].forEach((c, i) => wav.setUint8(offset + i, c.charCodeAt(0)));
      writeString(0, "RIFF");
      wav.setUint32(4, length - 8, true);
      writeString(8, "WAVE");
      writeString(12, "fmt ");
      wav.setUint32(16, 16, true); // PCM format
      wav.setUint16(20, 1, true);
      wav.setUint16(22, 1, true); // Mono
      wav.setUint32(24, buffer.sampleRate, true);
      wav.setUint32(28, buffer.sampleRate * 2, true);
      wav.setUint16(32, 2, true); // Block align
      wav.setUint16(34, 16, true); // Bits per sample
      writeString(36, "data");
      wav.setUint32(40, buffer.length * 2, true);

      // PCM data
      const floatData = buffer.getChannelData(0);
      let offset = 44;
      for (let i = 0; i < floatData.length; i++) {
        const sample = Math.max(-1, Math.min(1, floatData[i])); // Clamp
        wav.setInt16(offset, sample < 0 ? sample * 0x8000 : sample * 0x7fff, true);
        offset += 2;
      }

      return new Blob([wav], { type: "audio/wav" });
    },

    subscribeToTranscriptions() {
      // this endpoint is set up in Spring/WebSocketConfig
      // might want to add another endpoint in the future for AI and maybe partial transcriptions
      // sessionId should be used in the future, not now tho
      this.client.subscribe(`/topic/transcriptions/${this.sessionId}`, message => {
        const result = message.body;
        this.transcription = result;
        this.transcriptionList.push(this.transcription);
        this.transcription = '';
      });

      this.client.subscribe(`/topic/partialTranscriptions/${this.sessionId}`, message => {
        const partialResult = message.body;
        this.transcription = partialResult;
      });
    },

    disconnectWebSocket() {
      if (this.client) {
        this.client.deactivate();
      }
    },

    async startRecording() {
      if (!this.isRecording) {
        this.isRecording = true;
        this.recordButtonText = 'Stop Recording';
        // empty audioChunks array
        this.audioChunks = [];

        // necessary, could do this in the created() method but am worried about making it async
        // can only happen once per session
        if (!this.connected) {
          this.connected = true;
          await register(await connect());
        }

        this.stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        // make sure this uses the right (extendable-) MediaRecorder, otherwise theres trouble
        this.mediaRecorder = new MediaRecorder(this.stream, { mimeType: 'audio/wav' });

        this.mediaRecorder.ondataavailable = (event) => {
          this.audioChunks.push(event.data);
        }

        this.mediaRecorder.start(50);

      }
    },

    async stopRecording() {
      if (this.isRecording) {
        this.isRecording = false;
        this.recordButtonText = 'Start Recording';
        this.mediaRecorder.stop();
        // biome-ignore lint/complexity/noForEach: <explanation>
        this.stream.getTracks().forEach((track) => track.stop());

        let audioBlob = new Blob(this.audioChunks, { type: this.mediaRecorder.mimeType });
        audioBlob = await this.toMono(audioBlob);
        this.sendAudio(audioBlob);

        this.mediaRecorder = null;
        this.stream = null;
      }
    },

    sendAudio(audio) {
      const formData = new FormData();
      formData.append('audio', audio);
      formData.append('sessionId', '1');
      formData.append('sampleRate', this.sampleRate);

      fetch(this.transcribeURL, {
        method: "POST",
        cache: "no-cache",
        body: formData
      });
    },
  }
}
</script>