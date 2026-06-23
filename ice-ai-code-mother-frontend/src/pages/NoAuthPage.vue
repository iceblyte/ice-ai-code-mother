<template>
  <div id="noAuthPage">
    <a-result status="403" title="403" sub-title="你没有权限访问当前页面">
      <template #extra>
        <a-button type="primary" @click="router.push('/')">返回首页</a-button>
      </template>
    </a-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>
