import { ref } from 'vue'

export type PromptOptimizerState = 'idle' | 'optimizing' | 'optimized'

export interface PromptOptimizerOptions {
  /** 模拟优化耗时（毫秒）；接入真实接口后由请求状态替换 */
  delay?: number
  /** 前端模拟的文案增强逻辑；接入后端后由接口返回结果替换 */
  enhance?: (prompt: string) => string
}

/**
 * 提示词优化（前端模拟版）
 * - 优化中保留原文，完成后可一键撤回
 * - 用户手动编辑输入框时调用 reset()，丢弃本次优化状态
 */
export function usePromptOptimizer(options: PromptOptimizerOptions = {}) {
  const delay = options.delay ?? 1200
  const state = ref<PromptOptimizerState>('idle')
  const original = ref('')

  // 每次优化/撤回/重置都递增，避免过期异步结果覆盖新状态
  let runId = 0

  const optimize = async (prompt: string): Promise<string | null> => {
    if (!prompt.trim()) return null

    const id = ++runId
    original.value = prompt
    state.value = 'optimizing'

    await new Promise<void>((resolve) => {
      setTimeout(resolve, delay)
    })

    if (id !== runId) return null

    state.value = 'optimized'
    return options.enhance ? options.enhance(prompt) : prompt
  }

  const undo = (): string => {
    runId += 1
    state.value = 'idle'
    const source = original.value
    original.value = ''
    return source
  }

  const reset = () => {
    runId += 1
    state.value = 'idle'
    original.value = ''
  }

  return { state, original, optimize, undo, reset }
}
