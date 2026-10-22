<template>
  <a-layout-header class="header">
    <a-row :wrap="false">
      <!-- 左侧：Logo和标题 -->
      <a-col flex="200px">
        <RouterLink to="/" aria-label="CodeNow">
          <BrandLogo />
        </RouterLink>
      </a-col>
      <!-- 中间：导航菜单 -->
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          :items="menuItems"
          @click="handleMenuClick"
        />
      </a-col>
      <!-- 右侧：用户操作区域 -->
      <a-col>
        <div class="header-actions">
          <HeaderPreferences />
          <div class="user-login-status">
            <div v-if="loginUserStore.loginUser.id">
              <a-dropdown>
                <a-space>
                  <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                  {{ loginUserStore.loginUser.userName ?? t('header.guestName') }}
                </a-space>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="router.push('/user/profile')">
                      <UserOutlined/>
                      {{ t('header.userMenu.profile') }}
                    </a-menu-item>
                    <a-menu-item @click="doLogout">
                      <LogoutOutlined />
                      {{ t('header.userMenu.logout') }}
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </div>
            <div v-else>
              <a-button type="primary" href="/user/login">{{ t('header.login') }}</a-button>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, h, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { type MenuProps, message } from 'ant-design-vue'
import { LogoutOutlined, UserOutlined, HomeOutlined } from '@ant-design/icons-vue'
import HeaderPreferences from '@/components/HeaderPreferences.vue'
import BrandLogo from '@/components/BrandLogo.vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { userLogout } from '@/api/userController'
import checkAccess from '@/access/checkAccess'
import ACCESS_ENUM from '@/access/accessEnum'
import appRouter from '@/router'

const route = useRoute()
const loginUserStore = useLoginUserStore()
const router = useRouter()
const { t } = useI18n({ useScope: 'global' })
const selectedKeys = ref<string[]>([route.path])

const originItems = computed<MenuProps['items']>(() => [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: t('header.nav.home'),
    title: t('header.nav.home'),
  },
  {
    key: '/user/profile',
    label: t('header.nav.profile'),
    title: t('header.nav.profile'),
  },
  {
    key: '/admin/userManage',
    label: t('header.nav.userManage'),
    title: t('header.nav.userManage'),
  },
  {
    key: '/admin/appManage',
    label: t('header.nav.appManage'),
    title: t('header.nav.appManage'),
  },
  {
    key: 'others',
    label: h('a', { href: 'https://github.com/iceblyte', target: '_blank' }, 'GitHub'),
    title: 'GitHub',
  },
])

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
  return originItems.value?.filter((menu) => {
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

// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  // 跳转到对应页面
  if (key.startsWith('/')) {
    router.push(key)
  }
}

// 退出登录
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
      userRole: ACCESS_ENUM.NOT_LOGIN,
    })
    message.success(t('header.logoutSuccess'))
    await router.push('/user/login')
  } else {
    message.error(t('header.logoutFailed', { msg: res.data.message }))
  }
}
</script>

<style scoped>
.header {
  background: var(--bg-surface);
  background: color-mix(in srgb, var(--bg-surface) 82%, transparent);
  backdrop-filter: saturate(180%) blur(20px);
  padding: 0 24px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-login-status {
  min-width: 120px;
}

.ant-menu-horizontal {
  border-bottom: none !important;
}
</style>
