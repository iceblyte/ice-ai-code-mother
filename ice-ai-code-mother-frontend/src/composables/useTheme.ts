import { computed, reactive, watchEffect } from 'vue'
import {
  DEFAULT_SKIN,
  DEFAULT_THEME,
  ENABLED_SKINS,
  SKINS,
  STORAGE_KEYS,
  THEMES,
  hasStoredPreference,
  readPreference,
  writePreference,
  type Skin,
  type Theme,
} from '@/constants/uiPreferences'

/**
 * 主题 / 外观皮肤偏好（模块级单例，多组件共享）
 * 状态变化同步到 <html> 的 data-theme / data-skin 属性，
 * 由 styles/tokens.css 与 styles/skins/*.css 的 CSS 变量驱动视觉。
 */

interface ThemeState {
  theme: Theme
  skin: Skin
}

function resolveInitialTheme(): Theme {
  const stored = readPreference<Theme>(STORAGE_KEYS.theme, THEMES)
  if (stored) return stored
  // 无手动偏好时跟随系统，系统不可用回落亮色
  const prefersDark =
    typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches
  return prefersDark ? 'dark' : DEFAULT_THEME
}

function resolveInitialSkin(): Skin {
  const stored = readPreference<Skin>(STORAGE_KEYS.skin, SKINS)
  if (stored && (ENABLED_SKINS as readonly string[]).includes(stored)) return stored
  return DEFAULT_SKIN
}

const state = reactive<ThemeState>({
  theme: resolveInitialTheme(),
  skin: resolveInitialSkin(),
})

/** 把当前偏好同步到 <html> 属性 */
watchEffect(() => {
  document.documentElement.setAttribute('data-theme', state.theme)
  document.documentElement.setAttribute('data-skin', state.skin)
})

/** 未手动选择过主题时，跟随系统明暗变化 */
let systemListenerBound = false
function bindSystemThemeListener(): void {
  if (systemListenerBound || typeof window === 'undefined' || !window.matchMedia) return
  systemListenerBound = true
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (event) => {
    if (hasStoredPreference(STORAGE_KEYS.theme)) return
    state.theme = event.matches ? 'dark' : 'light'
  })
}

function setTheme(name: Theme): void {
  if (!(THEMES as readonly string[]).includes(name)) return
  state.theme = name
  writePreference(STORAGE_KEYS.theme, name)
}

function toggleTheme(): void {
  setTheme(state.theme === 'dark' ? 'light' : 'dark')
}

function setSkin(skin: Skin): void {
  if (!(ENABLED_SKINS as readonly string[]).includes(skin)) return
  state.skin = skin
  writePreference(STORAGE_KEYS.skin, skin)
}

export function useTheme() {
  bindSystemThemeListener()
  return {
    theme: computed<Theme>(() => state.theme),
    skin: computed<Skin>(() => state.skin),
    isDark: computed<boolean>(() => state.theme === 'dark'),
    setTheme,
    toggleTheme,
    setSkin,
  }
}
