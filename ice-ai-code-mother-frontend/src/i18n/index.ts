import { createI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import zhCN from './locales/zh-CN.json'
import enUS from './locales/en-US.json'
import {
  STORAGE_KEYS,
  SUPPORT_LOCALES,
  detectLocale,
  writePreference,
  type AppLocale,
} from '@/constants/uiPreferences'

export type { AppLocale }
export { SUPPORT_LOCALES }

/** Composition API 模式（legacy: false），全局作用域单例 */
const i18n = createI18n({
  legacy: false,
  locale: detectLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
})

/** 应用语言到运行环境：i18n 实例 + html lang + 页面标题 + dayjs */
function applyLocale(locale: AppLocale): void {
  document.documentElement.setAttribute('lang', locale)
  document.title = i18n.global.t('site.title')
  dayjs.locale(locale === 'zh-CN' ? 'zh-cn' : 'en')
}

/** 切换语言并持久化 */
export function setAppLocale(locale: AppLocale): void {
  if (!(SUPPORT_LOCALES as readonly string[]).includes(locale)) return
  i18n.global.locale.value = locale
  writePreference(STORAGE_KEYS.lang, locale)
  applyLocale(locale)
}

// 初始应用（与 index.html 内联脚本的探测结果保持一致）
applyLocale(detectLocale())

export default i18n
