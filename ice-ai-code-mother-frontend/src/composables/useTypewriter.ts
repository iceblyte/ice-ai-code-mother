import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

export interface TypewriterSpeed {
  /** 每个字符的打字间隔（毫秒） */
  type?: number
  /** 每个字符的删除间隔（毫秒） */
  delete?: number
  /** 整句打完后的停留时间（毫秒） */
  hold?: number
}

type Phase = 'typing' | 'holding' | 'deleting'

/**
 * 循环打字机：
 * 打字 → 停留 → 删除 → 立即开始下一句。
 * 输入框有内容（active=false）或页面隐藏时暂停；恢复后从当前位置继续。
 * 系统开启「减少动态效果」时静态展示第一句，不做动画。
 */
export function useTypewriter(
  lines: () => string[],
  active: () => boolean,
  speed: TypewriterSpeed = {},
) {
  const typeDelay = speed.type ?? 80
  const deleteDelay = speed.delete ?? 40
  const holdDelay = speed.hold ?? 1500

  const text = ref('')
  const lineIndex = ref(0)

  let phase: Phase = 'typing'
  let timer: ReturnType<typeof setTimeout> | null = null
  let motionQuery: MediaQueryList | null = null
  let reducedMotion = false
  let started = false
  let disposed = false

  const currentLines = (): string[] => {
    const list = lines()
    if (!Array.isArray(list)) return []
    return list.filter((line): line is string => typeof line === 'string' && line.length > 0)
  }

  const canAnimate = (): boolean =>
    !disposed && !reducedMotion && active() && typeof document !== 'undefined' && !document.hidden

  const clearTimer = () => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  const schedule = (delay: number) => {
    clearTimer()
    if (!canAnimate()) return
    timer = setTimeout(tick, delay)
  }

  const tick = () => {
    timer = null
    if (!canAnimate()) return

    const list = currentLines()
    if (list.length === 0) return

    // 语言切换后列表长度可能变化，防止索引越界
    if (lineIndex.value >= list.length) lineIndex.value = 0
    const line = list[lineIndex.value]

    if (phase === 'typing') {
      if (text.value.length < line.length) {
        text.value = line.slice(0, text.value.length + 1)
        schedule(typeDelay)
      } else {
        phase = 'holding'
        schedule(holdDelay)
      }
      return
    }

    if (phase === 'holding') {
      phase = 'deleting'
      schedule(deleteDelay)
      return
    }

    if (text.value.length > 0) {
      text.value = line.slice(0, text.value.length - 1)
      schedule(deleteDelay)
      return
    }

    lineIndex.value = (lineIndex.value + 1) % list.length
    phase = 'typing'
    schedule(typeDelay)
  }

  const reset = () => {
    clearTimer()
    text.value = ''
    lineIndex.value = 0
    phase = 'typing'

    if (reducedMotion) {
      text.value = currentLines()[0] ?? ''
      return
    }
    schedule(0)
  }

  const resume = () => {
    if (!started || disposed || reducedMotion || timer) return
    if (canAnimate()) schedule(0)
  }

  const onVisibilityChange = () => {
    if (document.hidden) clearTimer()
    else resume()
  }

  const onMotionChange = (event: MediaQueryListEvent) => {
    reducedMotion = event.matches
    if (reducedMotion) {
      clearTimer()
      text.value = currentLines()[0] ?? ''
    } else {
      reset()
    }
  }

  onMounted(() => {
    started = true
    motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
    reducedMotion = motionQuery.matches
    motionQuery.addEventListener('change', onMotionChange)
    document.addEventListener('visibilitychange', onVisibilityChange)

    if (reducedMotion) {
      text.value = currentLines()[0] ?? ''
    } else {
      reset()
    }
  })

  onBeforeUnmount(() => {
    disposed = true
    clearTimer()
    document.removeEventListener('visibilitychange', onVisibilityChange)
    motionQuery?.removeEventListener('change', onMotionChange)
  })

  watch(
    () => active(),
    (isActive) => {
      if (!started) return
      if (isActive) resume()
      else clearTimer()
    },
  )

  watch(
    () => lines().join('\u0000'),
    () => {
      if (!started) return
      reset()
    },
  )

  return { text, lineIndex }
}
