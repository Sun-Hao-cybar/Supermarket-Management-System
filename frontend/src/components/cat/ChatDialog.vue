<template>
  <transition name="dialog-slide">
    <div
      v-if="visible"
      class="chat-dialog"
      :style="dialogStyle"
    >
      <!-- 头部 -->
      <div class="chat-header">
        <span class="chat-title">🐱 小喵助手</span>
        <div class="chat-header-actions">
          <button class="chat-btn-icon" title="设置" @click="$emit('openSettings')">⚙️</button>
          <button class="chat-btn-icon" title="关闭" @click="$emit('close')">✕</button>
        </div>
      </div>

      <!-- 消息列表 -->
      <div class="chat-messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="chat-empty">
          <div class="chat-empty-icon">🐱</div>
          <p>喵~ 我是进销存系统小助手</p>
          <p class="chat-empty-hint">点击下方快捷问题或输入你的问题</p>
        </div>

        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          class="chat-msg"
          :class="msg.role === 'user' ? 'msg-user' : 'msg-agent'"
        >
          <div class="msg-bubble">{{ msg.content }}</div>
        </div>

        <!-- 正在输入中 -->
        <div v-if="isLoading" class="chat-msg msg-agent">
          <div class="msg-bubble typing-bubble">
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
          </div>
        </div>
      </div>

      <!-- 快捷问题 -->
      <div class="chat-quick-questions">
        <span
          v-for="(q, idx) in quickQuestions"
          :key="idx"
          class="quick-tag"
          @click="$emit('send', q)"
        >{{ q }}</span>
      </div>

      <!-- 输入区 -->
      <div class="chat-input-area">
        <input
          ref="inputRef"
          v-model="inputText"
          class="chat-input"
          placeholder="输入你的问题..."
          @keyup.enter="sendMessage"
        />
        <button class="chat-send-btn" @click="sendMessage" :disabled="!inputText.trim() || isLoading">
          发送
        </button>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch, nextTick, computed } from 'vue'
import { getQuickQuestions } from '@/utils/knowledgeBase'

const props = defineProps({
  visible: { type: Boolean, default: false },
  messages: { type: Array, default: () => [] },
  isLoading: { type: Boolean, default: false },
  anchorX: { type: Number, default: 0 },
  anchorY: { type: Number, default: 0 }
})

const emit = defineEmits(['send', 'close', 'openSettings'])

const inputRef = ref(null)
const messagesRef = ref(null)
const inputText = ref('')
const quickQuestions = ref(getQuickQuestions())

// 对话面板定位（小猫附近）
const dialogStyle = computed(() => {
  const panelW = 360
  const panelH = 480
  let left = props.anchorX - panelW + 80  // 默认右边对齐小猫
  let top = props.anchorY - panelH - 10

  // 边界检查
  if (left < 10) left = 10
  if (left + panelW > window.innerWidth - 10) left = window.innerWidth - panelW - 10
  if (top < 10) top = props.anchorY + 90
  if (top + panelH > window.innerHeight - 10) top = window.innerHeight - panelH - 10

  return {
    left: left + 'px',
    top: top + 'px'
  }
})

// 当面板打开时聚焦输入框
watch(() => props.visible, async (val) => {
  if (val) {
    await nextTick()
    inputRef.value?.focus()
  }
})

// 新消息到达时滚动到底部
watch(() => props.messages.length, async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
})

function sendMessage() {
  const text = inputText.value.trim()
  if (!text || props.isLoading) return
  emit('send', text)
  inputText.value = ''
}
</script>

<style scoped>
.chat-dialog {
  position: fixed;
  z-index: 10000;
  width: 360px;
  height: 480px;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 16px;
  box-shadow: 0 8px 36px rgba(120, 80, 30, 0.18), 0 0 0 1px rgba(180, 130, 80, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dialog-slide-enter-active, .dialog-slide-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.dialog-slide-enter-from, .dialog-slide-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.96);
}

/* 头部 */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(200, 150, 100, 0.15);
  flex-shrink: 0;
}
.chat-title { font-weight: 600; color: #5a3e28; font-size: 15px; }
.chat-header-actions { display: flex; gap: 4px; }
.chat-btn-icon {
  width: 28px; height: 28px; border: none; background: transparent;
  cursor: pointer; border-radius: 6px; font-size: 14px; color: #9b8570;
  display: flex; align-items: center; justify-content: center;
}
.chat-btn-icon:hover { background: rgba(200, 150, 100, 0.1); color: #5a3e28; }

/* 消息列表 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.chat-messages::-webkit-scrollbar { width: 4px; }
.chat-messages::-webkit-scrollbar-thumb { background: rgba(180, 130, 80, 0.15); border-radius: 4px; }

.chat-empty { text-align: center; padding: 40px 20px; color: #9b8570; }
.chat-empty-icon { font-size: 40px; margin-bottom: 10px; }
.chat-empty-hint { font-size: 12px; margin-top: 6px; }

.chat-msg { display: flex; }
.msg-user { justify-content: flex-end; }
.msg-agent { justify-content: flex-start; }

.msg-bubble {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}
.msg-user .msg-bubble {
  background: linear-gradient(135deg, #8b6fd4, #a78bfa);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.msg-agent .msg-bubble {
  background: #fef9f0;
  color: #5a3e28;
  border: 1px solid rgba(200, 150, 100, 0.12);
  border-bottom-left-radius: 4px;
}

/* 输入中动画 */
.typing-bubble {
  display: flex; gap: 4px; padding: 14px 16px;
}
.typing-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: rgba(180, 130, 80, 0.4);
  animation: typingBounce 1.2s ease-in-out infinite;
}
.typing-dot:nth-child(2) { animation-delay: 0.15s; }
.typing-dot:nth-child(3) { animation-delay: 0.3s; }
@keyframes typingBounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

/* 快捷问题 */
.chat-quick-questions {
  display: flex;
  gap: 6px;
  padding: 8px 14px;
  overflow-x: auto;
  flex-shrink: 0;
  border-top: 1px solid rgba(200, 150, 100, 0.08);
}
.chat-quick-questions::-webkit-scrollbar { height: 0; }
.quick-tag {
  flex-shrink: 0;
  padding: 4px 12px;
  background: #fef5ea;
  color: #b8661e;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  border: 1px solid rgba(220, 160, 90, 0.15);
}
.quick-tag:hover { background: #fde8cf; }

/* 输入区 */
.chat-input-area {
  display: flex;
  gap: 8px;
  padding: 10px 14px;
  border-top: 1px solid rgba(200, 150, 100, 0.12);
  flex-shrink: 0;
}
.chat-input {
  flex: 1;
  border: 1px solid rgba(200, 150, 100, 0.2);
  border-radius: 20px;
  padding: 8px 14px;
  font-size: 13px;
  outline: none;
  color: #5a3e28;
  background: #fefcf8;
  transition: border-color 0.2s;
}
.chat-input:focus { border-color: #d4843b; }
.chat-send-btn {
  border: none;
  padding: 8px 18px;
  border-radius: 20px;
  background: linear-gradient(135deg, #d4843b, #e8a85f);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.2s;
}
.chat-send-btn:hover { opacity: 0.9; }
.chat-send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
