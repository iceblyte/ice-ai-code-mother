<template>
  <div id="userRegisterPage">
    <h2 class="title">{{ t('auth.register.title') }}</h2>
    <div class="desc">{{ t('auth.register.subtitle') }}</div>
    <a-form ref="formRef" :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
      <a-form-item name="userAccount" :rules="[{ required: true, message: t('auth.register.accountRequired') }]">
        <a-input v-model:value="formState.userAccount" :placeholder="t('auth.register.accountPh')" />
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: t('auth.register.passwordRequired') },
          { min: 8, message: t('auth.register.passwordMin') },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" :placeholder="t('auth.register.passwordPh')" />
      </a-form-item>
      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: t('auth.register.confirmRequired') },
          { min: 8, message: t('auth.register.passwordMin') },
          { validator: validateCheckPassword },
        ]"
      >
        <a-input-password v-model:value="formState.checkPassword" :placeholder="t('auth.register.confirmPh')" />
      </a-form-item>
      <div class="tips">
        {{ t('auth.register.hasAccount') }}
        <RouterLink to="/user/login">{{ t('auth.register.toLogin') }}</RouterLink>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">
          {{ t('auth.register.submit') }}
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { userRegister } from '@/api/userController.ts'
import { message, type FormInstance } from 'ant-design-vue'
import { reactive, ref, watch } from 'vue'

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
#userRegisterPage {
  background: var(--bg-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  max-width: 720px;
  padding: 24px;
  margin: 24px auto;
}

.title {
  text-align: center;
  margin-bottom: 16px;
}

.desc {
  text-align: center;
  color: var(--text-muted);
  margin-bottom: 16px;
}

.tips {
  margin-bottom: 16px;
  color: var(--text-muted);
  font-size: 13px;
  text-align: right;
}
</style>
