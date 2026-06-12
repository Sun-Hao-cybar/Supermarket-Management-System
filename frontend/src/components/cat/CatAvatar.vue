<template>
  <div
    class="cat-wrapper"
    :class="{ 'is-dragging': isDragging, 'is-jumping': isJumping }"
    :style="positionStyle"
    @mousedown="onMouseDown"
    @touchstart.prevent="onTouchStart"
    @dblclick="resetPosition"
  >
    <!-- 小猫圆圈（overflow:hidden 保证完美圆形无黑边） -->
    <div class="cat-avatar">
      <video
        ref="videoRef"
        class="cat-video"
        src="/cat-agent.mp4"
        autoplay
        loop
        muted
        playsinline
        preload="auto"
      ></video>
    </div>

    <!-- 状态气泡（在圆圈外部，不会被 overflow:hidden 裁剪） -->
    <transition name="bubble-fade">
      <div v-if="bubbleText" class="cat-bubble" @click.stop>
        {{ bubbleText }}
      </div>
    </transition>

    <!-- 模式角标（在圆圈外部） -->
    <span class="cat-badge" :title="modeLabel">{{ modeIcon }}</span>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  mode: { type: String, default: 'smart' },
  initialX: { type: Number, default: 0 },
  initialY: { type: Number, default: 0 }
})

const emit = defineEmits(['click', 'positionChange'])

const videoRef = ref(null)
const isDragging = ref(false)
const isJumping = ref(false)
const bubbleText = ref('')
const posX = ref(0)
const posY = ref(0)

const catSize = computed(() => props.mode === 'fold' ? 40 : 80)

const positionStyle = computed(() => ({
  left: posX.value + 'px',
  top: posY.value + 'px',
  width: catSize.value + 'px',
  height: catSize.value + 'px'
}))

const modeIcon = computed(() => {
  switch (props.mode) {
    case 'smart': return '🐾'
    case 'quiet': return '🌙'
    case 'fold': return '📌'
    default: return '🐾'
  }
})

const modeLabel = computed(() => {
  switch (props.mode) {
    case 'smart': return '灵动模式'
    case 'quiet': return '安静模式'
    case 'fold': return '折叠模式'
    default: return ''
  }
})

onMounted(() => {
  const savedX = localStorage.getItem('catPosX')
  const savedY = localStorage.getItem('catPosY')
  if (savedX !== null && savedY !== null) {
    posX.value = parseInt(savedX, 10)
    posY.value = parseInt(savedY, 10)
  } else {
    posX.value = window.innerWidth - 100
    posY.value = window.innerHeight - 200
  }

  if (props.mode === 'smart') {
    startSmartBehavior()
  }
})

function savePosition() {
  localStorage.setItem('catPosX', posX.value)
  localStorage.setItem('catPosY', posY.value)
}

function resetPosition() {
  posX.value = window.innerWidth - 100
  posY.value = window.innerHeight - 200
  savePosition()
}

let smartTimer = null
function startSmartBehavior() {
  smartTimer = setInterval(() => {
    if (Math.random() > 0.6) {
      isJumping.value = true
      setTimeout(() => { isJumping.value = false }, 600)
    }
  }, 15000)
}

// ========== 拖拽 ==========
let dragStartX = 0
let dragStartY = 0
let startPosX = 0
let startPosY = 0
let hasMoved = false
let onMouseMoveRef = null
let onMouseUpRef = null
let onTouchMoveRef = null
let onTouchEndRef = null
let bubbleTimer = null

function clampPosition(x, y) {
  const maxX = window.innerWidth - catSize.value - 10
  const maxY = window.innerHeight - catSize.value - 10
  return {
    x: Math.max(10, Math.min(x, maxX)),
    y: Math.max(10, Math.min(y, maxY))
  }
}

function onMouseDown(e) {
  if (e.button !== 0) return
  dragStartX = e.clientX
  dragStartY = e.clientY
  startPosX = posX.value
  startPosY = posY.value
  hasMoved = false
  isDragging.value = true
  onMouseMoveRef = onMouseMove
  onMouseUpRef = onMouseUp
  document.addEventListener('mousemove', onMouseMoveRef)
  document.addEventListener('mouseup', onMouseUpRef)
}

function onMouseMove(e) {
  const dx = e.clientX - dragStartX
  const dy = e.clientY - dragStartY
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) hasMoved = true
  const clamped = clampPosition(startPosX + dx, startPosY + dy)
  posX.value = clamped.x
  posY.value = clamped.y
}

function onMouseUp() {
  isDragging.value = false
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
  savePosition()
  if (!hasMoved) {
    emit('click')
  }
}

function onTouchStart(e) {
  const touch = e.touches[0]
  dragStartX = touch.clientX
  dragStartY = touch.clientY
  startPosX = posX.value
  startPosY = posY.value
  hasMoved = false
  isDragging.value = true
  onTouchMoveRef = onTouchMove
  onTouchEndRef = onTouchEnd
  document.addEventListener('touchmove', onTouchMoveRef, { passive: false })
  document.addEventListener('touchend', onTouchEndRef)
}

function onTouchMove(e) {
  e.preventDefault()
  const touch = e.touches[0]
  const dx = touch.clientX - dragStartX
  const dy = touch.clientY - dragStartY
  if (Math.abs(dx) > 5 || Math.abs(dy) > 5) hasMoved = true
  const clamped = clampPosition(startPosX + dx, startPosY + dy)
  posX.value = clamped.x
  posY.value = clamped.y
}

function onTouchEnd() {
  isDragging.value = false
  document.removeEventListener('touchmove', onTouchMove)
  document.removeEventListener('touchend', onTouchEnd)
  savePosition()
  if (!hasMoved) {
    emit('click')
  }
}

function showBubble(text, duration = 5000) {
  if (bubbleTimer) clearTimeout(bubbleTimer)
  bubbleText.value = text
  if (duration > 0) {
    bubbleTimer = setTimeout(() => { bubbleText.value = '' }, duration)
  }
}

defineExpose({ showBubble, resetPosition })

onUnmounted(() => {
  if (smartTimer) clearInterval(smartTimer)
  if (bubbleTimer) clearTimeout(bubbleTimer)
  if (onMouseMoveRef) document.removeEventListener('mousemove', onMouseMoveRef)
  if (onMouseUpRef) document.removeEventListener('mouseup', onMouseUpRef)
  if (onTouchMoveRef) document.removeEventListener('touchmove', onTouchMoveRef)
  if (onTouchEndRef) document.removeEventListener('touchend', onTouchEndRef)
})
</script>

<style scoped>
/* ========== 外层包装器 ========== */
.cat-wrapper {
  position: fixed;
  z-index: 9999;
  cursor: grab;
  user-select: none;
  transition: width 0.3s ease, height 0.3s ease;
}

.cat-wrapper.is-dragging {
  cursor: grabbing;
  transition: none;
}

.cat-wrapper.is-jumping {
  animation: catJump 0.6s ease;
}

@keyframes catJump {
  0%, 100% { transform: translateY(0); }
  30% { transform: translateY(-18px) scale(1.08); }
  50% { transform: translateY(-22px) scale(1.05); }
  70% { transform: translateY(-8px) scale(0.95); }
}

/* ========== 小猫圆形主体（overflow:hidden 彻底裁剪黑边） ========== */
.cat-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  overflow: hidden;
  /* 柔光白边 + 投影 */
  box-shadow:
    0 0 0 5px rgba(255, 255, 255, 0.9),
    0 0 24px 6px rgba(255, 240, 220, 0.5),
    0 4px 18px rgba(180, 130, 80, 0.18);
}

.cat-video {
  /* 放大 8% 把视频自带的黑边推出 overflow:hidden 裁剪区 */
  width: 108%;
  height: 108%;
  margin: -4%;
  object-fit: cover;
  display: block;
  animation: catBreathe 4s ease-in-out infinite;
}

@keyframes catBreathe {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

/* ========== 状态气泡（在 wrapper 内、avatar 外） ========== */
.cat-bubble {
  position: absolute;
  bottom: calc(100% + 10px);
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.95);
  color: #5a3e28;
  padding: 8px 14px;
  border-radius: 16px;
  font-size: 13px;
  white-space: nowrap;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  pointer-events: none;
}

.bubble-fade-enter-active, .bubble-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.bubble-fade-enter-from, .bubble-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(6px);
}

/* ========== 模式角标（在 wrapper 内、avatar 外） ========== */
.cat-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 22px;
  height: 22px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.15);
  pointer-events: none;
}
</style>
