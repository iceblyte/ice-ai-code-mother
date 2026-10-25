<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import BrandLogo from '@/components/BrandLogo.vue'
import TypewriterPlaceholder from '@/components/TypewriterPlaceholder.vue'
import ParticleField from '@/components/ParticleField.vue'
import ContentState from '@/components/ContentState.vue'
import { usePromptOptimizer } from '@/composables/usePromptOptimizer'
import { useAsyncSection } from '@/composables/useAsyncSection'
import { addApp, listMyAppVoByPage, listGoodAppVoByPage } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import { formatCodeGenType, getCodeGenTypeOptions } from '@/utils/codeGenTypes'
import { formatDate } from '@/utils/time'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const { t, tm, rt } = useI18n({ useScope: 'global' })

// 用户提示词
const userPrompt = ref('')
const composerRef = ref<HTMLTextAreaElement>()
const creating = ref(false)

// 动态占位符：输入框有内容时暂停并隐藏，清空后恢复
const typewriterActive = computed(() => userPrompt.value.length === 0)
const typewriterLines = computed(() => {
  const messages = tm('home.typewriter') as unknown
  if (!Array.isArray(messages)) return []
  return messages.map((message) => rt(message as string))
})

// 快捷提示词（文案与预设提示词均由 i18n 管理）
const quickActionKeys = ['blog', 'corporate', 'shop', 'portfolio'] as const

// 提示词优化（前端模拟，后续接入后端时替换 optimize 实现即可）
const {
  state: optimizerState,
  optimize,
  undo: undoOptimize,
  reset: resetOptimizer,
} = usePromptOptimizer({
  enhance: (prompt) => `${prompt.trim()}\n\n${t('home.optimize.enhanceSuffix')}`,
})

const optimizerTip = computed(() => {
  if (optimizerState.value === 'optimizing') return t('home.optimize.optimizing')
  if (optimizerState.value === 'optimized') return t('home.optimize.undoTip')
  return t('home.optimize.tip')
})

const onOptimize = async () => {
  if (optimizerState.value === 'optimizing') return

  if (optimizerState.value === 'optimized') {
    userPrompt.value = undoOptimize()
    composerRef.value?.focus()
    return
  }

  if (!userPrompt.value.trim()) {
    message.info(t('home.optimize.empty'))
    return
  }

  const source = userPrompt.value
  const optimized = await optimize(source)
  // 优化期间用户改动了文案，则丢弃本次结果
  if (optimized === null || userPrompt.value !== source) return

  userPrompt.value = optimized
  composerRef.value?.focus()
}

// 手动编辑后退出「撤回优化」态，避免撤回覆盖用户的新内容
const onPromptInput = () => {
  if (optimizerState.value !== 'idle') resetOptimizer()
}

// 登录态
const isLoggedIn = computed(() => Boolean(loginUserStore.loginUser.id))

// 我的应用数据（独立区块状态：loading / empty / error / retry）
const {
  data: myAppsData,
  loading: myAppsLoading,
  error: myAppsError,
  run: loadMyApps,
} = useAsyncSection<API.AppVO[]>(
  async () => {
    const res = await listMyAppVoByPage({
      pageNum: 1,
      pageSize: 3,
      sortField: 'updateTime',
      sortOrder: 'desc',
    })

    if (res.data.code !== 0) {
      throw new Error(res.data.message || 'load my apps failed')
    }
    return res.data.data?.records ?? []
  },
  { errorMessage: () => t('home.loadFailed') },
)

const myApps = computed(() => myAppsData.value ?? [])
const myAppsEmpty = computed(() => !isLoggedIn.value || myApps.value.length === 0)

// 精选应用数据（独立区块状态：loading / empty / error / retry）
const featuredType = ref('')
const {
  data: featuredData,
  loading: featuredLoading,
  error: featuredError,
  run: loadFeaturedApps,
} = useAsyncSection<API.AppVO[]>(
  async () => {
    const res = await listGoodAppVoByPage({
      pageNum: 1,
      pageSize: 6,
      sortField: 'createTime',
      sortOrder: 'desc',
      codeGenType: featuredType.value || undefined,
    })

    if (res.data.code !== 0) {
      throw new Error(res.data.message || 'load featured apps failed')
    }
    return res.data.data?.records ?? []
  },
  { errorMessage: () => t('home.loadFailed') },
)

const featuredApps = computed(() => featuredData.value ?? [])
const featuredEmpty = computed(
  () => !featuredLoading.value && !featuredError.value && featuredApps.value.length === 0,
)
const featuredTypeOptions = computed(() => [
  { label: t('home.filterAll'), value: '' },
  ...getCodeGenTypeOptions().map((option) => ({
    label: option.label,
    value: option.value,
  })),
])

// 设置提示词并聚焦输入框
const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
  composerRef.value?.focus()
}

// 新建应用：聚焦输入框
const focusComposer = () => {
  userPrompt.value = ''
  composerRef.value?.focus()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 创建应用
const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning(t('home.warnEmpty'))
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning(t('home.warnLogin'))
    await router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({
      initPrompt: userPrompt.value.trim(),
    })

    if (res.data.code === 0 && res.data.data) {
      message.success(t('home.createSuccess'))
      // 跳转到对话页面，确保ID是字符串类型
      const appId = String(res.data.data)
      await router.push(`/app/chat/${appId}`)
    } else {
      message.error(t('home.createFailedWithReason', { msg: res.data.message }))
    }
  } catch (error) {
    console.error('创建应用失败：', error)
    message.error(t('home.createFailed'))
  } finally {
    creating.value = false
  }
}

// 精选类型筛选
const setFeaturedType = (value: string) => {
  featuredType.value = value
  loadFeaturedApps()
}

// 游客态：去登录
const goLogin = () => {
  router.push('/user/login')
}

// 查看对话
const viewChat = (appId: string | number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}?view=1`)
  }
}

// 查看部署后的作品
const viewWork = (app: API.AppVO) => {
  if (app.deployKey) {
    const url = getDeployUrl(app.deployKey)
    window.open(url, '_blank')
  }
}

// 页面加载时获取数据
onMounted(() => {
  nextTick(() => {
    if (isLoggedIn.value) loadMyApps()
    loadFeaturedApps()
  })
})
</script>

<template>
  <div id="homePage">
    <!-- 页面级氛围层：渐变光晕 + 粒子（不拦截交互） -->
    <div class="home-ambient" aria-hidden="true">
      <ParticleField class="home-ambient__particles" />
    </div>

    <!-- Hero：一句话标题 + composer + 快捷提示 -->
    <section class="home-hero">
      <div class="home-container">
        <h1 class="home-hero__title">
          <span>{{ t('home.titleBefore') }}</span>
          <BrandLogo class="home-hero__logo" :mark-width="44" :wordmark="false" />
          <span>{{ t('home.titleAfter') }}</span>
        </h1>
        <p class="home-hero__sub">{{ t('home.sub') }}</p>

        <div class="home-composer">
          <TypewriterPlaceholder :lines="typewriterLines" :active="typewriterActive" />
          <textarea
            ref="composerRef"
            v-model="userPrompt"
            class="home-composer__input"
            :class="{ 'home-composer__input--empty': typewriterActive }"
            rows="3"
            :aria-label="t('home.composerLabel')"
            :maxlength="1000"
            @input="onPromptInput"
          ></textarea>
          <div class="home-composer__bar">
            <div class="home-composer__tools">
              <a-tooltip :title="optimizerTip">
                <button
                  type="button"
                  class="home-optimize"
                  :class="{
                    'home-optimize--done': optimizerState === 'optimized',
                    'home-optimize--busy': optimizerState === 'optimizing',
                  }"
                  :aria-label="optimizerTip"
                  :aria-busy="optimizerState === 'optimizing'"
                  @click="onOptimize"
                >
                  <svg
                    v-if="optimizerState === 'optimizing'"
                    class="home-optimize__spinner"
                    width="15"
                    height="15"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2.2"
                    stroke-linecap="round"
                    aria-hidden="true"
                  >
                    <circle cx="12" cy="12" r="9" stroke-dasharray="42 57" />
                  </svg>
                  <svg
                    v-else-if="optimizerState === 'optimized'"
                    width="15"
                    height="15"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.9"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    aria-hidden="true"
                  >
                    <path d="M9 14 4 9l5-5" />
                    <path d="M4 9h10a6 6 0 0 1 0 12h-3" />
                  </svg>
                  <svg
                    v-else
                    width="15"
                    height="15"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    aria-hidden="true"
                  >
                    <path d="M4 20l9-9" />
                    <path d="M15 3.5l.8 2.2 2.2.8-2.2.8-.8 2.2-.8-2.2-2.2-.8 2.2-.8.8-2.2z" />
                    <path d="M20 12v2M18 13h4M19.5 5.5l1.5-1.5" />
                  </svg>
                  <span>
                    {{
                      optimizerState === 'optimized'
                        ? t('home.optimize.undo')
                        : t('home.optimize.action')
                    }}
                  </span>
                </button>
              </a-tooltip>
              <a-tooltip :title="t('home.optimize.help')" :overlay-style="{ maxWidth: '280px' }">
                <button type="button" class="home-help" :aria-label="t('home.optimize.help')">
                  <svg
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    aria-hidden="true"
                  >
                    <circle cx="12" cy="12" r="9" />
                    <path d="M9.6 9.2a2.4 2.4 0 1 1 3.5 2.2c-.7.4-1.1 1-1.1 1.8v.4" />
                    <path d="M12 17h.01" />
                  </svg>
                </button>
              </a-tooltip>
            </div>
            <button
              class="home-send"
              :disabled="creating"
              :title="t('home.generate')"
              :aria-label="t('home.generate')"
              @click="createApp"
            >
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
              >
                <path d="M12 19V5M5 12l7-7 7 7" />
              </svg>
            </button>
          </div>
        </div>

        <div class="home-quick">
          <div class="home-quick__chips">
            <button
              v-for="key in quickActionKeys"
              :key="key"
              class="home-chip home-chip--preset"
              @click="setPrompt(t(`home.quick.${key}.prompt`))"
            >
              {{ t(`home.quick.${key}.label`) }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- 我的作品 -->
    <section class="home-section">
      <div class="home-container">
        <div class="home-panel">
          <div class="home-panel__head">
            <h2>{{ t('home.mine') }}</h2>
            <RouterLink to="/user/profile" class="home-quiet-btn">
              {{ t('home.all') }}
            </RouterLink>
          </div>
          <ContentState
            :loading="myAppsLoading"
            :error="myAppsError"
            :empty="myAppsEmpty"
            :retry-text="t('home.retry')"
            @retry="loadMyApps"
          >
            <template #empty>
              <div class="home-empty">
                <p>{{ isLoggedIn ? t('home.myEmpty') : t('home.guestEmpty') }}</p>
                <button
                  type="button"
                  class="home-empty__action"
                  @click="isLoggedIn ? focusComposer() : goLogin()"
                >
                  {{ isLoggedIn ? t('home.myEmptyAction') : t('home.guestAction') }}
                </button>
              </div>
            </template>
            <div class="home-grid">
              <article
                v-for="app in myApps"
                :key="app.id"
                class="home-appcard"
                @click="viewChat(app.id)"
              >
                <div class="home-appcard__cover">
                  <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
                  <div v-else class="home-appcard__fallback">#{{ app.id }}</div>
                  <span v-if="app.deployKey" class="home-badge home-badge--live">
                    <i class="home-dot home-dot--live"></i>{{ t('home.live') }}
                  </span>
                  <span v-else class="home-badge">{{ t('home.draft') }}</span>
                </div>
                <div class="home-appcard__body">
                  <h3 class="home-appcard__title">{{ app.appName || t('appCard.untitled') }}</h3>
                  <div class="home-appcard__meta">
                    <span class="home-tag">{{ formatCodeGenType(app.codeGenType) }}</span>
                    <span
                      >{{ t('home.updated') }}
                      {{ formatDate(app.updateTime || app.createTime) }}</span
                    >
                  </div>
                </div>
              </article>

              <a class="home-newcard" @click="focusComposer">
                <span class="home-newcard__plus">+</span>
                <span class="home-newcard__label">{{ t('home.new') }}</span>
              </a>
            </div>
          </ContentState>
        </div>
      </div>
    </section>

    <!-- 精选案例 -->
    <section class="home-section home-section--last">
      <div class="home-container">
        <div class="home-panel">
          <div class="home-panel__head">
            <h2>{{ t('home.featured') }}</h2>
            <div class="home-filter">
              <button
                v-for="option in featuredTypeOptions"
                :key="option.value"
                class="home-chip home-chip--filter"
                :class="{ 'home-chip--active': featuredType === option.value }"
                @click="setFeaturedType(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
          </div>
          <ContentState
            :loading="featuredLoading"
            :error="featuredError"
            :empty="featuredEmpty"
            :empty-text="t('home.featuredEmpty')"
            :retry-text="t('home.retry')"
            @retry="loadFeaturedApps"
          >
            <div class="home-grid">
              <article
                v-for="app in featuredApps"
                :key="app.id"
                class="home-appcard"
                @click="viewChat(app.id)"
              >
                <div class="home-appcard__cover">
                  <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
                  <div v-else class="home-appcard__fallback">#{{ app.id }}</div>
                </div>
                <div class="home-appcard__body">
                  <h3 class="home-appcard__title">{{ app.appName || t('appCard.untitled') }}</h3>
                  <div class="home-appcard__meta">
                    <span class="home-tag">{{ formatCodeGenType(app.codeGenType) }}</span>
                    <span class="home-appcard__author">
                      {{ app.user?.userName || t('appCard.official') }}
                    </span>
                    <a-button
                      v-if="app.deployKey"
                      type="link"
                      size="small"
                      class="home-appcard__visit"
                      @click.stop="viewWork(app)"
                    >
                      {{ t('appCard.viewWork') }}
                    </a-button>
                  </div>
                </div>
              </article>
            </div>
          </ContentState>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* ============================================================
   首页：氛围渐变 + 粒子 + 一句话标题 + 双面板
   ============================================================ */
#homePage {
  position: relative;
  isolation: isolate;
}

.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ---------------- 氛围层 ---------------- */
.home-ambient {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  background: var(--home-bg-gradient);
}

.home-ambient::before,
.home-ambient::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.85;
}

.home-ambient::before {
  top: 8%;
  left: -12%;
  width: min(60vw, 760px);
  height: min(60vw, 760px);
  background: var(--home-glow-1);
}

.home-ambient::after {
  top: 34%;
  right: -10%;
  width: min(52vw, 640px);
  height: min(52vw, 640px);
  background: var(--home-glow-2);
}

.home-ambient__particles {
  position: absolute;
  inset: 0;
}

/* ---------------- Hero ---------------- */
.home-hero {
  position: relative;
  z-index: 1;
  padding: 88px 0 64px;
  text-align: center;
  overflow: hidden;
}

.home-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-muted);
  flex: none;
}

.home-dot--live {
  background: var(--brand);
  box-shadow: 0 0 0 0 var(--brand-ring);
  animation: home-pulse 2s var(--ease) infinite;
}

@keyframes home-pulse {
  0% {
    box-shadow: 0 0 0 0 var(--brand-ring);
  }
  100% {
    box-shadow: 0 0 0 8px rgba(0, 0, 0, 0);
  }
}

.home-hero__title {
  position: relative;
  max-width: 900px;
  margin: 0 auto 16px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: var(--text-display);
  line-height: var(--leading-display);
  font-weight: var(--weight-heavy);
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.home-hero__title > span {
  display: inline-block;
}

.home-hero__logo {
  flex: none;
}

.home-hero__logo :deep(svg) {
  width: auto;
  height: 0.9em;
}

.home-hero__sub {
  position: relative;
  max-width: 560px;
  margin: 0 auto 36px;
  color: var(--text-secondary);
}

/* ---------------- Composer ---------------- */
.home-composer {
  position: relative;
  max-width: 780px;
  margin: 0 auto;
  text-align: left;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
  transition:
    border-color var(--dur) var(--ease),
    box-shadow var(--dur) var(--ease);
}

.home-composer:focus-within {
  border-color: var(--brand);
  box-shadow:
    0 0 0 4px var(--brand-ring),
    var(--shadow-md);
}

.home-composer__input {
  position: relative;
  z-index: 0;
  display: block;
  width: 100%;
  border: 0;
  background: transparent;
  resize: none;
  font: inherit;
  font-size: var(--text-title);
  line-height: 1.6;
  color: var(--text-primary);
  padding: 20px 20px 8px;
}

.home-composer__input:focus {
  outline: none;
}

/* 空输入时隐藏原生光标，避免与打字机光标重叠 */
.home-composer__input--empty {
  caret-color: transparent;
}

.home-composer__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px 16px;
}

.home-composer__tools {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.home-optimize {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: var(--radius-full);
  background: var(--bg-surface-muted);
  color: var(--text-secondary);
  font-size: var(--text-caption);
  font-weight: var(--weight-medium);
  cursor: pointer;
  transition: all var(--dur-fast) var(--ease);
}

.home-optimize:hover {
  color: var(--text-primary);
  border-color: var(--border-strong);
}

.home-optimize--done {
  background: var(--brand-soft);
  color: var(--brand-text);
}

.home-optimize--busy {
  cursor: progress;
}

.home-optimize__spinner {
  animation: home-optimize-spin 0.9s linear infinite;
}

@keyframes home-optimize-spin {
  to {
    transform: rotate(360deg);
  }
}

.home-help {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: all var(--dur-fast) var(--ease);
}

.home-help:hover {
  color: var(--text-primary);
  background: var(--bg-surface-muted);
}

@media (prefers-reduced-motion: reduce) {
  .home-optimize__spinner {
    animation: none;
  }
}

.home-send {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: 50%;
  background: var(--brand);
  color: var(--brand-on);
  cursor: pointer;
  flex: none;
  transition:
    background var(--dur-fast) var(--ease),
    transform var(--dur-fast) var(--ease);
}

.home-send:hover {
  background: var(--brand-hover);
  transform: scale(1.04);
}

.home-send[disabled] {
  opacity: 0.45;
  pointer-events: none;
}

/* ---------------- 快捷提示 ---------------- */
.home-quick {
  position: relative;
  max-width: 900px;
  margin: 22px auto 0;
  display: flex;
  justify-content: center;
}

.home-quick__chips {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.home-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 12px;
  background: var(--bg-surface-muted);
  border: 1px solid transparent;
  border-radius: var(--radius-full);
  color: var(--text-secondary);
  font-size: var(--text-caption);
  font-weight: var(--weight-medium);
  cursor: pointer;
  transition: all var(--dur-fast) var(--ease);
}

.home-chip:hover {
  color: var(--text-primary);
  border-color: var(--border-strong);
}

.home-chip--preset {
  height: 38px;
  padding: 0 18px;
  font-size: var(--text-body);
}

.home-chip--active {
  background: var(--brand-soft);
  color: var(--brand-text);
  border-color: transparent;
}

/* ---------------- 区块面板 ---------------- */
.home-section {
  position: relative;
  z-index: 1;
  padding: 40px 0 0;
}

.home-section--last {
  padding-bottom: 80px;
}

.home-panel {
  position: relative;
  padding: 28px;
  background: var(--home-panel-bg);
  border: 1px solid var(--home-panel-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--home-panel-shadow);
  backdrop-filter: blur(14px) saturate(130%);
}

.home-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
}

.home-panel__head h2 {
  margin: 0;
  font-size: var(--text-h2);
  line-height: var(--leading-h2);
  font-weight: var(--weight-strong);
  color: var(--text-primary);
}

.home-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.home-quiet-btn {
  font-size: var(--text-caption);
  font-weight: var(--weight-medium);
  color: var(--text-secondary);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  border: 1px solid transparent;
  transition: all var(--dur-fast) var(--ease);
}

.home-quiet-btn:hover {
  color: var(--text-primary);
  border-color: var(--border);
}

/* ---------------- 应用卡片 ---------------- */
.home-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

@media (max-width: 1024px) {
  .home-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .home-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .home-hero {
    padding: 48px 0 56px;
  }

  .home-hero__title {
    font-size: var(--text-h1);
  }
}

.home-appcard {
  display: flex;
  flex-direction: column;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  overflow: hidden;
  cursor: pointer;
  transition:
    border-color var(--dur) var(--ease),
    box-shadow var(--dur) var(--ease);
}

.home-appcard:hover {
  border-color: var(--border-strong);
  box-shadow: var(--shadow-md);
}

.home-appcard__cover {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: var(--bg-surface-muted);
  border-bottom: 1px solid var(--border);
}

.home-appcard__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.home-appcard__fallback {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background:
    repeating-linear-gradient(
      45deg,
      transparent 0 10px,
      color-mix(in srgb, var(--text-muted) 6%, transparent) 10px 20px
    ),
    var(--bg-surface-muted);
  color: var(--text-muted);
  font-family: var(--font-mono);
  font-size: var(--text-micro);
}

.home-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 22px;
  padding: 0 8px;
  border-radius: var(--radius-xs);
  border: 1px solid var(--border);
  background: var(--bg-surface-muted);
  color: var(--text-secondary);
  font-size: var(--text-micro);
  font-weight: var(--weight-medium);
}

.home-badge--live {
  background: var(--brand-soft);
  border-color: transparent;
  color: var(--brand-text);
}

.home-appcard__body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.home-appcard__title {
  margin: 0;
  font-size: var(--text-title);
  font-weight: var(--weight-medium);
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.home-appcard__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: var(--text-micro);
  color: var(--text-muted);
}

.home-appcard__author {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.home-appcard__visit {
  margin-left: auto;
  padding: 0;
  height: auto;
}

.home-tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: var(--radius-xs);
  border: 1px solid var(--border);
  background: var(--bg-surface-muted);
  color: var(--text-secondary);
  font-family: var(--font-mono);
  font-size: var(--text-micro);
  font-weight: var(--weight-medium);
  white-space: nowrap;
  flex: none;
}

/* ---------------- 新建应用卡 ---------------- */
.home-newcard {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 100%;
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-card);
  color: var(--text-muted);
  cursor: pointer;
  padding: 32px 24px;
  transition:
    border-color var(--dur) var(--ease),
    color var(--dur) var(--ease),
    background var(--dur) var(--ease);
}

.home-newcard:hover {
  border-color: var(--brand);
  color: var(--brand-text);
  background: var(--brand-soft);
}

.home-newcard__plus {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid currentColor;
  display: grid;
  place-items: center;
  font-family: var(--font-mono);
}

.home-newcard__label {
  font-size: var(--text-caption);
  font-weight: var(--weight-medium);
  color: var(--text-secondary);
}

/* ---------------- 空状态引导 ---------------- */
.home-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 32px 16px;
  text-align: center;
}

.home-empty p {
  margin: 0;
  color: var(--text-secondary);
}

.home-empty__action {
  height: 34px;
  padding: 0 16px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-full);
  background: var(--bg-surface);
  color: var(--text-primary);
  font-size: var(--text-caption);
  font-weight: var(--weight-medium);
  cursor: pointer;
  transition: all var(--dur-fast) var(--ease);
}

.home-empty__action:hover {
  border-color: var(--brand);
  background: var(--brand-soft);
  color: var(--brand-text);
}
</style>
