<template>
  <div id="userManagePage" class="admin-page">
    <h1 class="admin-title">{{ t('admin.user.title') }}</h1>
    <p class="admin-desc">{{ t('admin.user.desc') }}</p>

    <!-- 工具栏 -->
    <div class="admin-toolbar">
      <a-input-search
        v-model:value="searchKey"
        class="admin-search"
        :placeholder="t('admin.user.searchPh')"
        allow-clear
        @search="onSearch"
      />
      <a-select
        v-model:value="roleFilter"
        class="admin-select"
        :options="roleFilterOptions"
      />
    </div>

    <!-- 表格 -->
    <a-table
      style="margin-top: 16px"
      :columns="columns"
      :data-source="filteredData"
      :loading="loading"
      :pagination="false"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userAvatar'">
          <a-image :src="record.userAvatar" :width="120" />
        </template>
        <template v-else-if="column.dataIndex === 'userRole'">
          <a-tag v-if="record.userRole === 'admin'" class="role-tag role-tag--admin">
            {{ t('admin.user.roleAdmin') }}
          </a-tag>
          <a-tag v-else class="role-tag role-tag--user">
            {{ t('admin.user.roleUser') }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          <span class="pf-mono admin-mono">{{ dayjs(record.createTime).format('YYYY-MM-DD') }}</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="openEditModal(record)">
              {{ t('admin.common.edit') }}
            </a-button>
            <a-popconfirm
              :title="t('admin.user.deleteConfirm')"
              :ok-text="t('common.confirm')"
              :cancel-text="t('common.cancel')"
              @confirm="doDelete(record.id)"
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
        :show-total="(total: number) => t('admin.user.total', { total })"
        @change="fetchData"
      />
    </div>

    <a-modal
      v-model:open="editModalOpen"
      :title="t('admin.user.editTitle')"
      :ok-text="t('admin.user.save')"
      :cancel-text="t('common.cancel')"
      @ok="handleEditSubmit"
    >
      <a-form layout="vertical" :model="editForm">
        <a-form-item :label="t('admin.user.userName')">
          <a-input v-model:value="editForm.userName" :placeholder="t('admin.user.userNameEditPh')" />
        </a-form-item>
        <a-form-item :label="t('admin.user.avatar')">
          <a-input v-model:value="editForm.userAvatar" :placeholder="t('admin.user.avatarPh')" />
        </a-form-item>
        <a-form-item :label="t('admin.user.bio')">
          <a-textarea
            v-model:value="editForm.userProfile"
            :placeholder="t('admin.user.bioPh')"
            :auto-size="{ minRows: 3, maxRows: 5 }"
          />
        </a-form-item>
        <a-form-item :label="t('admin.user.roleLabel')">
          <a-select v-model:value="editForm.userRole" :options="userRoleOptions" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { deleteUser, listUserVoByPage, updateUser } from '@/api/userController'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'

const { t } = useI18n({ useScope: 'global' })

const columns = computed(() => [
  {
    title: t('admin.common.id'),
    dataIndex: 'id',
    width: 90,
  },
  {
    title: t('admin.user.columns.userName'),
    dataIndex: 'userName',
    ellipsis: true,
  },
  {
    title: t('admin.user.columns.userAccount'),
    dataIndex: 'userAccount',
    width: 140,
  },
  {
    title: t('admin.user.columns.userRole'),
    dataIndex: 'userRole',
    width: 100,
  },
  {
    title: t('admin.common.createTime'),
    dataIndex: 'createTime',
    width: 130,
  },
  {
    title: t('admin.common.action'),
    key: 'action',
    width: 140,
  },
])

const roleFilterOptions = computed(() => [
  { label: t('admin.user.roleAll'), value: '' },
  { label: t('admin.user.roleAdmin'), value: 'admin' },
  { label: t('admin.user.roleUser'), value: 'user' },
])

const userRoleOptions = computed(() => [
  {
    label: t('admin.user.roleUser'),
    value: 'user',
  },
  {
    label: t('admin.user.roleAdmin'),
    value: 'admin',
  },
])

const data = ref<API.UserVO[]>([])
const total = ref(0)
const loading = ref(false)
const editModalOpen = ref(false)
const editForm = reactive<API.UserUpdateRequest>({
  id: undefined,
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
})

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})
const searchKey = ref('')
const roleFilter = ref('')

// 角色为前端字段过滤（后端暂不支持 userRole 查询参数，仅作用于当前页）
const filteredData = computed(() => {
  if (!roleFilter.value) return data.value
  return data.value.filter((user) => user.userRole === roleFilter.value)
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listUserVoByPage({
      ...searchParams,
      userName: searchKey.value.trim() || undefined,
    })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error(t('admin.common.fetchFailed', { msg: res.data.message }))
    }
  } finally {
    loading.value = false
  }
}

// 搜索数据
const onSearch = () => {
  // 重置页码
  searchParams.pageNum = 1
  fetchData()
}

// 删除数据
const doDelete = async (id?: number) => {
  if (!id) {
    return
  }
  const res = await deleteUser({ id })
  if (res.data.code === 0) {
    message.success(t('admin.common.deleteSuccess'))
    // 刷新数据
    await fetchData()
  } else {
    message.error(t('admin.common.deleteFailed', { msg: res.data.message }))
  }
}

// 打开编辑弹窗
const openEditModal = (record: API.UserVO) => {
  editForm.id = record.id
  editForm.userName = record.userName
  editForm.userAvatar = record.userAvatar
  editForm.userProfile = record.userProfile
  editForm.userRole = record.userRole
  editModalOpen.value = true
}

// 提交编辑
const handleEditSubmit = async () => {
  const res = await updateUser({
    ...editForm,
  })
  if (res.data.code === 0) {
    message.success(t('admin.user.updateSuccess'))
    editModalOpen.value = false
    await fetchData()
  } else {
    message.error(t('admin.user.updateFailed', { msg: res.data.message }))
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.role-tag--admin {
  color: var(--success);
  background: var(--success-soft);
  border-color: var(--success-soft);
}

.role-tag--user {
  color: var(--brand-text);
  background: var(--brand-soft);
  border-color: var(--brand-soft);
}

.admin-mono {
  font-family: var(--font-mono);
}
</style>
