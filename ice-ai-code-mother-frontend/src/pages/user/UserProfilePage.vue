<template>
  <div id="userProfilePage">
    <!-- 页头 -->
    <h1 class="pf-title">{{ t('profile.title') }}</h1>
    <p class="pf-desc">{{ t('profile.desc') }}</p>

    <!-- 用户信息卡 -->
    <div class="pf-usercard">
      <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="48">
        {{ loginUserStore.loginUser.userName?.charAt(0) || 'U' }}
      </a-avatar>
      <div class="pf-usercard__info">
        <h2 class="pf-usercard__name">{{ loginUserStore.loginUser.userName || t('common.unknownUser') }}</h2>
        <p class="pf-usercard__account">{{ loginUserStore.loginUser.userAccount }}</p>
      </div>
      <a-button @click="openEditModal">{{ t('profile.editProfile') }}</a-button>
    </div>

    <!-- 工具栏：类型筛选 + 搜索 + 视图切换 -->
    <div class="pf-toolbar">
      <div class="pf-filters">
        <button
          class="pf-filter"
          :class="{ 'pf-filter--active': activeType === '' }"
          @click="setType('')"
        >
          {{ t('profile.filters.all') }}
        </button>
        <button
          v-for="option in genTypeOptions"
          :key="option.value"
          class="pf-filter"
          :class="{ 'pf-filter--active': activeType === option.value }"
          @click="setType(option.value)"
        >
          {{ option.label }}
        </button>
      </div>
      <a-input-search
        v-model:value="searchKey"
        class="pf-search"
        :placeholder="t('profile.searchPh')"
        allow-clear
        @search="onSearch"
      />
      <a-segmented
        v-model:value="viewType"
        :options="viewOptions"
      />
    </div>

    <!-- 卡片视图 -->
    <template v-if="viewType === 'cards'">
      <a-empty v-if="!loading && apps.length === 0" :description="t('profile.empty')" class="pf-empty" />
      <div v-else class="pf-grid">
        <article
          v-for="app in apps"
          :key="app.id"
          class="pf-appcard"
          @click="viewChat(app.id)"
        >
          <div class="pf-appcard__cover">
            <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
            <div v-else class="pf-appcard__placeholder">🤖</div>
            <span
              v-if="app.deployKey"
              class="pf-status pf-status--live"
            >
              <i class="pf-status__dot"></i>{{ t('profile.live') }}
            </span>
            <span v-else class="pf-status pf-status--draft">{{ t('profile.draft') }}</span>
          </div>
          <div class="pf-appcard__body">
            <h3 class="pf-appcard__name">{{ app.appName || t('appCard.untitled') }}</h3>
            <div class="pf-appcard__meta">
              <span class="pf-type-tag">{{ formatCodeGenType(app.codeGenType) }}</span>
              <span class="pf-appcard__updated">
                {{ t('profile.updated') }} {{ formatDate(app.updateTime || app.createTime) }}
              </span>
            </div>
          </div>
        </article>
      </div>
    </template>

    <!-- 列表视图 -->
    <div v-else class="pf-table">
      <a-table
        :columns="columns"
        :data-source="apps"
        :loading="loading"
        :pagination="false"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'id'">
            <span class="pf-mono">#{{ record.id }}</span>
          </template>
          <template v-else-if="column.key === 'codeGenType'">
            <span class="pf-type-tag">{{ formatCodeGenType(record.codeGenType) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <span v-if="record.deployKey" class="pf-status pf-status--live">
              <i class="pf-status__dot"></i>{{ t('profile.live') }}
            </span>
            <span v-else class="pf-status pf-status--draft">{{ t('profile.draft') }}</span>
          </template>
          <template v-else-if="column.key === 'updated'">
            <span class="pf-mono">{{ formatDate(record.updateTime || record.createTime) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="editApp(record)">
                {{ t('profile.actions.edit') }}
              </a-button>
              <a-popconfirm
                :title="t('detail.deleteConfirm')"
                :ok-text="t('common.confirm')"
                :cancel-text="t('common.cancel')"
                @confirm="removeApp(record.id)"
              >
                <a-button type="link" danger size="small">{{ t('profile.actions.delete') }}</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 分页 -->
    <div class="pf-pagination">
      <a-pagination
        v-model:current="page.current"
        v-model:page-size="page.pageSize"
        :total="total"
        :show-size-changer="false"
        :show-total="(total: number) => t('profile.total', { total })"
        @change="loadApps"
      />
    </div>

    <!-- 编辑资料弹窗 -->
    <a-modal
      v-model:open="editModalOpen"
      :title="t('profile.editProfile')"
      :ok-text="t('profile.submit')"
      :cancel-text="t('common.cancel')"
      @ok="handleEditSubmit"
    >
      <a-form layout="vertical" :model="editForm">
        <a-form-item :label="t('profile.account')">
          <a-input :value="loginUserStore.loginUser.userAccount" disabled />
        </a-form-item>
        <a-form-item :label="t('profile.userName')">
          <a-input v-model:value="editForm.userName" :placeholder="t('profile.userNamePh')" />
        </a-form-item>
        <a-form-item :label="t('profile.avatar')">
          <a-input v-model:value="editForm.userAvatar" :placeholder="t('profile.avatarPh')" />
        </a-form-item>
        <a-form-item :label="t('profile.bio')">
          <a-textarea
            v-model:value="editForm.userProfile"
            :placeholder="t('profile.bioPh')"
            :auto-size="{ minRows: 3, maxRows: 5 }"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { listMyAppVoByPage, deleteApp } from '@/api/appController'
import { updateMyUser } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'
import { formatCodeGenType, getCodeGenTypeOptions } from '@/utils/codeGenTypes'
import { formatDate } from '@/utils/time'
import BrandLogo from '@/components/BrandLogo.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const { t } = useI18n({ useScope: 'global' })

// 我的应用数据
const apps = ref<API.AppVO[]>([])
const total = ref(0)
const loading = ref(false)
const page = reactive({ current: 1, pageSize: 9 })
const activeType = ref('')
const searchKey = ref('')
const viewType = ref<'cards' | 'list'>('cards')

const genTypeOptions = computed(() => getCodeGenTypeOptions())

const viewOptions = computed(() => [
  { label: t('profile.view.cards'), value: 'cards' },
  { label: t('profile.view.list'), value: 'list' },
])

const columns = computed(() => [
  { title: t('profile.columns.id'), key: 'id', width: 90 },
  { title: t('profile.columns.name'), dataIndex: 'appName', ellipsis: true },
  { title: t('profile.columns.type'), key: 'codeGenType', width: 140 },
  { title: t('profile.columns.status'), key: 'status', width: 100 },
  { title: t('profile.columns.updated'), key: 'updated', width: 130 },
  { title: t('profile.columns.action'), key: 'action', width: 140 },
])

// 加载我的应用
const loadApps = async () => {
  loading.value = true
  try {
    const res = await listMyAppVoByPage({
      pageNum: page.current,
      pageSize: page.pageSize,
      sortField: 'updateTime',
      sortOrder: 'desc',
      codeGenType: activeType.value || undefined,
      appName: searchKey.value.trim() || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      apps.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error(t('profile.fetchFailed'))
    }
  } catch (error) {
    console.error('加载我的应用失败：', error)
    message.error(t('profile.fetchFailed'))
  } finally {
    loading.value = false
  }
}

// 类型筛选
const setType = (value: string) => {
  activeType.value = value
  page.current = 1
  loadApps()
}

// 搜索
const onSearch = () => {
  page.current = 1
  loadApps()
}

// 查看对话
const viewChat = (appId: string | number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}?view=1`)
  }
}

// 编辑应用
const editApp = (app: API.AppVO) => {
  if (app.id) {
    router.push(`/app/edit/${app.id}`)
  }
}

// 删除应用
const removeApp = async (id: number | undefined) => {
  if (!id) return
  try {
    const res = await deleteApp({ id })
    if (res.data.code === 0) {
      message.success(t('detail.deleteSuccess'))
      loadApps()
    } else {
      message.error(t('detail.deleteFailed', { msg: res.data.message }))
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error(t('detail.deleteFailedPlain'))
  }
}

// 编辑资料弹窗
const editModalOpen = ref(false)
const editForm = reactive<API.UserUpdateMyRequest>({
  userName: '',
  userAvatar: '',
  userProfile: '',
})

const openEditModal = () => {
  editForm.userName = loginUserStore.loginUser.userName
  editForm.userAvatar = loginUserStore.loginUser.userAvatar
  editForm.userProfile = loginUserStore.loginUser.userProfile
  editModalOpen.value = true
}

const handleEditSubmit = async () => {
  const res = await updateMyUser({ ...editForm })
  if (res.data.code === 0) {
    await loginUserStore.fetchLoginUser()
    message.success(t('profile.success'))
    editModalOpen.value = false
  } else {
    message.error(t('profile.failedWithReason', { msg: res.data.message }))
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadApps()
})
</script>

<style scoped>
#userProfilePage {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px 48px;
  width: 100%;
  box-sizing: border-box;
}

.pf-title {
  margin: 0;
  font-size: var(--text-h1);
  line-height: var(--leading-h1);
  font-weight: var(--weight-heavy);
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.pf-desc {
  margin: 8px 0 0;
  font-size: var(--text-caption);
  line-height: var(--leading-caption);
  color: var(--text-muted);
}

/* 用户信息卡 */
.pf-usercard {
  margin-top: 24px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-xs);
}

.pf-usercard__info {
  flex: 1;
  min-width: 0;
}

.pf-usercard__name {
  margin: 0;
  font-size: var(--text-h2);
  line-height: var(--leading-h2);
  font-weight: var(--weight-strong);
  color: var(--text-primary);
}

.pf-usercard__account {
  margin: 4px 0 0;
  font-family: var(--font-mono);
  font-size: var(--text-caption);
  color: var(--text-secondary);
}

/* 工具栏 */
.pf-toolbar {
  margin-top: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.pf-filters {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pf-filter {
  padding: 6px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  background: var(--bg-surface);
  color: var(--text-secondary);
  font-size: var(--text-caption);
  cursor: pointer;
  transition: all var(--dur-fast) var(--ease);
}

.pf-filter:hover {
  color: var(--brand-text);
  border-color: var(--brand);
}

.pf-filter--active {
  background: var(--brand-soft);
  border-color: var(--brand);
  color: var(--brand-text);
  font-weight: var(--weight-medium);
}

.pf-search {
  width: 100%;
  max-width: 320px;
}

/* 卡片网格 */
.pf-grid {
  margin-top: 24px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 24px;
}

.pf-appcard {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow var(--dur) var(--ease), transform var(--dur) var(--ease);
}

.pf-appcard:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-4px);
}

.pf-appcard__cover {
  height: 150px;
  background: var(--bg-surface-muted);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.pf-appcard__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pf-appcard__placeholder {
  font-size: 40px;
  color: var(--text-muted);
}

.pf-status {
  position: absolute;
  top: 10px;
  right: 10px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  font-size: var(--text-micro);
}

.pf-status--live {
  background: var(--brand-soft);
  color: var(--brand-text);
}

.pf-status__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--brand);
}

.pf-status--draft {
  background: var(--bg-elevated);
  color: var(--text-muted);
  border: 1px solid var(--border);
}

.pf-appcard__body {
  padding: 14px 16px 16px;
}

.pf-appcard__name {
  margin: 0;
  font-size: var(--text-title);
  font-weight: var(--weight-strong);
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pf-appcard__meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: var(--text-micro);
  color: var(--text-muted);
}

.pf-type-tag {
  padding: 1px 8px;
  border-radius: var(--radius-xs);
  background: var(--bg-surface-muted);
  color: var(--text-secondary);
  font-family: var(--font-mono);
  font-size: var(--text-micro);
}

/* 列表视图 */
.pf-table {
  margin-top: 24px;
}

.pf-mono {
  font-family: var(--font-mono);
  font-size: var(--text-caption);
}

/* 分页 */
.pf-pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.pf-empty {
  margin-top: 48px;
}
</style>
