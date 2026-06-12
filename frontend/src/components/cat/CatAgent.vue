<template>
  <div class="cat-agent-root">
    <!-- 小猫形象 -->
    <CatAvatar
      ref="avatarRef"
      :mode="mode"
      @click="toggleChat"
    />

    <!-- 对话面板 -->
    <ChatDialog
      :visible="chatVisible"
      :messages="messages"
      :isLoading="isLoading"
      :anchorX="avatarPos.x"
      :anchorY="avatarPos.y"
      @send="handleSend"
      @close="chatVisible = false"
      @openSettings="settingsVisible = true"
    />

    <!-- 设置面板 -->
    <SettingsPanel
      :visible="settingsVisible"
      :mode="mode"
      @update:mode="setMode"
      @close="settingsVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import CatAvatar from './CatAvatar.vue'
import ChatDialog from './ChatDialog.vue'
import SettingsPanel from './SettingsPanel.vue'
import { matchKnowledge } from '@/utils/knowledgeBase'
import { sendMessage } from '@/api/agent'

const mode = ref('smart')
const chatVisible = ref(false)
const settingsVisible = ref(false)
const isLoading = ref(false)
const messages = ref([])  // [{role: 'user'|'agent', content: '...'}]

const avatarPos = reactive({ x: 0, y: 0 })

const avatarRef = ref(null)

// 初始化：读取保存的模式 + 监听小猫位置
onMounted(() => {
  const savedMode = localStorage.getItem('catMode')
  if (savedMode && ['smart', 'quiet', 'fold'].includes(savedMode)) {
    mode.value = savedMode
  }

  const savedX = localStorage.getItem('catPosX')
  const savedY = localStorage.getItem('catPosY')
  if (savedX !== null && savedY !== null) {
    avatarPos.x = parseInt(savedX, 10)
    avatarPos.y = parseInt(savedY, 10)
  } else {
    avatarPos.x = window.innerWidth - 100
    avatarPos.y = window.innerHeight - 200
  }

  // 定时同步小猫位置
  const syncPos = setInterval(() => {
    const x = localStorage.getItem('catPosX')
    const y = localStorage.getItem('catPosY')
    if (x) avatarPos.x = parseInt(x, 10)
    if (y) avatarPos.y = parseInt(y, 10)
  }, 500)

  onUnmounted(() => clearInterval(syncPos))

  // 灵动模式：30s 后主动提示
  if (mode.value === 'smart') {
    setTimeout(() => {
      if (!chatVisible.value && mode.value === 'smart') {
        avatarRef.value?.showBubble('需要帮助吗？点我~', 8000)
      }
    }, 30000)
  }
})

// 清理：限制消息数量
function cleanMessages() {
  if (messages.value.length > 50) {
    messages.value = messages.value.slice(-30)
  }
}

function toggleChat() {
  chatVisible.value = !chatVisible.value
}

function setMode(newMode) {
  mode.value = newMode
  localStorage.setItem('catMode', newMode)
}

async function handleSend(text) {
  // 添加用户消息
  messages.value.push({ role: 'user', content: text })
  cleanMessages()

  // 1. 先尝试本地知识库匹配
  const localMatch = matchKnowledge(text)
  if (localMatch) {
    messages.value.push({ role: 'agent', content: localMatch.answer })
    cleanMessages()
    return
  }

  // 2. 未命中，调用后端 DeepSeek API
  isLoading.value = true
  try {
    const history = messages.value.slice(0, -1).map(m => ({
      role: m.role === 'agent' ? 'assistant' : m.role,
      content: m.content
    }))
    const res = await sendMessage(text, history)
    const reply = res?.data?.reply || '喵~ 抱歉，我暂时无法回答这个问题，请稍后再试~'
    messages.value.push({ role: 'agent', content: reply })
    cleanMessages()
  } catch (e) {
    messages.value.push({ role: 'agent', content: '喵~ 网络好像不太稳定，请检查后端服务是否启动，稍后再试~' })
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.cat-agent-root {
  /* 无样式，纯容器 */
}
</style>
