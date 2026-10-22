<template>
  <div id="appEditPage">
    <div class="page-header">
      <h1>{{ t('edit.title') }}</h1>
    </div>

    <div class="edit-container">
      <a-card :title="t('edit.basic')" :loading="loading">
        <a-form
          :model="formData"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit"
          ref="formRef"
        >
          <a-form-item :label="t('edit.appName')" name="appName">
            <a-input
              v-model:value="formData.appName"
              :placeholder="t('edit.appNamePh')"
              :maxlength="50"
              show-count
            />
          </a-form-item>

          <a-form-item
            v-if="isAdmin"
            :label="t('edit.cover')"
            name="cover"
            :extra="t('edit.coverExtra')"
          >
            <a-input v-model:value="formData.cover" :placeholder="t('edit.coverPh')" />
            <div v-if="formData.cover" class="cover-preview">
              <a-image
                :src="formData.cover"
                :width="200"
                :height="150"
                fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
              />
            </div>
          </a-form-item>

          <a-form-item
            v-if="isAdmin"
            :label="t('edit.priority')"
            name="priority"
            :extra="t('edit.priorityExtra')"
          >
            <a-input-number
              v-model:value="formData.priority"
              :min="0"
              :max="99"
              style="width: 200px"
            />
          </a-form-item>

          <a-form-item :label="t('edit.initPrompt')" name="initPrompt">
            <a-textarea
              v-model:value="formData.initPrompt"
              :placeholder="t('edit.initPromptPh')"
              :rows="4"
              :maxlength="1000"
              show-count
              disabled
            />
            <div class="form-tip">{{ t('edit.initPromptTip') }}</div>
          </a-form-item>

          <a-form-item :label="t('edit.genType')" name="codeGenType">
            <a-input
              :value="formatCodeGenType(formData.codeGenType)"
              :placeholder="t('edit.genType')"
              disabled
            />
            <div class="form-tip">{{ t('edit.genTypeTip') }}</div>
          </a-form-item>

          <a-form-item v-if="formData.deployKey" :label="t('edit.deployKey')" name="deployKey">
            <a-input v-model:value="formData.deployKey" :placeholder="t('edit.deployKey')" disabled />
            <div class="form-tip">{{ t('edit.deployKeyTip') }}</div>
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit" :loading="submitting">
                {{ t('edit.submit') }}
              </a-button>
              <a-button @click="resetForm">{{ t('edit.reset') }}</a-button>
              <a-button type="link" @click="goToChat">{{ t('edit.goToChat') }}</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-card>

      <!-- 应用信息展示 -->
      <a-card :title="t('edit.info')" style="margin-top: 24px">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item :label="t('edit.appId')">
            {{ appInfo?.id }}
          </a-descriptions-item>
          <a-descriptions-item :label="t('edit.creator')">
            <UserInfo :user="appInfo?.user" size="small" />
          </a-descriptions-item>
          <a-descriptions-item :label="t('edit.createTime')">
            {{ formatTime(appInfo?.createTime) }}
          </a-descriptions-item>
          <a-descriptions-item :label="t('edit.updateTime')">
            {{ formatTime(appInfo?.updateTime) }}
          </a-descriptions-item>
          <a-descriptions-item :label="t('edit.deployTime')">
            {{ appInfo?.deployedTime ? formatTime(appInfo.deployedTime) : t('edit.notDeployed') }}
          </a-descriptions-item>
          <a-descriptions-item :label="t('edit.deployLink')">
            <a-button v-if="appInfo?.deployKey" type="link" @click="openPreview" size="small">
              {{ t('edit.viewPreview') }}
            </a-button>
            <span v-else>{{ t('edit.notDeployed') }}</span>
          </a-descriptions-item>
        </a-descriptions>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { getAppVoById, updateApp, updateAppByAdmin } from '@/api/appController'
import { formatCodeGenType } from '@/utils/codeGenTypes'
import { formatTime } from '@/utils/time'
import UserInfo from '@/components/UserInfo.vue'
import { getStaticPreviewUrl } from '@/config/env'
import type { FormInstance } from 'ant-design-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const { t } = useI18n({ useScope: 'global' })

// 应用信息
const appInfo = ref<API.AppVO>()
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

// 表单数据
const formData = reactive({
  appName: '',
  cover: '',
  priority: 0,
  initPrompt: '',
  codeGenType: '',
  deployKey: '',
})

// 是否为管理员
const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// 表单验证规则（computed 随语言响应）
const rules = computed(() => ({
  appName: [
    { required: true, message: t('edit.appNameRequired'), trigger: 'blur' },
    { min: 1, max: 50, message: t('edit.appNameLength'), trigger: 'blur' },
  ],
  cover: [{ type: 'url' as const, message: t('edit.coverUrlInvalid'), trigger: 'blur' }],
  priority: [
    { type: 'number' as const, min: 0, max: 99, message: t('edit.priorityRange'), trigger: 'blur' },
  ],
}))

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error(t('edit.noAppId'))
    router.push('/')
    return
  }

  loading.value = true
  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 检查权限
      if (!isAdmin.value && appInfo.value.userId !== loginUserStore.loginUser.id) {
        message.error(t('edit.noPermission'))
        router.push('/')
        return
      }

      // 填充表单数据
      formData.appName = appInfo.value.appName || ''
      formData.cover = appInfo.value.cover || ''
      formData.priority = appInfo.value.priority || 0
      formData.initPrompt = appInfo.value.initPrompt || ''
      formData.codeGenType = appInfo.value.codeGenType || ''
      formData.deployKey = appInfo.value.deployKey || ''
    } else {
      message.error(t('edit.fetchFailed'))
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error(t('edit.fetchFailed'))
    router.push('/')
  } finally {
    loading.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!appInfo.value?.id) return

  submitting.value = true
  try {
    let res
    if (isAdmin.value) {
      // 管理员可以修改更多字段
      res = await updateAppByAdmin({
        id: appInfo.value.id,
        appName: formData.appName,
        cover: formData.cover,
        priority: formData.priority,
      })
    } else {
      // 普通用户只能修改应用名称
      res = await updateApp({
        id: appInfo.value.id,
        appName: formData.appName,
      })
    }

    if (res.data.code === 0) {
      message.success(t('edit.updateSuccess'))
      // 重新获取应用信息
      await fetchAppInfo()
    } else {
      message.error(t('edit.updateFailed', { msg: res.data.message }))
    }
  } catch (error) {
    console.error('修改失败：', error)
    message.error(t('edit.updateFailedPlain'))
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  if (appInfo.value) {
    formData.appName = appInfo.value.appName || ''
    formData.cover = appInfo.value.cover || ''
    formData.priority = appInfo.value.priority || 0
  }
  formRef.value?.clearValidate()
}

// 进入对话页面
const goToChat = () => {
  if (appInfo.value?.id) {
    router.push(`/app/chat/${appInfo.value.id}`)
  }
}

// 打开预览
const openPreview = () => {
  if (appInfo.value?.codeGenType && appInfo.value?.id) {
    const url = getStaticPreviewUrl(appInfo.value.codeGenType, String(appInfo.value.id))
    window.open(url, '_blank')
  }
}

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()
})
</script>

<style scoped>
#appEditPage {
  padding: 24px;
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.edit-container {
  border-radius: 8px;
}

.cover-preview {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg-surface-muted);
}

.form-tip {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

:deep(.ant-card-head) {
  background: var(--bg-surface-muted);
}

:deep(.ant-descriptions-item-label) {
  background: var(--bg-surface-muted);
  font-weight: 500;
}
</style>
