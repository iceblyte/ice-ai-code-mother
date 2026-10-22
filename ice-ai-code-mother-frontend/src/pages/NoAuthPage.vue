<template>
  <div id="noAuthPage">
    <a-result status="403" title="403" :sub-title="t('noauth.subTitle')">
      <template #extra>
        <a-button type="primary" @click="router.push('/')">{{ t('noauth.backHome') }}</a-button>
      </template>
    </a-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const { t } = useI18n({ useScope: 'global' })
</script>
