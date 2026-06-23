<template>
  <a-layout-header class="header">
    <a-row :wrap="false">
      <a-col flex="200px">
        <RouterLink to="/">
          <div class="header-left">
            <img class="logo" src="@/assets/logo.png" alt="Logo" />
            <h1 class="site-title">AI 应用生成</h1>
          </div>
        </RouterLink>
      </a-col>
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          :items="menuItems"
          @click="handleMenuClick"
        />
      </a-col>
      <a-col>
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="router.push('/user/profile')">
                    <UserOutlined/>
                    个人中心
                  </a-menu-item>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, h, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { type MenuProps, message } from 'ant-design-vue'
import { LogoutOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { userLogout } from '@/api/userController'
import checkAccess from '@/access/checkAccess'
import ACCESS_ENUM from '@/access/accessEnum'
import appRouter from '@/router'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()
const selectedKeys = ref<string[]>([route.path])

const originItems: MenuProps['items'] = [
  {
    key: '/',
    label: '主页',
    title: '主页',
  },
  {
    key: '/user/profile',
    label: '个人中心',
    title: '个人中心',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: 'others',
    label: h('a', { href: 'https://github.com/iceblyte', target: '_blank' }, 'GitHub'),
    title: 'GitHub',
  },
]

watch(
  () => route.path,
  (newPath) => {
    selectedKeys.value = [newPath]
  },
  {
    immediate: true,
  },
)

const routeMap = computed(() => {
  const entries = appRouter.getRoutes().map((item) => [item.path, item] as const)
  return Object.fromEntries(entries)
})

const menuItems = computed<MenuProps['items']>(() => {
  return originItems?.filter((menu) => {
    const menuKey = menu?.key as string
    if (!menuKey || !menuKey.startsWith('/')) {
      return true
    }
    const routeItem = routeMap.value[menuKey]
    if (!routeItem) {
      return true
    }
    if (routeItem.meta?.hideInMenu) {
      return false
    }
    const needAccess = (routeItem.meta?.access as string) ?? ACCESS_ENUM.NOT_LOGIN
    return checkAccess(loginUserStore.loginUser, needAccess)
  })
})

const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  if (key.startsWith('/')) {
    router.push(key)
  }
}

const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
      userRole: ACCESS_ENUM.NOT_LOGIN,
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.header {
  background: #fff;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  height: 48px;
  width: 48px;
}

.site-title {
  margin: 0;
  font-size: 18px;
  color: #1890ff;
}

.user-login-status {
  min-width: 120px;
}

.ant-menu-horizontal {
  border-bottom: none !important;
}
</style>
