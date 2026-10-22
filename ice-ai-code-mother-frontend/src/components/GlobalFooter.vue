<template>
  <a-layout-footer class="footer">
    <div class="footer-inner">
      <div class="footer-main">
        <!-- 品牌列 -->
        <div class="footer-brand">
          <RouterLink to="/" class="footer-logo" aria-label="CodeNow">
            <BrandLogo :mark-width="30" :word-height="20" />
          </RouterLink>
          <p class="footer-tagline">{{ t('footer.tagline') }}</p>
        </div>

        <!-- 链接列（仅真实可达页面） -->
        <div class="footer-links">
          <div class="footer-col">
            <h4>{{ t('footer.product') }}</h4>
            <RouterLink to="/">{{ t('footer.home') }}</RouterLink>
            <RouterLink to="/user/profile">{{ t('footer.myapps') }}</RouterLink>
          </div>
          <div class="footer-col">
            <h4>{{ t('footer.resources') }}</h4>
            <a
              href="https://github.com/iceblyte"
              target="_blank"
              rel="noopener noreferrer"
            >
              GitHub
            </a>
          </div>
        </div>
      </div>

      <div class="footer-bottom">
        <span>© 2026 CodeNow. {{ t('footer.rights') }}</span>
        <span>{{ t('footer.openSourceBy') }}</span>
      </div>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import BrandLogo from '@/components/BrandLogo.vue'

const { t } = useI18n({ useScope: 'global' })
</script>

<style scoped>
/* 对齐原型 .cn-footer：品牌列 + 链接列 + 底部条 */
.footer {
  border-top: 1px solid var(--border);
  background: var(--bg-surface);
  margin-top: 48px;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.footer-main {
  display: flex;
  flex-wrap: wrap;
  gap: 64px;
  justify-content: space-between;
  padding: 64px 0 48px;
}

.footer-brand {
  max-width: 320px;
}

.footer-logo {
  display: inline-flex;
}

.footer-tagline {
  margin: 20px 0 0;
  font-size: var(--text-body);
  line-height: var(--leading-body);
  color: var(--text-muted);
}

.footer-links {
  display: flex;
  flex-wrap: wrap;
  gap: 72px;
}

.footer-col h4 {
  font-size: var(--text-title);
  font-weight: var(--weight-strong);
  color: var(--text-primary);
  margin: 0 0 16px;
}

.footer-col a {
  display: block;
  font-size: var(--text-body);
  color: var(--text-muted);
  padding: 6px 0;
}

.footer-col a:hover {
  color: var(--text-primary);
}

.footer-bottom {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: space-between;
  padding: 20px 0 40px;
  border-top: 1px solid var(--border);
  font-size: var(--text-caption);
  color: var(--text-muted);
}
</style>
