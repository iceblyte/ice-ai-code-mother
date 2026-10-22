import i18n from '@/i18n'

/**
 * 代码生成类型枚举
 * 展示文案统一由前端 i18n 层管理（genType.*），后端仅返回稳定 value
 */
export enum CodeGenTypeEnum {
  HTML = 'html',
  MULTI_FILE = 'multi_file',
  VUE_PROJECT = 'vue_project',
}

const GEN_TYPE_KEYS: Record<CodeGenTypeEnum, string> = {
  [CodeGenTypeEnum.HTML]: 'genType.html',
  [CodeGenTypeEnum.MULTI_FILE]: 'genType.multiFile',
  [CodeGenTypeEnum.VUE_PROJECT]: 'genType.vueProject',
}

/**
 * 代码生成类型选项（用于下拉选择，随语言响应；请在 computed 中调用）
 */
export const getCodeGenTypeOptions = () =>
  Object.values(CodeGenTypeEnum).map((value) => ({
    label: i18n.global.t(GEN_TYPE_KEYS[value]),
    value,
  }))

/**
 * 代码生成类型选项（模块加载时求值，兼容既有调用点；新代码请用 getCodeGenTypeOptions）
 */
export const CODE_GEN_TYPE_OPTIONS = getCodeGenTypeOptions()

/**
 * 格式化代码生成类型
 * @param type 代码生成类型
 * @returns 格式化后的类型描述
 */
export const formatCodeGenType = (type: string | undefined): string => {
  if (!type) return i18n.global.t('genType.unknown')

  const key = GEN_TYPE_KEYS[type as CodeGenTypeEnum]
  return key ? i18n.global.t(key) : type
}

/**
 * 获取所有代码生成类型
 */
export const getAllCodeGenTypes = () => {
  return Object.values(CodeGenTypeEnum)
}

/**
 * 检查是否为有效的代码生成类型
 * @param type 待检查的类型
 */
export const isValidCodeGenType = (type: string): type is CodeGenTypeEnum => {
  return Object.values(CodeGenTypeEnum).includes(type as CodeGenTypeEnum)
}
