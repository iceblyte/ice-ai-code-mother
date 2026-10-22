<template>
  <a-layout class="basic-layout">
    <!-- 顶部导航栏 -->
    <GlobalHeader />
    <!-- 主要内容区域 -->
    <a-layout-content class="main-content">
      <router-view />
    </a-layout-content>
    <!-- 底部版权信息（对话/编辑工作台为固定视口布局，不显示页脚） -->
    <GlobalFooter v-if="!isWorkspace" />
  </a-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'

const route = useRoute()

// 工作台页面（对话/编辑）占满视口剩余高度，外围不滚动
const isWorkspace = computed(() => route.path.startsWith('/app/'))
</script>

<style scoped>
.basic-layout {
  background: none;
}

.main-content {
  width: 100%;
  padding: 0;
  background: none;
  margin: 0;
}
</style>
