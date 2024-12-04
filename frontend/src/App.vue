<template>
  <main>
    <div>
      <button @click="toggleRecording">{{ recordButtonText }}</button><br>
      <hr class="ruler">
      <li v-for="item in convo">
        <div>
          <p>{{ item[0] }}</p>
          <p>{{ item[1] }}</p>
        </div>
      </li>
      <div v-if="transcription">
        <p>{{ transcription }}</p>
        <p>{{ reply }}</p>
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
</script>



<script>
export default {
  data() {
    return {
      mediaRecorder: null,
      isRecording: false,
      audioChunks: [],
      transcription: '',
      transcriptionList: [],
      stream: null,
      sampleRate: null,
      reply: '',
      audio: null,
      convo: [],
      client: null,
      transcribeURL: 'http://localhost:8080/process-audio',

      recordButtonText: 'Start Recording',
    }
  },

  created() {
    this.client = new Client({
      webSocketFactory: () => new WebSocket("ws://localhost:8080/backend-websocket"),
      reconnectDelay: 5000,
      onConnect: () => { this.subscribeToEndpoints(); },
      debug: (str) => {
        console.log(str);
      }
    })
    this.client.activate();
  },

  beforeUnMount() {
    this.disconnectWebSocket();
  },

  methods: {

    disconnectWebSocket() {
      if(this.client) {
        this.client.deactivate();
      }
    },

    subscribeToEndpoints() {
      this.client.subscribe('/topic/transcription', (message) => {
        this.transcription = message.body;
        console.log(this.transcription);
      });
      
      this.client.subscribe('/topic/reply', (message) => {
        this.reply = message.body;
        console.log(this.reply);
      })

      this.client.subscribe('/topic/audio', (message) => {
        this.audio = new Audio("data:audio/mp3;base64," + message.body)
        this.audio.play();
        this.convo.push([this.transcription, this.reply, this.audio])
        this.transcription = '';
        this.reply = '';
        this.audio = null;
      })
        

    },

    toggleRecording() {
      if (!this.isRecording) {
        this.startRecording();
      }
      else {
        this.stopRecording();
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
        await this.processAudio(audioBlob);

        this.mediaRecorder = null;
        this.stream = null;
      }
    },

    async processAudio(audio) {
      try {
        const formData = new FormData();
        formData.append('file', audio);

        const response = await fetch(this.transcribeURL, {
          method: "POST",
          body: formData
        });

        if (!response.ok) {
          throw new Error(`HTTP Error! Status: ${response.status}`)
        }

        //const data = await response.json();
        //this.transcription = data.transcription;
        //this.reply = data.reply;

        //this.convo.push([data.transcription, data.reply])

        //var snd = new Audio("data:audio/mp3;base64," + data.audio);
        //snd.play();

        // convert audio from Base64 to Blob URL
        /*const audioBlob = new Blob(
          [Uint8Array.from(atob(data.audio), c => c.charCodeAt(0))],
          { type: "audio/mpeg" }
        );
        console.log(audioBlob);
        this.audioUrl = URL.createObjectURL(audioBlob); */
      }
      catch (error) {
        console.error("Error processing audio:", error);
      }
    },
  }
}
</script>