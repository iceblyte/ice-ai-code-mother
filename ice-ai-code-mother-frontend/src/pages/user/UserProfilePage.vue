<template>
  <div id="userProfilePage">
    <a-card :title="t('profile.title')">
      <a-form layout="vertical" :model="formState" @finish="handleSubmit">
        <a-form-item :label="t('profile.account')">
          <a-input :value="loginUserStore.loginUser.userAccount" disabled />
        </a-form-item>
        <a-form-item :label="t('profile.userName')">
          <a-input v-model:value="formState.userName" :placeholder="t('profile.userNamePh')" />
        </a-form-item>
        <a-form-item :label="t('profile.avatar')">
          <a-input v-model:value="formState.userAvatar" :placeholder="t('profile.avatarPh')" />
        </a-form-item>
        <a-form-item :label="t('profile.bio')">
          <a-textarea
            v-model:value="formState.userProfile"
            :placeholder="t('profile.bioPh')"
            :auto-size="{ minRows: 4, maxRows: 6 }"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">{{ t('profile.submit') }}</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { updateMyUser } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'

const loginUserStore = useLoginUserStore()
const { t } = useI18n({ useScope: 'global' })

const formState = reactive<API.UserUpdateMyRequest>({
  userName: '',
  userAvatar: '',
  userProfile: '',
})

watch(
  () => loginUserStore.loginUser,
  (loginUser) => {
    formState.userName = loginUser.userName
    formState.userAvatar = loginUser.userAvatar
    formState.userProfile = loginUser.userProfile
  },
  {
    immediate: true,
    deep: true,
  },
)

const handleSubmit = async () => {
  const res = await updateMyUser({
    ...formState,
  })
  if (res.data.code === 0) {
    await loginUserStore.fetchLoginUser()
    message.success(t('profile.success'))
  } else {
    message.error(t('profile.failedWithReason', { msg: res.data.message }))
  }
}
</script>

<style scoped>
#userProfilePage {
  width: 600px;
  margin: 0 auto;
}
</style>
