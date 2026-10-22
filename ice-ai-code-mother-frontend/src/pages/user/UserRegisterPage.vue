<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-card__body">
        <RouterLink to="/" class="auth-logo" aria-label="CodeNow">
          <BrandLogo />
        </RouterLink>

        <h1 class="auth-title">{{ t('auth.register.title') }}</h1>
        <p class="auth-subtitle">{{ t('auth.register.subtitle') }}</p>

        <a-form
          ref="formRef"
          :model="formState"
          autocomplete="off"
          layout="vertical"
          class="auth-form"
          @finish="handleSubmit"
        >
          <a-form-item
            :label="t('auth.register.accountLabel')"
            name="userAccount"
            :rules="[{ required: true, message: t('auth.register.accountRequired') }]"
          >
            <a-input
              v-model:value="formState.userAccount"
              :placeholder="t('auth.register.accountPh')"
              size="large"
            />
          </a-form-item>
          <a-form-item
            :label="t('auth.register.passwordLabel')"
            name="userPassword"
            :rules="[
              { required: true, message: t('auth.register.passwordRequired') },
              { min: 8, message: t('auth.register.passwordMin') },
            ]"
          >
            <a-input-password
              v-model:value="formState.userPassword"
              :placeholder="t('auth.register.passwordPh')"
              size="large"
            />
          </a-form-item>
          <a-form-item
            :label="t('auth.register.confirmLabel')"
            name="checkPassword"
            :rules="[
              { required: true, message: t('auth.register.confirmRequired') },
              { min: 8, message: t('auth.register.passwordMin') },
              { validator: validateCheckPassword },
            ]"
          >
            <a-input-password
              v-model:value="formState.checkPassword"
              :placeholder="t('auth.register.confirmPh')"
              size="large"
            />
          </a-form-item>
          <a-form-item class="auth-submit">
            <a-button type="primary" html-type="submit" block size="large">
              {{ t('auth.register.submit') }}
            </a-button>
          </a-form-item>
        </a-form>

        <p class="auth-note">
          <span>{{ t('auth.register.haveAccount') }}</span>
          <RouterLink to="/user/login">{{ t('auth.register.toLogin') }}</RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { userRegister } from '@/api/userController.ts'
import { message, type FormInstance } from 'ant-design-vue'
import { reactive, ref, watch } from 'vue'
import BrandLogo from '@/components/BrandLogo.vue'

const router = useRouter()
const { t, locale } = useI18n({ useScope: 'global' })
const formRef = ref<FormInstance>()

// 切换语言时清除旧语言的校验提示，下次校验按新语言展示
watch(locale, () => formRef.value?.clearValidate())

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

/**
 * 验证确认密码
 * @param rule
 * @param value
 * @param callback
 */
const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) {
    callback(new Error(t('auth.register.passwordMismatch')))
  } else {
    callback()
  }
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: API.UserRegisterRequest) => {
  const res = await userRegister(values)
  // 注册成功，跳转到登录页面
  if (res.data.code === 0) {
    message.success(t('auth.register.success'))
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error(t('auth.register.failedWithReason', { msg: res.data.message }))
  }
}
</script>

<style scoped>
/* 布局对齐原型 register.html：居中 420px 认证卡片（与登录页同构） */
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  min-height: calc(100vh - var(--header-h, 64px));
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-sm);
}

.auth-card__body {
  padding: 40px;
}

.auth-logo {
  display: inline-flex;
}

.auth-title {
  margin: 24px 0 0;
  font-size: var(--text-h1);
  line-height: var(--leading-h1);
  font-weight: var(--weight-heavy);
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.auth-subtitle {
  margin: 16px 0 0;
  font-size: var(--text-caption);
  line-height: var(--leading-caption);
  color: var(--text-muted);
}

.auth-form {
  margin-top: 24px;
}

.auth-form :deep(.ant-form-item-label > label) {
  color: var(--text-secondary);
  font-size: var(--text-caption);
}

.auth-submit {
  margin-bottom: 0;
}

.auth-note {
  margin: 24px 0 0;
  text-align: center;
  font-size: var(--text-caption);
  color: var(--text-muted);
}

.auth-note a {
  color: var(--brand-text);
  font-weight: var(--weight-medium);
  margin-left: 4px;
}

.auth-note a:hover {
  text-decoration: underline;
}
</style>
