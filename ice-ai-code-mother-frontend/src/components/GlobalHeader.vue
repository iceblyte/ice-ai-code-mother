<template>
  <a-layout-header class="header">
    <div class="header-inner">
      <RouterLink to="/" class="header-logo" aria-label="CodeNow">
        <BrandLogo :mark-width="30" :word-height="22" />
      </RouterLink>

      <!-- 桌面导航 -->
      <nav class="header-nav">
        <template v-for="item in visibleItems" :key="item.key">
          <RouterLink
            v-if="item.to"
            :to="item.to"
            :aria-current="item.current ? 'page' : undefined"
          >
            {{ item.label }}
          </RouterLink>
          <a v-else :href="item.href" target="_blank" rel="noopener noreferrer">
            {{ item.label }}
          </a>
        </template>
      </nav>

      <span class="header-grow"></span>

      <!-- 偏好控件 + 用户区 -->
      <div class="header-controls">
        <HeaderPreferences />
        <div v-if="loginUserStore.loginUser.id" class="header-user">
          <a-dropdown placement="bottomRight">
            <button class="header-user__summary" type="button">
              <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="28">
                {{ loginUserStore.loginUser.userName?.charAt(0) || 'U' }}
              </a-avatar>
              <span class="header-user__name">
                {{ loginUserStore.loginUser.userName ?? t('header.guestName') }}
              </span>
            </button>
            <template #overlay>
              <a-menu @click="onUserMenuClick">
                <a-menu-item key="profile">{{ t('header.userMenu.profile') }}</a-menu-item>
                <a-menu-item key="logout">{{ t('header.userMenu.logout') }}</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <RouterLink v-else to="/user/login" class="header-login">
          {{ t('header.login') }}
        </RouterLink>

        <!-- 移动端汉堡 -->
        <button
          class="header-burger"
          type="button"
          :aria-label="t('header.menu')"
          :aria-expanded="mobileOpen"
          @click="mobileOpen = !mobileOpen"
        >
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
            aria-hidden="true"
          >
            <path d="M4 7h16M4 12h16M4 17h16" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 移动端导航 -->
    <div v-if="mobileOpen" class="header-mobilenav">
      <nav class="header-nav header-nav--mobile">
        <template v-for="item in visibleItems" :key="item.key">
          <RouterLink v-if="item.to" :to="item.to" @click="mobileOpen = false">
            {{ item.label }}
          </RouterLink>
          <a v-else :href="item.href" target="_blank" rel="noopener noreferrer">
            {{ item.label }}
          </a>
        </template>
      </nav>
      <div class="header-mobilenav__account">
        <template v-if="loginUserStore.loginUser.id">
          <RouterLink to="/user/profile" @click="mobileOpen = false">
            {{ t('header.userMenu.profile') }}
          </RouterLink>
          <a @click.prevent="doLogout">{{ t('header.userMenu.logout') }}</a>
        </template>
        <RouterLink v-else to="/user/login" @click="mobileOpen = false">
          {{ t('header.login') }}
        </RouterLink>
      </div>
    </div>
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
const mobileOpen = ref(false)

interface NavItem {
  key: string
  label: string
  to?: string
  href?: string
  current?: boolean
}

// 导航项（与原有权限过滤逻辑一致）
const originItems = computed<NavItem[]>(() => {
  const routeMap: Record<string, { hideInMenu?: boolean; meta?: { access?: string } }> =
    Object.fromEntries(appRouter.getRoutes().map((item) => [item.path, item] as const))

  const canShow = (path: string) => {
    const routeItem = routeMap[path]
    if (!routeItem || routeItem.hideInMenu) return false
    const needAccess = (routeItem.meta?.access as string) ?? ACCESS_ENUM.NOT_LOGIN
    return checkAccess(loginUserStore.loginUser, needAccess)
  }

  const items: NavItem[] = [
    { key: '/', label: t('header.nav.home'), to: '/', current: route.path === '/' },
  ]
  if (canShow('/user/profile')) {
    items.push({
      key: '/user/profile',
      label: t('header.nav.profile'),
      to: '/user/profile',
      current: route.path === '/user/profile',
    })
  }
  if (canShow('/admin/userManage')) {
    items.push({
      key: '/admin/userManage',
      label: t('header.nav.userManage'),
      to: '/admin/userManage',
      current: route.path === '/admin/userManage',
    })
  }
  if (canShow('/admin/appManage')) {
    items.push({
      key: '/admin/appManage',
      label: t('header.nav.appManage'),
      to: '/admin/appManage',
      current: route.path === '/admin/appManage',
    })
  }
  items.push({
    key: 'github',
    label: 'GitHub',
    href: 'https://github.com/iceblyte',
  })
  return items
})

const visibleItems = originItems

watch(
  () => route.path,
  (newPath) => {
    selectedKeys.value = [newPath]
    mobileOpen.value = false
  },
  {
    immediate: true,
  },
)

const onUserMenuClick: MenuProps['onClick'] = ({ key }) => {
  if (key === 'profile') {
    router.push('/user/profile')
  } else if (key === 'logout') {
    doLogout()
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
/* 对齐原型 chrome.js：sticky 顶栏 + 毛玻璃 + 文字导航 + 图标控件 */
.header {
  position: sticky;
  top: 0;
  z-index: 50;
  height: var(--header-h, 64px);
  line-height: normal;
  background: var(--bg-surface);
  background: color-mix(in srgb, var(--bg-surface) 82%, transparent);
  backdrop-filter: saturate(180%) blur(20px);
  border-bottom: 1px solid var(--border);
  padding: 0 24px;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: var(--header-h, 64px);
  display: flex;
  align-items: center;
  gap: 24px;
}

.header-logo {
  display: inline-flex;
  flex: none;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-nav a {
  padding: 10px 14px;
  border-radius: var(--radius);
  font-size: var(--text-title);
  color: var(--text-secondary);
  transition:
    background var(--dur-fast) var(--ease),
    color var(--dur-fast) var(--ease);
}

.header-nav a:hover {
  background: var(--bg-surface-muted);
  color: var(--text-primary);
}

.header-nav a[aria-current='page'] {
  color: var(--text-primary);
  font-weight: var(--weight-medium);
  background: var(--bg-surface-muted);
}

.header-grow {
  flex: 1 1 auto;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: none;
}

/* 用户区（对齐 .cn-user__summary 胶囊） */
.header-user__summary {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 12px 0 5px;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  cursor: pointer;
  color: var(--text-secondary);
  background: transparent;
  transition: all var(--dur-fast) var(--ease);
}

.header-user__summary:hover {
  border-color: var(--border-strong);
  color: var(--text-primary);
}

.header-user__name {
  font-size: var(--text-body);
  font-weight: var(--weight-medium);
  max-width: 120px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.header-login {
  display: inline-flex;
  align-items: center;
  height: 38px;
  padding: 0 18px;
  border: 1px solid transparent;
  border-radius: var(--radius-full);
  background: var(--brand);
  color: var(--brand-on);
  font-size: var(--text-body);
  font-weight: var(--weight-medium);
  transition: background var(--dur-fast) var(--ease);
}

.header-login:hover {
  background: var(--brand-hover);
}

/* 移动端汉堡 + 折叠导航 */
.header-burger {
  display: none;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  cursor: pointer;
}

.header-burger:hover {
  color: var(--text-primary);
  border-color: var(--border-strong);
  background: var(--bg-surface-muted);
}

.header-mobilenav {
  border-top: 1px solid var(--border);
  background: var(--bg-surface);
}

.header-nav--mobile {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 8px 0;
  gap: 0;
}

.header-nav--mobile a {
  padding: 12px;
  border-radius: 0;
}

.header-mobilenav__account {
  display: flex;
  gap: 16px;
  padding: 12px 12px 16px;
  border-top: 1px solid var(--border);
  font-size: var(--text-caption);
  color: var(--text-secondary);
}

.header-mobilenav__account a {
  cursor: pointer;
}

.header-mobilenav__account a:hover {
  color: var(--text-primary);
}

@media (max-width: 860px) {
  .header-nav:not(.header-nav--mobile) {
    display: none;
  }

  .header-user__name {
    display: none;
  }

  .header-burger {
    display: inline-flex;
  }
}
</style>
