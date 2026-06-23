import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getLoginUser } from '@/api/userController.ts'
import ACCESS_ENUM from '@/access/accessEnum'

export const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<API.LoginUserVO>({
    userName: '未登录',
    userRole: ACCESS_ENUM.NOT_LOGIN,
  })

  async function fetchLoginUser() {
    try {
      const res = await getLoginUser()
      if (res.data.code === 0 && res.data.data) {
        loginUser.value = res.data.data
        return
      }
    } catch (_error) {
      // ignore
    }
    loginUser.value = {
      userName: '未登录',
      userRole: ACCESS_ENUM.NOT_LOGIN,
    }
  }

  function setLoginUser(newLoginUser: API.LoginUserVO) {
    loginUser.value = newLoginUser
  }

  return { loginUser, setLoginUser, fetchLoginUser }
})
