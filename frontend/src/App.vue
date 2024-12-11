<template>
  <main>
    <div>
      <button @click="toggleRecording">{{ recordButtonText }}</button><br>
      <hr class="ruler">
      <li v-for="interaction in interactions">
        <p>User: {{ interaction[0] }}</p>
        <p>AI: {{ interaction[1] }}</p>
      </li>
    </div>
  </main>
</template>



<script setup>
// extandable media Recorder is required since the normal one doesnt support wav
// this is easier than conversion from Webm
import { MediaRecorder, register } from 'extendable-media-recorder'
import { connect } from 'extendable-media-recorder-wav-encoder'
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
      reply: '',
      audioUrl: '',
      transcribeURL: 'http://localhost:8080/api/chat/process-audio',
      recordButtonText: 'Start Recording',

      interactions: []
    }
  },

  methods: {

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

        const data = await response.json();
        this.transcription = data.transcription;
        this.reply = data.reply;

        var snd = new Audio("data:audio/mp3;base64," + data.audio);
        snd.play();

        this.interactions.push([this.transcription, this.reply, snd])

      }
      catch (error) {
        console.error("Error processing audio:", error);
      }
    },
  }
}
</script>