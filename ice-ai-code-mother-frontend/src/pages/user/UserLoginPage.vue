<template>
  <div id="userLoginPage">
    <h2 class="title">{{ t('auth.login.title') }}</h2>
    <div class="desc">{{ t('auth.login.subtitle') }}</div>
    <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
      <a-form-item name="userAccount" :rules="[{ required: true, message: t('auth.login.accountRequired') }]">
        <a-input v-model:value="formState.userAccount" :placeholder="t('auth.login.accountPh')" />
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: t('auth.login.passwordRequired') },
          { min: 8, message: t('auth.login.passwordMin') },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" :placeholder="t('auth.login.passwordPh')" />
      </a-form-item>
      <div class="tips">
        {{ t('auth.login.noAccount') }}
        <RouterLink to="/user/register">{{ t('auth.login.toRegister') }}</RouterLink>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">
          {{ t('auth.login.submit') }}
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<script lang="ts" setup>
import { reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { userLogin } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const { t } = useI18n({ useScope: 'global' })

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
#userLoginPage {
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
  text-align: right;
  color: var(--text-muted);
  font-size: 13px;
  margin-bottom: 16px;
}
</style>
