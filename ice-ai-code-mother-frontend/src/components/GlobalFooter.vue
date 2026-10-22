<template>
  <a-layout-footer class="footer">
    <div class="footer-content">
      <p class="copyright">
        <a
          href="https://github.com/iceblyte"
          target="_blank"
          rel="noopener noreferrer"
          class="author-link"
        >
          {{ t('footer.openSourceBy') }}
        </a>
      </p>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n({ useScope: 'global' })
</script>

<style scoped>
.footer {
  background: var(--bg-surface);
  backdrop-filter: blur(10px);
  text-align: center;
  padding: 20px;
  margin-top: 40px;
  border-top: 1px solid var(--border);
}

.copyright {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}
</style>
