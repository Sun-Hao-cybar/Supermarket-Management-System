import request from '@/utils/request'

/**
 * 发送消息给 AI Agent
 * @param {string} message - 用户消息
 * @param {Array} history - 历史对话 [{role, content}, ...]
 * @returns {Promise<{reply: string}>}
 */
export function sendMessage(message, history = []) {
  return request.post('/agent/chat', { message, history })
}
