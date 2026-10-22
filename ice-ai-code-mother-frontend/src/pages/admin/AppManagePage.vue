<template>
  <div id="appManagePage" class="admin-page">
    <h1 class="admin-title">{{ t('admin.app.title') }}</h1>
    <p class="admin-desc">{{ t('admin.app.desc') }}</p>

    <!-- 工具栏 -->
    <div class="admin-toolbar">
      <a-input-search
        v-model:value="searchKey"
        class="admin-search"
        :placeholder="t('admin.app.searchPh')"
        allow-clear
        @search="onSearch"
      />
      <a-select
        v-model:value="searchParams.codeGenType"
        class="admin-select"
        :placeholder="t('admin.app.all')"
        allow-clear
        :options="genTypeSelectOptions"
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
        <template v-if="column.dataIndex === 'cover'">
          <a-image v-if="record.cover" :src="record.cover" :width="80" :height="60" />
          <div v-else class="no-cover">{{ t('admin.app.noCover') }}</div>
        </template>
        <template v-else-if="column.dataIndex === 'initPrompt'">
          <a-tooltip :title="record.initPrompt">
            <div class="prompt-text">{{ record.initPrompt }}</div>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'codeGenType'">
          {{ formatCodeGenType(record.codeGenType) }}
        </template>
        <template v-else-if="column.dataIndex === 'priority'">
          <a-tag v-if="record.priority === 99" class="featured-tag">
            {{ t('admin.app.featured') }}
          </a-tag>
          <span v-else>{{ record.priority || 0 }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'deployedTime'">
          <span v-if="record.deployedTime">
            {{ formatTime(record.deployedTime) }}
          </span>
          <span v-else class="text-muted">{{ t('admin.app.notDeployed') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatTime(record.createTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'user'">
          <UserInfo :user="record.user" size="small" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="editApp(record)">
              {{ t('admin.common.edit') }}
            </a-button>
            <a-button
              type="link"
              size="small"
              @click="toggleFeatured(record)"
              :class="{ 'featured-btn': record.priority === 99 }"
            >
              {{ record.priority === 99 ? t('admin.app.unfeatured') : t('admin.app.featured') }}
            </a-button>
            <a-popconfirm
              :title="t('admin.app.deleteConfirm')"
              :ok-text="t('common.confirm')"
              :cancel-text="t('common.cancel')"
              @confirm="deleteApp(record.id)"
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
        :show-total="(total: number) => t('admin.app.total', { total })"
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
import { listAppVoByPageByAdmin, deleteAppByAdmin, updateAppByAdmin } from '@/api/appController'
import { getCodeGenTypeOptions, formatCodeGenType } from '@/utils/codeGenTypes'
import { formatTime } from '@/utils/time'
import UserInfo from '@/components/UserInfo.vue'

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
    title: t('admin.app.columns.appName'),
    dataIndex: 'appName',
    width: 150,
    ellipsis: true,
  },
  {
    title: t('admin.app.columns.cover'),
    dataIndex: 'cover',
    width: 100,
  },
  {
    title: t('admin.app.columns.initPrompt'),
    dataIndex: 'initPrompt',
    width: 200,
  },
  {
    title: t('admin.app.columns.codeGenType'),
    dataIndex: 'codeGenType',
    width: 130,
  },
  {
    title: t('admin.app.columns.priority'),
    dataIndex: 'priority',
    width: 90,
  },
  {
    title: t('admin.app.columns.deployedTime'),
    dataIndex: 'deployedTime',
    width: 160,
  },
  {
    title: t('admin.app.columns.user'),
    dataIndex: 'user',
    width: 120,
  },
  {
    title: t('admin.common.createTime'),
    dataIndex: 'createTime',
    width: 160,
  },
  {
    title: t('admin.common.action'),
    key: 'action',
    width: 190,
    fixed: 'right' as const,
  },
])

// 生成类型选项（随语言响应）
const genTypeOptions = computed(() => getCodeGenTypeOptions())

const genTypeSelectOptions = computed(() => [
  { label: t('admin.app.all'), value: '' },
  ...genTypeOptions.value,
])

// 数据
const data = ref<API.AppVO[]>([])
const total = ref(0)
const loading = ref(false)

// 搜索条件
const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})
const searchKey = ref('')

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAppVoByPageByAdmin({
      ...searchParams,
      appName: searchKey.value.trim() || undefined,
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

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})

// 搜索
const onSearch = () => {
  // 重置页码
  searchParams.pageNum = 1
  fetchData()
}

// 编辑应用
const editApp = (app: API.AppVO) => {
  router.push(`/app/edit/${app.id}`)
}

// 切换精选状态
const toggleFeatured = async (app: API.AppVO) => {
  if (!app.id) return

  const newPriority = app.priority === 99 ? 0 : 99

  try {
    const res = await updateAppByAdmin({
      id: app.id,
      priority: newPriority,
    })

    if (res.data.code === 0) {
      message.success(newPriority === 99 ? t('admin.app.featuredSet') : t('admin.app.featuredUnset'))
      // 刷新数据
      fetchData()
    } else {
      message.error(t('admin.app.opFailed', { msg: res.data.message }))
    }
  } catch (error) {
    console.error('操作失败：', error)
    message.error(t('admin.app.opFailedPlain'))
  }
}

// 删除应用
const deleteApp = async (id: number | undefined) => {
  if (!id) return

  try {
    const res = await deleteAppByAdmin({ id })
    if (res.data.code === 0) {
      message.success(t('admin.common.deleteSuccess'))
      // 刷新数据
      fetchData()
    } else {
      message.error(t('admin.common.deleteFailed', { msg: res.data.message }))
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error(t('admin.common.deleteFailedPlain'))
  }
}
</script>

<style scoped>
.no-cover {
  width: 80px;
  height: 60px;
  background: var(--bg-surface-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 12px;
  border-radius: 4px;
}

.prompt-text {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-muted {
  color: var(--text-muted);
}

.featured-tag {
  color: var(--warning);
  background: var(--warning-soft);
  border-color: var(--warning-soft);
}

.featured-btn {
  color: var(--warning);
}

.featured-btn:hover {
  color: color-mix(in srgb, var(--warning) 85%, #000);
}

:deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
}
</style>
