import { theme } from 'ant-design-vue'
import { generate } from '@ant-design/colors'
import type { Skin, Theme } from '@/constants/uiPreferences'

/**
 * antd ConfigProvider 主题映射：把「皮肤 × 明暗」翻译成 antd design token。
 * 色值与 styles/tokens.css、styles/skins/*.css 保持一致（来源：docs/prototype）。
 * 新增皮肤：在此补充一行映射 + 新增 skins/*.css 文件即可，无需改业务代码。
 *
 * ⚠️ ant-design-vue 4 的 token 合并机制：
 *   theme.token 的色值只作为「种子」进入算法重新推导（map 层色板第 6 阶），
 *   并不会平铺覆盖派生结果。暗色下 map = 0.85 × seed + 0.15 × #141414，
 *   因此暗色品牌填充色需先反解种子（solveDarkSeed），
 *   再对最显眼的主行动按钮（Button）做组件级令牌精确锁定。
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

/**
 * 反解暗色种子：使 darkAlgorithm 派生的色板第 6 阶尽量逼近目标品牌色。
 * 暗色混合公式：output = 0.85 × seed + 0.15 × #141414（蓝通道受底色钳制，
 * 亮度高于 #DB 蓝的目标色无法精确命中，返回可达的最近色）。
 */
function solveDarkSeed(target: string): string {
  const toRgb = (hex: string): number[] =>
    [1, 3, 5].map(i => parseInt(hex.slice(i, i + 2), 16))
  const toHex = (rgb: number[]): string =>
    '#' +
    rgb
      .map(v => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, '0'))
      .join('')
  const seed = toHex(toRgb(target).map(v => (v - 0.15 * 20) / 0.85))
  // 校验反解结果（生成失败时回退为原目标色作为种子）
  const achieved = generate(seed, { theme: 'dark' })[5]
  return achieved ? seed : target
}

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
      // 暗色品牌/语义色均为目标值反解后的种子，使派生结果贴近原型令牌
      colorPrimary: solveDarkSeed('#5AB0FF'),
      colorLink: solveDarkSeed('#5AB0FF'),
      colorInfo: solveDarkSeed('#5AB0FF'),
      colorSuccess: solveDarkSeed('#2BD3A3'),
      colorWarning: solveDarkSeed('#E0B060'),
      colorError: solveDarkSeed('#F2726A'),
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
    components: {
      ...LAYOUT_COMPONENTS,
      // 主行动按钮精确锁定品牌色（原型 --brand / --brand-hover / --brand-on）；
      // 其余组件（分页选中、聚焦环等）使用反解种子派生色
      Button: {
        colorPrimary: '#5AB0FF',
        colorPrimaryHover: '#8CC8FF',
        colorPrimaryActive: '#2997FF',
        colorTextLightSolid: '#04121F',
      },
    },
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
