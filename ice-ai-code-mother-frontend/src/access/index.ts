import router from '@/router'
import ACCESS_ENUM from '@/access/accessEnum'
import checkAccess from '@/access/checkAccess'
import { useLoginUserStore } from '@/stores/loginUser'

router.beforeEach(async (to) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser

  if (!loginUser.userRole) {
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
  }

  const needAccess = (to.meta?.access as string) ?? ACCESS_ENUM.NOT_LOGIN
  if (needAccess === ACCESS_ENUM.NOT_LOGIN) {
    return true
  }
  if (!loginUser.userRole || loginUser.userRole === ACCESS_ENUM.NOT_LOGIN) {
    return `/user/login?redirect=${encodeURIComponent(to.fullPath)}`
  }
  if (!checkAccess(loginUser, needAccess)) {
    return '/noAuth'
  }
  return true
})
