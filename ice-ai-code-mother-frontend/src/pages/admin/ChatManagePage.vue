<template>
  <div id="chatManagePage" class="admin-page">
    <h1 class="admin-title">{{ t('admin.chat.title') }}</h1>
    <p class="admin-desc">{{ t('admin.chat.desc') }}</p>

    <!-- 工具栏 -->
    <div class="admin-toolbar">
      <a-input-search
        v-model:value="searchParams.message"
        class="admin-search"
        :placeholder="t('admin.chat.searchPh')"
        allow-clear
        @search="onSearch"
      />
      <a-select
        v-model:value="searchParams.messageType"
        class="admin-select"
        :placeholder="t('admin.chat.all')"
        allow-clear
        :options="messageTypeOptions"
        @change="onSearch"
      />
    </div>

    <!-- 表格 -->
    <a-table
      style="margin-top: 16px"
      :columns="columns"
      :data-source="data"
      :loading="loading"
      :pagination="false"
      :scroll="{ x: 1200 }"
      row-key="id"
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
            <a-button type="link" size="small" @click="viewAppChat(record.appId)">
              {{ t('admin.chat.viewChat') }}
            </a-button>
            <a-popconfirm
              :title="t('admin.chat.deleteConfirm')"
              :ok-text="t('common.confirm')"
              :cancel-text="t('common.cancel')"
              @confirm="deleteMessage(record.id)"
            >
              <a-button type="link" danger size="small">{{ t('admin.common.delete') }}</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 分页 -->
    <div class="admin-pagination">
      <a-pagination
        v-model:current="searchParams.pageNum"
        v-model:page-size="searchParams.pageSize"
        :total="total"
        :show-size-changer="false"
        :show-total="(total: number) => t('admin.chat.total', { total })"
        @change="fetchData"
      />
    </div>
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
    width: 90,
  },
  {
    title: t('admin.chat.columns.message'),
    dataIndex: 'message',
    ellipsis: true,
  },
  {
    title: t('admin.chat.columns.messageType'),
    dataIndex: 'messageType',
    width: 110,
  },
  {
    title: t('admin.chat.columns.appId'),
    dataIndex: 'appId',
    width: 90,
  },
  {
    title: t('admin.chat.columns.userId'),
    dataIndex: 'userId',
    width: 90,
  },
  {
    title: t('admin.common.createTime'),
    dataIndex: 'createTime',
    width: 170,
  },
  {
    title: t('admin.common.action'),
    key: 'action',
    width: 150,
  },
])

const messageTypeOptions = computed(() => [
  { label: t('admin.chat.userMsg'), value: 'user' },
  { label: t('admin.chat.aiMsg'), value: 'assistant' },
])

// 数据
const data = ref<API.ChatHistory[]>([])
const total = ref(0)
const loading = ref(false)

// 搜索条件
const searchParams = reactive<API.ChatHistoryQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

// 获取数据
const fetchData = async () => {
  loading.value = true
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
  } finally {
    loading.value = false
  }
}

// 搜索
const onSearch = () => {
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

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
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
