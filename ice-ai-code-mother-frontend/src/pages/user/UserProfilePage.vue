<template>
  <div id="userProfilePage">
    <a-card title="个人中心">
      <a-form layout="vertical" :model="formState" @finish="handleSubmit">
        <a-form-item label="账号">
          <a-input :value="loginUserStore.loginUser.userAccount" disabled />
        </a-form-item>
        <a-form-item label="用户名">
          <a-input v-model:value="formState.userName" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="头像地址">
          <a-input v-model:value="formState.userAvatar" placeholder="请输入头像地址" />
        </a-form-item>
        <a-form-item label="个人简介">
          <a-textarea
            v-model:value="formState.userProfile"
            placeholder="介绍一下自己"
            :auto-size="{ minRows: 4, maxRows: 6 }"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">保存资料</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import { message } from 'ant-design-vue'
import { updateMyUser } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'

const loginUserStore = useLoginUserStore()

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
    message.success('保存成功')
  } else {
    message.error('保存失败，' + res.data.message)
  }
}
</script>

<style scoped>
#userProfilePage {
  width: 600px;
  margin: 0 auto;
}
</style>
