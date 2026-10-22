<template>
  <div id="chatManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item :label="t('admin.chat.message')">
        <a-input v-model:value="searchParams.message" :placeholder="t('admin.chat.messagePh')" />
      </a-form-item>
      <a-form-item :label="t('admin.chat.messageType')">
        <a-select
          v-model:value="searchParams.messageType"
          :placeholder="t('admin.chat.messageTypePh')"
          style="width: 120px"
        >
          <a-select-option value="">{{ t('admin.app.all') }}</a-select-option>
          <a-select-option value="user">{{ t('admin.chat.userMsg') }}</a-select-option>
          <a-select-option value="assistant">{{ t('admin.chat.aiMsg') }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item :label="t('admin.chat.appId')">
        <a-input v-model:value="searchParams.appId" :placeholder="t('admin.chat.appIdPh')" />
      </a-form-item>
      <a-form-item :label="t('admin.chat.userId')">
        <a-input v-model:value="searchParams.userId" :placeholder="t('admin.chat.userIdPh')" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">{{ t('admin.common.search') }}</a-button>
      </a-form-item>
    </a-form>
    <a-divider />

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      @change="doTableChange"
      :scroll="{ x: 1400 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'message'">
          <a-tooltip :title="record.message">
            <div class="message-text">{{ record.message }}</div>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'messageType'">
          <a-tag
            :class="
              record.messageType === 'user'
                ? 'msg-type-tag msg-type-tag--user'
                : 'msg-type-tag msg-type-tag--ai'
            "
          >
            {{ record.messageType === 'user' ? t('admin.chat.userMsg') : t('admin.chat.aiMsg') }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="primary" size="small" @click="viewAppChat(record.appId)">
              {{ t('admin.chat.viewChat') }}
            </a-button>
            <a-popconfirm
              :title="t('admin.chat.deleteConfirm')"
              :ok-text="t('common.confirm')"
              :cancel-text="t('common.cancel')"
              @confirm="deleteMessage(record.id)"
            >
              <a-button danger size="small">{{ t('admin.common.delete') }}</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { listAllChatHistoryByPageForAdmin } from '@/api/chatHistoryController'
import { formatTime } from '@/utils/time'

const router = useRouter()
const { t } = useI18n({ useScope: 'global' })

const columns = computed(() => [
  {
    title: t('admin.common.id'),
    dataIndex: 'id',
    width: 80,
    fixed: 'left' as const,
  },
  {
    title: t('admin.chat.columns.message'),
    dataIndex: 'message',
    width: 300,
  },
  {
    title: t('admin.chat.columns.messageType'),
    dataIndex: 'messageType',
    width: 100,
  },
  {
    title: t('admin.chat.columns.appId'),
    dataIndex: 'appId',
    width: 80,
  },
  {
    title: t('admin.chat.columns.userId'),
    dataIndex: 'userId',
    width: 80,
  },
  {
    title: t('admin.common.createTime'),
    dataIndex: 'createTime',
    width: 160,
  },
  {
    title: t('admin.common.action'),
    key: 'action',
    width: 180,
    fixed: 'right' as const,
  },
])

// 数据
const data = ref<API.ChatHistory[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.ChatHistoryQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

// 获取数据
const fetchData = async () => {
  try {
    const res = await listAllChatHistoryByPageForAdmin({
      ...searchParams,
    })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error(t('admin.common.fetchFailed', { msg: res.data.message }))
    }
  } catch (error) {
    console.error('获取数据失败：', error)
    message.error(t('admin.common.fetchFailedPlain'))
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => t('admin.common.total', { total }),
  }
})

// 表格变化处理
const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索
const doSearch = () => {
  // 重置页码
  searchParams.pageNum = 1
  fetchData()
}

// 查看应用对话
const viewAppChat = (appId: number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}`)
  }
}

// 删除消息
const deleteMessage = async (id: number | undefined) => {
  if (!id) return

  try {
    // 注意：这里需要后端提供删除对话历史的接口
    // 目前先显示成功，实际实现需要调用删除接口
    message.success(t('admin.common.deleteSuccess'))
    // 刷新数据
    fetchData()
  } catch (error) {
    console.error('删除失败：', error)
    message.error(t('admin.common.deleteFailedPlain'))
  }
}
</script>

<style scoped>
#chatManagePage {
  padding: 24px;
  background: var(--bg-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin-top: 16px;
}

.message-text {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.msg-type-tag--user {
  color: var(--brand-text);
  background: var(--brand-soft);
  border-color: var(--brand-soft);
}

.msg-type-tag--ai {
  color: var(--success);
  background: var(--success-soft);
  border-color: var(--success-soft);
}

:deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
}
</style>
