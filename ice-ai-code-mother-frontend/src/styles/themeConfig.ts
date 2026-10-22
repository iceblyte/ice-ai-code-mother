import { theme } from 'ant-design-vue'
import type { Skin, Theme } from '@/constants/uiPreferences'

/**
 * antd ConfigProvider 主题映射：把「皮肤 × 明暗」翻译成 antd design token。
 * 色值与 styles/tokens.css、styles/skins/*.css 保持一致（来源：docs/prototype）。
 * 新增皮肤：在此补充一行映射 + 新增 skins/*.css 文件即可，无需改业务代码。
 */

type Algorithm = (typeof theme)['darkAlgorithm'] | (typeof theme)['defaultAlgorithm']

export interface AppAntdTheme {
  algorithm?: Algorithm
  token: Record<string, string | number>
  components?: Record<string, Record<string, string | number>>
}

const RADIUS = {
  borderRadiusSM: 4,
  borderRadius: 8,
  borderRadiusLG: 16,
} as const

const LAYOUT_COMPONENTS = {
  Layout: { headerBg: 'transparent', bodyBg: 'transparent' },
} as const

/** minimal · 极简（亮：#0066CC / 暗：#5AB0FF） */
const MINIMAL: Record<Theme, AppAntdTheme> = {
  light: {
    algorithm: theme.defaultAlgorithm,
    token: {
      colorPrimary: '#0066CC',
      colorLink: '#0066CC',
      colorInfo: '#0066CC',
      colorSuccess: '#0B8A66',
      colorWarning: '#8A5300',
      colorError: '#C8372D',
      colorBgBase: '#FFFFFF',
      colorBgLayout: '#FFFFFF',
      colorBgContainer: '#FFFFFF',
      colorBgElevated: '#FFFFFF',
      colorTextBase: '#1D1D1F',
      colorTextSecondary: '#4B4B52',
      colorTextTertiary: '#86868B',
      colorBorder: '#D2D2D7',
      colorBorderSecondary: '#E6E6EB',
      ...RADIUS,
    },
    components: { ...LAYOUT_COMPONENTS },
  },
  dark: {
    algorithm: theme.darkAlgorithm,
    token: {
      colorPrimary: '#5AB0FF',
      colorLink: '#5AB0FF',
      colorInfo: '#5AB0FF',
      colorSuccess: '#2BD3A3',
      colorWarning: '#E0B060',
      colorError: '#F2726A',
      colorBgBase: '#000000',
      colorBgLayout: '#000000',
      colorBgContainer: '#161618',
      colorBgElevated: '#1C1C1F',
      colorTextBase: '#F5F5F7',
      colorTextSecondary: '#C7C7CC',
      colorTextTertiary: '#8E8E93',
      colorBorder: '#424249',
      colorBorderSecondary: '#2E2E33',
      ...RADIUS,
    },
    components: { ...LAYOUT_COMPONENTS },
  },
}

/** 皮肤 → antd 主题映射表（尚未实现的皮肤回落到 minimal） */
const ANTD_THEME_MAP: Partial<Record<Skin, Record<Theme, AppAntdTheme>>> = {
  minimal: MINIMAL,
}

export function getAntdTheme(skin: Skin, name: Theme): AppAntdTheme {
  const bySkin = ANTD_THEME_MAP[skin] ?? MINIMAL
  return bySkin[name] ?? MINIMAL.light
}
