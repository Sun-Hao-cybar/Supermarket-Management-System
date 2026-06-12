<template>
  <transition name="settings-fade">
    <div v-if="visible" class="settings-overlay" @click.self="$emit('close')">
      <div class="settings-panel">
        <div class="settings-header">
          <span>🐱 小喵设置</span>
          <button class="settings-close" @click="$emit('close')">✕</button>
        </div>

        <div class="settings-body">
          <div class="settings-section">
            <div class="settings-label">交互模式</div>
            <div class="mode-options">
              <div
                v-for="opt in modeOptions"
                :key="opt.value"
                class="mode-card"
                :class="{ active: currentMode === opt.value }"
                @click="$emit('update:mode', opt.value)"
              >
                <span class="mode-emoji">{{ opt.emoji }}</span>
                <div class="mode-info">
                  <div class="mode-name">{{ opt.name }}</div>
                  <div class="mode-desc">{{ opt.desc }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="settings-section">
            <div class="settings-label">关于小喵</div>
            <p class="settings-about">
              小喵是进销存管理系统的 AI 助手，采用"本地知识库 + DeepSeek AI"混合方案。
              预设 40+ 条常见操作问答，覆盖员工管理、会员管理、商品管理、采购管理、供应商管理等模块。
            </p>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  mode: { type: String, default: 'smart' }
})

const emit = defineEmits(['update:mode', 'close'])

const currentMode = computed(() => props.mode)

const modeOptions = [
  { value: 'smart', emoji: '🐾', name: '灵动模式', desc: '主动发现你的困难，偶尔跳跃撒娇' },
  { value: 'quiet', emoji: '🌙', name: '安静模式', desc: '安静陪伴，点击才打开对话' },
  { value: 'fold', emoji: '📌', name: '折叠模式', desc: '缩小为图标，最小化存在感' }
]
</script>

<style scoped>
.settings-overlay {
  position: fixed; inset: 0; z-index: 10001;
  background: rgba(0,0,0,0.3);
  display: flex; align-items: center; justify-content: center;
}
.settings-panel {
  width: 360px; max-height: 80vh;
  background: #fff; border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.2);
  overflow: hidden;
}
.settings-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 18px; border-bottom: 1px solid #f0e6d8;
  font-weight: 600; color: #5a3e28;
}
.settings-close {
  border: none; background: transparent; cursor: pointer;
  font-size: 16px; color: #9b8570;
}
.settings-body { padding: 16px 18px; }
.settings-section { margin-bottom: 20px; }
.settings-label { font-size: 14px; font-weight: 600; color: #5a3e28; margin-bottom: 10px; }

.mode-options { display: flex; flex-direction: column; gap: 8px; }
.mode-card {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; border-radius: 12px;
  cursor: pointer; border: 2px solid transparent;
  transition: all 0.2s;
  background: #fefcf8;
}
.mode-card:hover { background: #fef5ea; }
.mode-card.active { border-color: #d4843b; background: #fef5ea; }
.mode-emoji { font-size: 28px; flex-shrink: 0; }
.mode-name { font-weight: 600; color: #5a3e28; font-size: 14px; }
.mode-desc { font-size: 12px; color: #9b8570; margin-top: 2px; }

.settings-about { font-size: 13px; color: #6b4d34; line-height: 1.8; }

.settings-fade-enter-active, .settings-fade-leave-active { transition: opacity 0.2s; }
.settings-fade-enter-from, .settings-fade-leave-to { opacity: 0; }
</style>
