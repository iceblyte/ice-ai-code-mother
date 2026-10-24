<template>
  <a-layout class="basic-layout">
    <!-- 工作台（对话页）：全局顶栏隐藏，悬停 logo 时以浮层滑出 -->
    <div
      v-if="isChatWorkspace"
      class="header-reveal"
      :class="{ 'header-reveal--open': navOpen }"
      @mouseenter="cancelClose"
      @mouseleave="scheduleClose"
    >
      <GlobalHeader />
    </div>
    <GlobalHeader v-else />
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
import { useWorkspaceNav } from '@/composables/useWorkspaceNav'

const route = useRoute()
const { navOpen, scheduleClose, cancelClose } = useWorkspaceNav()

// 工作台页面（对话/编辑）占满视口剩余高度，外围不滚动
const isWorkspace = computed(() => route.path.startsWith('/app/'))

// 对话工作台：隐藏常驻顶栏，改由 logo 悬停唤出
const isChatWorkspace = computed(() => route.path.startsWith('/app/chat/'))
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

/* 工作台唤出顶栏：固定浮层，滑入滑出 */
.header-reveal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  transform: translateY(-100%);
  transition: transform var(--dur) var(--ease);
  box-shadow: var(--shadow-lg);
  pointer-events: none;
}

.header-reveal--open {
  transform: translateY(0);
  pointer-events: auto;
}
</style>
