<template>
  <div v-show="active" class="typewriter-placeholder" aria-hidden="true">
    <span class="typewriter-placeholder__text">{{ text }}</span
    ><span class="typewriter-placeholder__caret">|</span>
  </div>
</template>

<script setup lang="ts">
import { useTypewriter } from '@/composables/useTypewriter'

const props = withDefaults(
  defineProps<{
    /** 循环展示的文案列表（由 i18n 提供，切换语言后自动重置） */
    lines: string[]
    /** 输入框为空时为 true；有内容时暂停并隐藏 */
    active?: boolean
    typeDelay?: number
    deleteDelay?: number
    holdDelay?: number
  }>(),
  {
    active: true,
    typeDelay: 80,
    deleteDelay: 40,
    holdDelay: 1500,
  },
)

const { text } = useTypewriter(() => props.lines, () => props.active, {
  type: props.typeDelay,
  delete: props.deleteDelay,
  hold: props.holdDelay,
})
</script>

<style scoped>
.typewriter-placeholder {
  position: absolute;
  top: 20px;
  left: 20px;
  right: 20px;
  z-index: 1;
  max-height: calc(1.6em * 3);
  overflow: hidden;
  pointer-events: none;
  font-size: var(--text-title);
  line-height: 1.6;
  color: var(--text-muted);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.typewriter-placeholder__caret {
  display: inline-block;
  margin-left: 1px;
  font-weight: var(--weight-regular);
  animation: typewriter-caret 1s steps(2, start) infinite;
}

@keyframes typewriter-caret {
  0%,
  49% {
    opacity: 1;
  }

  50%,
  100% {
    opacity: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .typewriter-placeholder__caret {
    animation: none;
    opacity: 1;
  }
}

@media (max-width: 860px) {
  .typewriter-placeholder {
    top: 16px;
    left: 16px;
    right: 16px;
  }
}
</style>
