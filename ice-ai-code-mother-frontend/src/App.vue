<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import enUS from 'ant-design-vue/es/locale/en_US'
import BasicLayout from '@/layouts/BasicLayout.vue'
import { useTheme } from '@/composables/useTheme'
import { getAntdTheme } from '@/styles/themeConfig'

// 全局语言作用域：随切换器实时响应
const { locale } = useI18n({ useScope: 'global' })
const { skin, theme: themeName } = useTheme()

// 「皮肤 × 明暗」→ antd design token
const antdTheme = computed(() => getAntdTheme(skin.value, themeName.value))
const antdLocale = computed(() => (locale.value === 'zh-CN' ? zhCN : enUS))
</script>

<template>
  <a-config-provider :theme="antdTheme" :locale="antdLocale">
    <BasicLayout />
  </a-config-provider>
</template>

<style>
/* 全局样式优化 */
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family: var(--font-sans);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
}

html {
  overflow-x: hidden;
}
</style>
