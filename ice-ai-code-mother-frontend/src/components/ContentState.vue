<template>
  <div class="content-state" role="status" aria-live="polite">
    <div v-if="loading" class="content-state__state" :style="{ minHeight }">
      <a-spin />
    </div>

    <div v-else-if="error" class="content-state__state" :style="{ minHeight }">
      <span class="content-state__icon" aria-hidden="true">
        <svg
          width="22"
          height="22"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <circle cx="12" cy="12" r="9" />
          <path d="M12 8v5" />
          <path d="M12 16h.01" />
        </svg>
      </span>
      <p class="content-state__text">{{ error }}</p>
      <button v-if="retryText" type="button" class="content-state__retry" @click="emit('retry')">
        {{ retryText }}
      </button>
    </div>

    <div v-else-if="empty" class="content-state__state" :style="{ minHeight }">
      <slot name="empty">
        <a-empty :description="emptyText" />
      </slot>
    </div>

    <slot v-else />
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    loading?: boolean
    error?: string
    empty?: boolean
    emptyText?: string
    retryText?: string
    minHeight?: string
  }>(),
  {
    loading: false,
    error: '',
    empty: false,
    emptyText: undefined,
    retryText: undefined,
    minHeight: '180px',
  },
)

const emit = defineEmits<{
  (event: 'retry'): void
}>()
</script>

<style scoped>
.content-state {
  position: relative;
}

.content-state__state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 24px 16px;
  text-align: center;
}

.content-state__icon {
  display: inline-flex;
  color: var(--danger);
}

.content-state__text {
  max-width: 420px;
  margin: 0;
  color: var(--text-secondary);
  font-size: var(--text-caption);
}

.content-state__retry {
  height: 32px;
  padding: 0 14px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--text-primary);
  font-size: var(--text-caption);
  font-weight: var(--weight-medium);
  cursor: pointer;
  transition:
    color var(--dur-fast) var(--ease),
    border-color var(--dur-fast) var(--ease),
    background var(--dur-fast) var(--ease);
}

.content-state__retry:hover {
  border-color: var(--brand);
  background: var(--brand-soft);
  color: var(--brand-text);
}
</style>
