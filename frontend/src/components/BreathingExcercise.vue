<template>
    <div class="breathing-container">
      <!-- The square with a moving dot along its border -->
      <div class="square">
        <div class="dot" :style="{ left: dotX + 'px', top: dotY + 'px' }"></div>
      </div>
  
      <!-- Display the current phase and countdown -->
      <div class="info">
        <h2>{{ currentPhase }}</h2>
        <p class="countdown">{{ countdown }}</p>
      </div>
  
      <!-- Button to start/stop the exercise -->
      <button @click="toggleExercise">{{ running ? 'Stop' : 'Start' }}</button>
    </div>
  </template>
  
  <script setup>
    import { ref, computed, onBeforeUnmount, watch } from 'vue';
    import breatheInSound from '../assets/BreathingSounds/breathe_in.mp3';
    import holdSound from '../assets/BreathingSounds/hold.mp3';
    import breatheOutSound from '../assets/BreathingSounds/breathe_out.mp3';

  
  // Create Audio objects for the instructions.
  const breatheInAudio = new Audio(breatheInSound);
  const holdAudio = new Audio(holdSound);
  const breatheOutAudio = new Audio(breatheOutSound);
  
  // Define the four phases (each lasting 4 seconds)
  const phases = ["Breathe in", "Hold your breath", "Breathe out", "Hold your breath"];
  const phaseDuration = 4; // seconds per phase
  const totalCycle = phaseDuration * phases.length; // 16 seconds per cycle
  
  // Visual dimensions (adjust as needed)
  const squareSize = 200; // square width/height in pixels
  const dotSize = 20;     // dot size in pixels
  
  // Timer and state management
  const running = ref(false);
  const startTime = ref(0);
  const elapsed = ref(0);
  let intervalId = null;
  
  function startExercise() {
    // Set the start time and begin updating elapsed time
    startTime.value = Date.now() - elapsed.value * 1000;
    running.value = true;
    intervalId = setInterval(() => {
      elapsed.value = (Date.now() - startTime.value) / 1000;
    }, 100);
  }
  
  function stopExercise() {
    running.value = false;
    clearInterval(intervalId);
    intervalId = null;
  }
  
  function toggleExercise() {
    running.value ? stopExercise() : startExercise();
  }
  
  onBeforeUnmount(() => {
    if (intervalId) clearInterval(intervalId);
  });
  
  // Compute the current phase index (0 to 3) in the cycle.
  const currentPhaseIndex = computed(() => {
    return Math.floor((elapsed.value % totalCycle) / phaseDuration);
  });
  
  // Display the current phase text.
  const currentPhase = computed(() => phases[currentPhaseIndex.value]);
  
  // Countdown display: shows numbers 4, 3, 2, 1.
  const countdown = computed(() => {
    const phaseElapsed = elapsed.value % phaseDuration;
    return 4 - Math.floor(phaseElapsed);
  });
  
  /*
    Animate the dot along the square’s border:
    - Phase 0 ("Breathe in"): top side, left to right.
    - Phase 1 ("Hold your breath"): right side, top to bottom.
    - Phase 2 ("Breathe out"): bottom side, right to left.
    - Phase 3 ("Hold your breath"): left side, bottom to top.
  */
  const dotX = computed(() => {
    const phaseProgress = (elapsed.value % phaseDuration) / phaseDuration;
    const phase = currentPhaseIndex.value;
    if (phase === 0) return phaseProgress * (squareSize - dotSize);
    if (phase === 1) return squareSize - dotSize;
    if (phase === 2) return (1 - phaseProgress) * (squareSize - dotSize);
    if (phase === 3) return 0;
  });
  
  const dotY = computed(() => {
    const phaseProgress = (elapsed.value % phaseDuration) / phaseDuration;
    const phase = currentPhaseIndex.value;
    if (phase === 0) return 0;
    if (phase === 1) return phaseProgress * (squareSize - dotSize);
    if (phase === 2) return squareSize - dotSize;
    if (phase === 3) return (1 - phaseProgress) * (squareSize - dotSize);
  });
  
  // Watch for phase changes and play the corresponding audio instruction.
  const lastPhaseIndex = ref(null);
  watch(currentPhaseIndex, (newVal) => {
    if (running.value && newVal !== lastPhaseIndex.value) {
      lastPhaseIndex.value = newVal;
      if (newVal === 0) {
        breatheInAudio.currentTime = 0;
        breatheInAudio.play();
      } else if (newVal === 1 || newVal === 3) {
        holdAudio.currentTime = 0;
        holdAudio.play();
      } else if (newVal === 2) {
        breatheOutAudio.currentTime = 0;
        breatheOutAudio.play();
      }
    }
  });
  </script>
  
  <style scoped>
  .breathing-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    font-family: sans-serif;
  }
  
  .square {
    position: relative;
    width: 200px;
    height: 200px;
    border: 2px solid #333;
    margin-bottom: 20px;
  }
  
  .dot {
    position: absolute;
    width: 20px;
    height: 20px;
    background-color: #007bff;
    border-radius: 50%;
  }
  
  .info {
    text-align: center;
    margin-bottom: 20px;
  }
  
  .countdown {
    font-size: 2em;
    font-weight: bold;
  }
  
  button {
    padding: 10px 20px;
    font-size: 16px;
    cursor: pointer;
  }
  </style>