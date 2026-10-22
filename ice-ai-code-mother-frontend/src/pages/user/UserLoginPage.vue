<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-card__body">
        <RouterLink to="/" class="auth-logo" aria-label="CodeNow">
          <BrandLogo />
        </RouterLink>

        <h1 class="auth-title">{{ t('auth.login.title') }}</h1>
        <p class="auth-subtitle">{{ t('auth.login.subtitle') }}</p>

        <a-form
          ref="formRef"
          :model="formState"
          autocomplete="off"
          layout="vertical"
          class="auth-form"
          @finish="handleSubmit"
        >
          <a-form-item
            :label="t('auth.login.accountLabel')"
            name="userAccount"
            :rules="[{ required: true, message: t('auth.login.accountRequired') }]"
          >
            <a-input
              v-model:value="formState.userAccount"
              :placeholder="t('auth.login.accountPh')"
              size="large"
            />
          </a-form-item>
          <a-form-item
            :label="t('auth.login.passwordLabel')"
            name="userPassword"
            :rules="[
              { required: true, message: t('auth.login.passwordRequired') },
              { min: 8, message: t('auth.login.passwordMin') },
            ]"
          >
            <a-input-password
              v-model:value="formState.userPassword"
              :placeholder="t('auth.login.passwordPh')"
              size="large"
            />
          </a-form-item>
          <a-form-item class="auth-submit">
            <a-button type="primary" html-type="submit" block size="large">
              {{ t('auth.login.submit') }}
            </a-button>
          </a-form-item>
        </a-form>

        <p class="auth-note">
          <span>{{ t('auth.login.noAccount') }}</span>
          <RouterLink to="/user/register">{{ t('auth.login.toRegister') }}</RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { userLogin } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useRoute, useRouter } from 'vue-router'
import { message, type FormInstance } from 'ant-design-vue'
import BrandLogo from '@/components/BrandLogo.vue'

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const { t, locale } = useI18n({ useScope: 'global' })
const formRef = ref<FormInstance>()

// 切换语言时清除旧语言的校验提示，下次校验按新语言展示
watch(locale, () => formRef.value?.clearValidate())

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  const res = await userLogin(values)
  // 登录成功，把登录态保存到全局状态中
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success(t('auth.login.success'))
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push({
      path: redirect,
      replace: true,
    })
  } else {
    message.error(t('auth.login.failedWithReason', { msg: res.data.message }))
  }
}
</script>

<style scoped>
/* 布局对齐原型 login.html：居中 420px 认证卡片 */
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
