<template>
  <div
    class="cat-avatar"
    :class="{ 'is-dragging': isDragging, 'is-jumping': isJumping }"
    :style="positionStyle"
    @mousedown="onMouseDown"
    @touchstart.prevent="onTouchStart"
    @dblclick="resetPosition"
  >
    <!-- 视频播放小猫 -->
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

    <!-- 状态气泡 -->
    <transition name="bubble-fade">
      <div v-if="bubbleText" class="cat-bubble" @click.stop>
        {{ bubbleText }}
      </div>
    </transition>

    <!-- 模式角标 -->
    <span class="cat-badge" :title="modeLabel">{{ modeIcon }}</span>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  mode: { type: String, default: 'smart' },  // smart | quiet | fold
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

// 初始化位置
onMounted(() => {
  const savedX = localStorage.getItem('catPosX')
  const savedY = localStorage.getItem('catPosY')
  if (savedX !== null && savedY !== null) {
    posX.value = parseInt(savedX)
    posY.value = parseInt(savedY)
  } else {
    posX.value = window.innerWidth - 100
    posY.value = window.innerHeight - 200
  }

  // 灵动模式：定时主动提示 + 跳跃
  if (props.mode === 'smart') {
    startSmartBehavior()
  }
})

// 保存位置
function savePosition() {
  localStorage.setItem('catPosX', posX.value)
  localStorage.setItem('catPosY', posY.value)
}

// 重置到默认位置
function resetPosition() {
  posX.value = window.innerWidth - 100
  posY.value = window.innerHeight - 200
  savePosition()
}

// 灵动模式主动行为
let smartTimer = null
function startSmartBehavior() {
  smartTimer = setInterval(() => {
    // 随机跳跃
    if (Math.random() > 0.6) {
      isJumping.value = true
      setTimeout(() => { isJumping.value = false }, 600)
    }
  }, 15000) // 每 15 秒可能跳一下
}

// ========== 拖拽 ==========
let dragStartX = 0
let dragStartY = 0
let startPosX = 0
let startPosY = 0
let hasMoved = false

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
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
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
  document.addEventListener('touchmove', onTouchMove, { passive: false })
  document.addEventListener('touchend', onTouchEnd)
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

// 暴露方法给父组件
function showBubble(text, duration = 5000) {
  bubbleText.value = text
  if (duration > 0) {
    setTimeout(() => { bubbleText.value = '' }, duration)
  }
}

defineExpose({ showBubble, resetPosition })

onUnmounted(() => {
  if (smartTimer) clearInterval(smartTimer)
})
</script>

<style scoped>
.cat-avatar {
  position: fixed;
  z-index: 9999;
  cursor: grab;
  user-select: none;
  border-radius: 50%;
  overflow: visible;
  transition: width 0.3s ease, height 0.3s ease;
}

.cat-avatar.is-dragging {
  cursor: grabbing;
  transition: none;
}

.cat-avatar.is-jumping {
  animation: catJump 0.6s ease;
}

@keyframes catJump {
  0%, 100% { transform: translateY(0); }
  30% { transform: translateY(-18px) scale(1.08); }
  50% { transform: translateY(-22px) scale(1.05); }
  70% { transform: translateY(-8px) scale(0.95); }
}

/* 视频播放 */
.cat-video {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  /* 柔边遮罩融合白底 */
  box-shadow:
    0 0 0 6px rgba(255, 255, 255, 0.75),
    0 0 20px 8px rgba(255, 240, 220, 0.5),
    0 4px 16px rgba(180, 130, 80, 0.15);
  animation: catBreathe 4s ease-in-out infinite;
}

@keyframes catBreathe {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

/* 状态气泡 */
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

/* 模式角标 */
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
