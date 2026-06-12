<template>
  <div class="fx-layer">
    <!-- 大柔光光斑 - 15个 -->
    <div v-for="i in 15" :key="'b-'+i" class="fx-bokeh" :style="bokehStyle(i)"></div>
    <!-- 光束 -->
    <div class="fx-beam beam-1"></div>
    <div class="fx-beam beam-2"></div>
    <div class="fx-beam beam-3"></div>
    <div class="fx-beam beam-4"></div>
    <div class="fx-beam beam-5"></div>
    <!-- 高光闪烁 -->
    <span v-for="i in 30" :key="'s-'+i" class="fx-spark" :style="sparkStyle(i)"></span>
    <!-- 缓慢扫过的光带 -->
    <div class="fx-sweep"></div>
    <div class="fx-sweep sweep-2"></div>
  </div>
  <router-view />
</template>

<script setup>
const bokehStyle = (i) => {
  const sizes = [160, 100, 180, 120, 150, 90, 140, 110, 170, 80, 130, 105, 155, 95, 125]
  return {
    width: sizes[i - 1] + 'px',
    height: sizes[i - 1] + 'px',
    left: ((i * 67 + 8) % 82) + '%',
    top: ((i * 49 + 3) % 68) + '%',
    animationDelay: (i * 1.5) + 's',
    animationDuration: (10 + (i % 4) * 3) + 's'
  }
}
const sparkStyle = (i) => ({
  left: ((i * 113 + 12) % 90) + '%',
  top: ((i * 67 + 2) % 62) + '%',
  animationDelay: (i * 0.25) + 's',
  animationDuration: (1.5 + (i % 4) * 1) + 's'
})
</script>

<style scoped>
.fx-layer {
  position: fixed; inset: 0; z-index: 0;
  pointer-events: none; overflow: hidden;
}

/* === 柔光光斑 === */
.fx-bokeh {
  position: absolute; border-radius: 50%;
  background: radial-gradient(circle at 35% 35%,
    rgba(255,255,253,0.7) 0%,
    rgba(255,250,230,0.35) 18%,
    rgba(255,230,170,0.12) 45%,
    transparent 62%);
  filter: blur(2px);
  animation: bokehDrift 12s ease-in-out infinite;
  will-change: transform;
}
@keyframes bokehDrift {
  0%,100% { transform: translate(0,0) scale(1); }
  25%  { transform: translate(30px,-20px) scale(1.14); }
  50%  { transform: translate(-15px,25px) scale(0.86); }
  75%  { transform: translate(20px,10px) scale(1.08); }
}

/* === 光束 === */
.fx-beam {
  position: absolute; top: 0; height: 100%;
  width: 180px;
  background: linear-gradient(180deg,
    rgba(255,255,252,0.35) 0%,
    rgba(255,252,240,0.2) 12%,
    rgba(255,242,215,0.08) 35%,
    transparent 70%);
  transform: skewX(-4deg);
  animation: beamSweep 12s ease-in-out infinite;
}
.beam-1 { left: 5%; animation-delay: 0s; }
.beam-2 { left: 28%; animation-delay: -4s; width: 140px; }
.beam-3 { left: 50%; animation-delay: -8s; width: 160px; }
.beam-4 { left: 70%; animation-delay: -2s; width: 130px; }
.beam-5 { left: 85%; animation-delay: -6s; width: 100px; }

@keyframes beamSweep {
  0%,100% { opacity: 0.5; transform: skewX(-4deg) translateX(0); }
  30%  { opacity: 1; transform: skewX(-3deg) translateX(14px); }
  55%  { opacity: 0.6; transform: skewX(-4deg) translateX(-10px); }
  80%  { opacity: 0.9; transform: skewX(-2deg) translateX(8px); }
}

/* === 高光闪烁 === */
.fx-spark {
  position: absolute;
  width: 5px; height: 5px; border-radius: 50%;
  background: #fff;
  box-shadow:
    0 0 8px 3px rgba(255,255,253,1),
    0 0 20px 8px rgba(255,250,225,0.7),
    0 0 40px 14px rgba(255,240,200,0.35);
  animation: sparkTwinkle 2s ease-in-out infinite;
}
@keyframes sparkTwinkle {
  0%,100% { opacity: 0.05; transform: scale(0.2); }
  20%  { opacity: 1; transform: scale(1.5); }
  40%  { opacity: 0.7; transform: scale(0.9); }
  60%  { opacity: 0; transform: scale(0.4); }
  80%  { opacity: 0.5; transform: scale(0.7); }
}

/* === 扫过光带 === */
.fx-sweep {
  position: absolute; top: 0; left: -30%;
  width: 35%; height: 100%;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(255,255,252,0.06) 35%,
    rgba(255,255,252,0.15) 50%,
    rgba(255,255,252,0.06) 65%,
    transparent 100%);
  transform: skewX(-5deg);
  animation: sweepAcross 18s ease-in-out infinite;
}
.sweep-2 {
  animation-delay: -9s;
  width: 25%;
}
@keyframes sweepAcross {
  0%   { left: -45%; }
  35%  { left: 115%; }
  100% { left: 115%; }
}
</style>