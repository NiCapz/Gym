<template>
  <main>
    <div>
      <li v-for="interaction in interactions">
        <p>User: {{ interaction[0] }}</p>
        <p>AI: {{ interaction[1] }}</p>
      </li>
      <li>
        <p v-if="transcription">User: {{ transcription }}</p>
        <p v-if="reply">AI: {{ reply }}</p>
      </li>
      <hr class="ruler">
      <textarea
      v-model="textInput"
      rows="5" 
      cols="50"
      placeholder="">
    </textarea>
  </div>
  <div class="buttons">
    <button @click="toggleRecording">{{ recordButtonText }}</button>
    <button @click="processText">Send written query</button>
  </div>
  <span>User ID</span><input v-model="userId" type="number" min="1">
  <span>User Mood: {{ userMood }}</span>
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
      stream: null,
      reply: null,
      sound: null,
      audioUrl: '',
      transcribeURL: 'http://localhost:8080/api/chat/process-audio',
      transcribeTextURL: 'http://localhost:8080/api/chat/process-text',
      recordButtonText: 'Start Recording',
      textInput: '',
      userMood: "%",

      interactions: [],

      client: null,
      socket: null,
      sessionId: '',
      userId: '',
      connected: false
    }
  },

  created() {
    this.sessionId = Math.floor(Math.random() * 100000);
    this.userId = Math.floor(Math.random() * 100000);
    console.log(this.sessionId);
    this.client = new Client({
      webSocketFactory: () => new WebSocket('ws:localhost:8080/transcription-websocket'),
      reconnectDelay: 5000,
      onConnect: () => {
        this.subscribeToTranscriptions();
      }
    });
    this.client.activate();
  },

  methods: {

    subscribeToTranscriptions() {
      this.client.subscribe(`/topic/transcription/${this.sessionId}`, message => {
        const result = message.body;
        this.transcription = result;
        console.log(this.transcription)
      });

      this.client.subscribe(`/topic/chatReply/${this.sessionId}`, message => {
        const result = message.body;
        this.reply = result;
        console.log(this.reply)
      });

      this.client.subscribe(`/topic/replyChunk/${this.sessionId}`, message => {
        console.log("receiving replychunk...")
        const result = message.body;
        this.reply += result;
      });

      this.client.subscribe(`/topic/audio/${this.sessionId}`, message => {
        console.log("receiving audio...");
        const result = message.body;
        this.sound = new Audio("data:audio/mp3;base64," + result);
        this.sound.play();
        if (this.transcription && this.reply && this.sound) {
          this.interactions.push([this.transcription, this.reply, this.sound]);
          this.transcription = null;
          this.reply = null;
          this.sound = null;
      }
    });

    },

    subscribeToMoodUpdates() {
      console.log("Subscribed to mood updates with Id " + this.userId)
      this.client.subscribe(`/topic/moodUpdates/${this.userId}`, message => {
      console.log("user mood:"  + message.body);
      const result = message.body
      
      switch(result) {
        case "1": this.userMood = "Extremely bad 😭"
        case "2": this.userMood = "Bad😞"
        case "3": this.userMood = "Neutral 😐"
        case "4": this.userMood = "Good 😊"
        case "5": this.userMood = "Extremely good! 😄"
      }
      console.log(this.userMood)
    });
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
        formData.append('sessionId', this.sessionId)

        const response = await fetch(this.transcribeURL, {
          method: "POST",
          body: formData
        });

        if (!response.ok) {
          throw new Error(`HTTP Error! Status: ${response.status}`)
        }

      }
      catch (error) {
        console.error("Error processing audio:", error);
      }
    },
    
    async processText() {
      this.subscribeToMoodUpdates()
      console.log(`/topic/moodUpdates/${this.userId}`)
      console.log(this.userId)
      const text = this.textInput
      this.textInput = '';  
      if (text != '') {
          try {
            const formData = new FormData();
            formData.append('text', text);
            formData.append('sessionId', this.sessionId)
            formData.append('userId', this.userId)
  
            const response = await fetch(this.transcribeTextURL, {
              method: 'POST',
              body: formData
            });
  
            if (!response.ok) {
              throw new Error(`Http Error! Status: ${response.status}`)
            }
          }
          catch (error) {
            console.error("Error processing text:", error);
          }
          this.textInput = '';
        }
    },
  }
}
</script>