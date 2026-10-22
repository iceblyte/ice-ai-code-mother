<template>
  <div class="header-preferences">
    <!-- 明暗主题切换 -->
    <a-tooltip :title="isDark ? t('header.theme.toLight') : t('header.theme.toDark')">
      <a-button
        type="text"
        shape="circle"
        size="small"
        class="pref-btn"
        :aria-label="isDark ? t('header.theme.toLight') : t('header.theme.toDark')"
        @click="toggleTheme"
      >
        <svg
          v-if="isDark"
          class="pref-icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
        </svg>
        <svg
          v-else
          class="pref-icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <circle cx="12" cy="12" r="4" />
          <path
            d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"
          />
        </svg>
      </a-button>
    </a-tooltip>

    <!-- 语言切换（下拉，支持后续扩展语言） -->
    <a-dropdown placement="bottomRight">
      <a-button
        type="text"
        shape="circle"
        size="small"
        class="pref-btn"
        :aria-label="t('header.lang.label')"
      >
        <svg
          class="pref-icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <circle cx="12" cy="12" r="10" />
          <path
            d="M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"
          />
        </svg>
      </a-button>
      <template #overlay>
        <a-menu :selected-keys="[locale]" @click="onLocaleClick">
          <a-menu-item key="zh-CN">简体中文</a-menu-item>
          <a-menu-item key="en-US">English</a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>

    <!-- 外观皮肤切换（仅极简可用，其余置灰待后续皮肤接入） -->
    <a-dropdown placement="bottomRight">
      <a-button
        type="text"
        shape="circle"
        size="small"
        class="pref-btn"
        :aria-label="t('header.skin.label')"
      >
        <svg
          class="pref-icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z" />
        </svg>
      </a-button>
      <template #overlay>
        <a-menu :selected-keys="[skin]" @click="onSkinClick">
          <a-menu-item v-for="item in skinItems" :key="item.key" :disabled="item.disabled">
            {{ item.label }}
            <span v-if="item.disabled" class="skin-coming"> · {{ t('common.comingSoon') }}</span>
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ENABLED_SKINS, SKINS, type AppLocale, type Skin } from '@/constants/uiPreferences'
import { setAppLocale } from '@/i18n'
import { useTheme } from '@/composables/useTheme'

// 语言切换涉及全局 locale，使用全局作用域
const { t, locale } = useI18n({ useScope: 'global' })
const { skin, isDark, toggleTheme, setSkin } = useTheme()

const skinItems = computed(() =>
  SKINS.map((item) => ({
    key: item,
    label: t(`header.skin.names.${item}`),
    disabled: !(ENABLED_SKINS as readonly string[]).includes(item),
  })),
)

const onLocaleClick = ({ key }: { key: string }) => {
  setAppLocale(key as AppLocale)
}

const onSkinClick = ({ key }: { key: string }) => {
  setSkin(key as Skin)
}
</script>

<style scoped>
.header-preferences {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.pref-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.pref-icon {
  width: 16px;
  height: 16px;
}

.skin-coming {
  color: var(--text-muted, #999);
}
</style>
