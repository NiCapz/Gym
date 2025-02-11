<template>
    <div class="click-speed-game">
      <h1>Click Speed Game</h1>
      <p>Click the button as many times as you can in 10 seconds!</p>
      <button @click="incrementScore" :disabled="!isGameActive">Click Me!</button>
      <p>Score: {{ score }}</p>
      <p v-if="timeLeft > 0">Time Left: {{ timeLeft }} seconds</p>
      <p v-else>Time's up! Your final score is {{ score }}.</p>
      <button @click="startGame" :disabled="isGameActive">Start Game</button>
    </div>
  </template>
  
  <script>
  export default {
    data() {
      return {
        score: 0,
        timeLeft: 10,
        isGameActive: false,
        timer: null
      };
    },
    methods: {
      startGame() {
        this.score = 0;
        this.timeLeft = 10;
        this.isGameActive = true;
        this.timer = setInterval(() => {
          this.timeLeft--;
          if (this.timeLeft <= 0) {
            clearInterval(this.timer);
            this.isGameActive = false;
          }
        }, 1000);
      },
      incrementScore() {
        if (this.isGameActive) {
          this.score++;
        }
      }
    }
  };
  </script>
  
  <style scoped>
  .click-speed-game {
    text-align: center;
    font-family: Arial, sans-serif;
    margin-top: 50px;
  }
  
  button {
    padding: 10px 20px;
    font-size: 16px;
    margin: 10px;
    cursor: pointer;
  }
  
  button:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
  </style>