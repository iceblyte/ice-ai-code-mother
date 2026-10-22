<template>
  <a-modal v-model:open="visible" :title="t('detail.title')" :footer="null" width="500px">
    <div class="app-detail-content">
      <!-- 应用基础信息 -->
      <div class="app-basic-info">
        <div class="info-item">
          <span class="info-label">{{ t('detail.creator') }}</span>
          <UserInfo :user="app?.user" size="small" />
        </div>
        <div class="info-item">
          <span class="info-label">{{ t('detail.createTime') }}</span>
          <span>{{ formatTime(app?.createTime) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">{{ t('detail.genTypeLabel') }}</span>
          <a-tag v-if="app?.codeGenType" class="gen-type-tag">
            {{ formatCodeGenType(app.codeGenType) }}
          </a-tag>
          <span v-else>{{ t('genType.unknown') }}</span>
        </div>
      </div>

      <!-- 操作栏（仅本人或管理员可见） -->
      <div v-if="showActions" class="app-actions">
        <a-space>
          <a-button type="primary" @click="handleEdit">
            <template #icon>
              <EditOutlined />
            </template>
            {{ t('detail.edit') }}
          </a-button>
          <a-button @click="showVersionCompare = true">
            <template #icon>
              <BranchesOutlined />
            </template>
            {{ t('detail.compare') }}
          </a-button>
          <a-popconfirm
            :title="t('detail.deleteConfirm')"
            @confirm="handleDelete"
            :ok-text="t('common.confirm')"
            :cancel-text="t('common.cancel')"
          >
            <a-button danger>
              <template #icon>
                <DeleteOutlined />
              </template>
              {{ t('detail.delete') }}
            </a-button>
          </a-popconfirm>
        </a-space>
      </div>
    </div>
  </a-modal>

  <AppVersionCompareModal v-model:open="showVersionCompare" :app-id="app?.id" />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { EditOutlined, DeleteOutlined, BranchesOutlined } from '@ant-design/icons-vue'
import UserInfo from './UserInfo.vue'
import AppVersionCompareModal from './AppVersionCompareModal.vue'
import { formatTime } from '@/utils/time'
import { formatCodeGenType } from '@/utils/codeGenTypes.ts'

interface Props {
  open: boolean
  app?: API.AppVO
  showActions?: boolean
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'edit'): void
  (e: 'delete'): void
}

const props = withDefaults(defineProps<Props>(), {
  showActions: false,
})

const emit = defineEmits<Emits>()
const { t } = useI18n({ useScope: 'global' })
const showVersionCompare = ref(false)

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
})

const handleEdit = () => {
  emit('edit')
}

const handleDelete = () => {
  emit('delete')
}
</script>

<style scoped>
.app-detail-content {
  padding: 8px 0;
}

.app-basic-info {
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.info-label {
  width: 80px;
  color: var(--text-secondary);
  font-size: 14px;
  flex-shrink: 0;
}

.gen-type-tag {
  color: var(--brand-text);
  background: var(--brand-soft);
  border-color: var(--brand-soft);
}

.app-actions {
  padding-top: 16px;
  border-top: 1px solid var(--border);
}
</style>
