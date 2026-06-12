/**
 * 对话缓存 — localStorage 持久化的自学习 Q&A 缓存
 *
 * 工作流程：
 * 1. 知识库未命中 → 查缓存 → 命中直接返回
 * 2. 缓存也未命中 → 调 DeepSeek → 成功则自动存入缓存
 * 3. 下次同样/相似问题秒回，不消耗 API
 */

const CACHE_KEY = 'cat_agent_cache'
const MAX_SIZE = 100

/**
 * 规范化问题文本（去空格、转小写）
 */
function normalize(text) {
  return text.trim().replace(/\s+/g, ' ')
}

/**
 * 读取缓存
 */
export function loadCache() {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

/**
 * 保存缓存
 */
function saveCache(entries) {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify(entries))
  } catch {
    // localStorage 满了，清掉一半旧数据
    const half = entries.slice(-Math.floor(MAX_SIZE / 2))
    localStorage.setItem(CACHE_KEY, JSON.stringify(half))
  }
}

/**
 * 在缓存中查找匹配的回答
 * 使用子串匹配（与知识库一致），匹配度最高者获胜
 * @returns {{ answer: string } | null}
 */
export function lookupCache(message) {
  const q = normalize(message).toLowerCase()
  if (q.length < 2) return null

  const entries = loadCache()
  let best = null
  let bestScore = 0

  for (const entry of entries) {
    const eq = normalize(entry.q).toLowerCase()
    // 完全匹配直接返回
    if (q === eq) return { answer: entry.a }

    // 子串包含：用户问句包含缓存问题 或 缓存问题包含用户问句
    let score = 0
    if (q.includes(eq)) score = eq.length
    else if (eq.includes(q)) score = q.length

    if (score > bestScore) {
      bestScore = score
      best = entry
    }
  }

  // 匹配长度 ≥5 或 ≥用户输入70%长度 → 认为命中
  if (best && (bestScore >= 5 || bestScore >= q.length * 0.7)) {
    return { answer: best.a }
  }

  return null
}

/**
 * 将新问答存入缓存
 */
export function saveToCache(question, answer) {
  if (!question || !answer) return
  const q = normalize(question)
  if (q.length < 3 || answer.length < 5) return

  const entries = loadCache()

  // 去重：相同问题替换旧回答
  const idx = entries.findIndex(e => normalize(e.q).toLowerCase() === q.toLowerCase())
  if (idx >= 0) {
    entries.splice(idx, 1)
  }

  // 追加到末尾
  entries.push({ q, a: answer })

  // 超限裁掉最早的
  while (entries.length > MAX_SIZE) {
    entries.shift()
  }

  saveCache(entries)
}

/**
 * 获取缓存统计
 */
export function getCacheStats() {
  const entries = loadCache()
  return { count: entries.length, max: MAX_SIZE }
}

/**
 * 清空缓存
 */
export function clearCache() {
  localStorage.removeItem(CACHE_KEY)
}
