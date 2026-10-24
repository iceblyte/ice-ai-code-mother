import { ref } from 'vue'

/**
 * 工作台（对话页）全局顶栏唤出状态：
 * 顶栏默认隐藏，悬停工作台 logo 时以浮层形式滑出，移开后收回。
 * 纯 UI 状态，不含业务逻辑；BasicLayout 与 AppChatPage 共享同一实例。
 */
const NAV_CLOSE_DELAY = 220

const navOpen = ref(false)
let closeTimer: ReturnType<typeof setTimeout> | undefined

const cancelClose = () => {
  if (closeTimer) {
    clearTimeout(closeTimer)
    closeTimer = undefined
  }
}

const openNav = () => {
  cancelClose()
  navOpen.value = true
}

const scheduleClose = () => {
  cancelClose()
  closeTimer = setTimeout(() => {
    navOpen.value = false
  }, NAV_CLOSE_DELAY)
}

export function useWorkspaceNav() {
  return { navOpen, openNav, scheduleClose, cancelClose }
}
