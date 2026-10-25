import { ref, shallowRef } from 'vue'

export interface AsyncSectionOptions {
  /** 将异常转换为可展示的文案；未提供时回退为异常文本 */
  errorMessage?: (error: unknown) => string
}

/**
 * 区块级异步数据加载：
 * 统一维护 data / loading / error，并通过 runId 丢弃过期请求结果。
 * 适用于首页「我的作品」「精选案例」等独立区块，也可复用于其他列表页。
 */
export function useAsyncSection<T>(loader: () => Promise<T>, options: AsyncSectionOptions = {}) {
  const data = shallowRef<T>()
  const loading = ref(false)
  const error = ref('')

  let runId = 0

  const run = async () => {
    const id = ++runId
    loading.value = true
    error.value = ''

    try {
      const result = await loader()
      if (id !== runId) return
      data.value = result
    } catch (reason) {
      if (id !== runId) return
      console.error('区块数据加载失败：', reason)
      error.value = options.errorMessage?.(reason) || String(reason)
    } finally {
      if (id === runId) loading.value = false
    }
  }

  return { data, loading, error, run }
}
