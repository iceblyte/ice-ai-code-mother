<template>
  <div class="header-preferences">
    <!-- 明暗主题切换 -->
    <a-tooltip :title="isDark ? t('header.theme.toLight') : t('header.theme.toDark')">
      <button
        type="button"
        class="pref-iconbtn"
        :aria-label="isDark ? t('header.theme.toLight') : t('header.theme.toDark')"
        :aria-pressed="isDark"
        @click="toggleTheme"
      >
        <svg
          v-if="isDark"
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <circle cx="12" cy="12" r="4.2" />
          <path
            d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M19.1 4.9l-1.4 1.4M6.3 17.7l-1.4 1.4"
          />
        </svg>
        <svg
          v-else
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M20 14.5A8.5 8.5 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5Z" />
        </svg>
      </button>
    </a-tooltip>

    <!-- 语言切换（下拉，支持后续扩展语言；按钮显示 中/EN） -->
    <a-dropdown placement="bottomRight">
      <button
        type="button"
        class="pref-iconbtn"
        :aria-label="t('header.lang.label')"
      >
        <span class="pref-iconbtn__text">{{ locale === 'zh-CN' ? '中' : 'EN' }}</span>
      </button>
      <template #overlay>
        <a-menu :selected-keys="[locale]" @click="onLocaleClick">
          <a-menu-item key="zh-CN">简体中文</a-menu-item>
          <a-menu-item key="en-US">English</a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>

    <!-- 外观皮肤切换（仅极简可用，其余置灰待后续皮肤接入） -->
    <a-dropdown placement="bottomRight">
      <button
        type="button"
        class="pref-iconbtn"
        :aria-label="t('header.skin.label')"
      >
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.7"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path
            d="M12 3a9 9 0 1 0 0 18c1.1 0 1.7-.7 1.7-1.6 0-.8-.5-1.3-.5-2 0-.9.7-1.6 1.7-1.6H17a4 4 0 0 0 4-4A9 9 0 0 0 12 3Z"
          />
          <circle cx="7.4" cy="12.2" r="1.1" fill="currentColor" stroke="none" />
          <circle cx="9.6" cy="7.8" r="1.1" fill="currentColor" stroke="none" />
          <circle cx="14.6" cy="7.3" r="1.1" fill="currentColor" stroke="none" />
        </svg>
      </button>
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
/* 对齐原型 .cn-iconbtn：34px 圆形描边图标按钮 */
.header-preferences {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.pref-iconbtn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 36px;
  min-width: 36px;
  padding: 0 12px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--dur-fast) var(--ease);
}

.pref-iconbtn:hover {
  color: var(--text-primary);
  border-color: var(--border-strong);
  background: var(--bg-surface-muted);
}

.pref-iconbtn__text {
  font-size: var(--text-body);
  font-weight: var(--weight-medium);
  font-family: var(--font-mono);
}

.skin-coming {
  color: var(--text-muted);
}
</style>
