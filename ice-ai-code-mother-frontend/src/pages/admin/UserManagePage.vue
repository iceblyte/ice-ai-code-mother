<template>
  <div id="userManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item :label="t('admin.user.account')">
        <a-input v-model:value="searchParams.userAccount" :placeholder="t('admin.user.accountPh')" />
      </a-form-item>
      <a-form-item :label="t('admin.user.userName')">
        <a-input v-model:value="searchParams.userName" :placeholder="t('admin.user.userNamePh')" />
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
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="primary" @click="openEditModal(record)">
              {{ t('admin.common.edit') }}
            </a-button>
            <a-button danger @click="doDelete(record.id)">{{ t('admin.common.delete') }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

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
  },
  {
    title: t('admin.user.columns.userAccount'),
    dataIndex: 'userAccount',
  },
  {
    title: t('admin.user.columns.userName'),
    dataIndex: 'userName',
  },
  {
    title: t('admin.user.columns.userAvatar'),
    dataIndex: 'userAvatar',
  },
  {
    title: t('admin.user.columns.userProfile'),
    dataIndex: 'userProfile',
  },
  {
    title: t('admin.user.columns.userRole'),
    dataIndex: 'userRole',
  },
  {
    title: t('admin.common.createTime'),
    dataIndex: 'createTime',
  },
  {
    title: t('admin.common.action'),
    key: 'action',
  },
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

// 获取数据
const fetchData = async () => {
  const res = await listUserVoByPage({
    ...searchParams,
  })
  if (res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error(t('admin.common.fetchFailed', { msg: res.data.message }))
  }
}

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

// 表格分页变化时的操作
const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索数据
const doSearch = () => {
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
#userManagePage {
  padding: 24px;
  background: var(--bg-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin-top: 16px;
}

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
</style>
