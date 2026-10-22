/**
 * UI 偏好（主题 / 语言 / 外观皮肤）常量与安全读写
 * 注意：index.html 内联防 FOUC 脚本镜像了这里的 key 与取值范围，两处需同步修改
 */

export type Theme = 'light' | 'dark'
export type AppLocale = 'zh-CN' | 'en-US'
export type Skin = 'minimal' | 'codenow' | 'cohere' | 'dell'

/** localStorage key 集中定义，禁止散落硬编码 */
export const STORAGE_KEYS = {
  theme: 'codenow-theme',
  lang: 'codenow-lang',
  skin: 'codenow-skin',
} as const

export const THEMES = ['light', 'dark'] as const
export const SUPPORT_LOCALES = ['zh-CN', 'en-US'] as const
export const SKINS = ['minimal', 'codenow', 'cohere', 'dell'] as const
/** 当前已实现的皮肤（其余在切换器中置灰） */
export const ENABLED_SKINS = ['minimal'] as const

export const DEFAULT_THEME: Theme = 'light'
export const DEFAULT_LOCALE: AppLocale = 'zh-CN'
export const DEFAULT_SKIN: Skin = 'minimal'

/** 读取并校验 localStorage 值，非法或缺省返回 null（由调用方回落默认值） */
export function readPreference<T extends string>(key: string, allowed: readonly T[]): T | null {
  try {
    const raw = localStorage.getItem(key)
    if (raw && (allowed as readonly string[]).includes(raw)) {
      return raw as T
    }
    return null
  } catch {
    return null
  }
}

export function writePreference(key: string, value: string): void {
  try {
    localStorage.setItem(key, value)
  } catch {
    /* 存储不可用时静默降级 */
  }
}

export function hasStoredPreference(key: string): boolean {
  try {
    return localStorage.getItem(key) !== null
  } catch {
    return false
  }
}

/** 语言探测：已存偏好 → 浏览器语言匹配 → 默认 zh-CN */
export function detectLocale(): AppLocale {
  const stored = readPreference<AppLocale>(STORAGE_KEYS.lang, SUPPORT_LOCALES)
  if (stored) return stored
  const nav = (typeof navigator !== 'undefined' && navigator.language) || ''
  const lower = nav.toLowerCase()
  if (lower.startsWith('zh')) return 'zh-CN'
  if (lower.startsWith('en')) return 'en-US'
  return DEFAULT_LOCALE
}
