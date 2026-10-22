<template>
  <div id="appChatPage">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-left">
        <h1 class="app-name">{{ appInfo?.appName || t('chat.defaultAppName') }}</h1>
        <a-tag v-if="appInfo?.codeGenType" class="code-gen-type-tag">
          {{ formatCodeGenType(appInfo.codeGenType) }}
        </a-tag>
      </div>
      <div class="header-right">
        <a-button type="default" @click="showAppDetail">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          {{ t('chat.detail') }}
        </a-button>
        <a-button
          type="primary"
          ghost
          @click="downloadCode"
          :loading="downloading"
          :disabled="(!isOwner && !isAdmin) || isBuilding"
        >
          <template #icon>
            <DownloadOutlined />
          </template>
          {{ t('chat.download') }}
        </a-button>
        <a-button
          type="primary"
          @click="deployApp"
          :loading="deploying"
          :disabled="isBuilding"
        >
          <template #icon>
            <CloudUploadOutlined />
          </template>
          {{ t('chat.deploy') }}
        </a-button>
      </div>
    </div>

    <!-- 主要内容区域：左右分栏，外围固定，各自内部滚动 -->
    <div class="main-content" ref="mainContentRef" :class="{ 'is-dragging': isDragging }">
      <!-- 左侧对话区域 -->
      <div class="chat-section" :style="{ '--chat-w': chatWidthPct + '%' }">
        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer">
          <!-- 加载更多按钮 -->
          <div v-if="hasMoreHistory" class="load-more-container">
            <a-button type="link" @click="loadMoreHistory" :loading="loadingHistory" size="small">
              {{ t('chat.loadMoreHistory') }}
            </a-button>
          </div>
          <div v-for="(message, index) in messages" :key="index" class="message-item">
            <div v-if="message.type === 'user'" class="user-message">
              <div class="message-content">{{ message.content }}</div>
              <div class="message-avatar">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              </div>
            </div>
            <div v-else class="ai-message">
              <div class="message-avatar">
                <span class="ai-avatar"><BrandLogo :mark-width="24" :wordmark="false" /></span>
              </div>
              <div class="message-content">
                <MarkdownRenderer
                  v-if="message.content"
                  :content="message.content"
                  :streaming="index === streamingMessageIndex"
                />
                <div v-if="message.loading" class="loading-indicator">
                  <a-spin size="small" />
                  <span>{{ t('chat.aiThinking') }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 选中元素信息展示 -->
        <a-alert
          v-if="selectedElementInfo"
          class="selected-element-alert"
          type="info"
          closable
          @close="clearSelectedElement"
        >
          <template #message>
            <div class="selected-element-info">
              <div class="element-header">
                <span class="element-tag">
                  {{ t('chat.selectedElement', { tag: selectedElementInfo.tagName.toLowerCase() }) }}
                </span>
                <span v-if="selectedElementInfo.id" class="element-id">
                  #{{ selectedElementInfo.id }}
                </span>
                <span v-if="selectedElementInfo.className" class="element-class">
                  .{{ selectedElementInfo.className.split(' ').join('.') }}
                </span>
              </div>
              <div class="element-details">
                <div v-if="selectedElementInfo.textContent" class="element-item">
                  {{ t('chat.textContent', { text: selectedElementInfo.textContent.substring(0, 50) }) }}
                  {{ selectedElementInfo.textContent.length > 50 ? '...' : '' }}
                </div>
                <div v-if="selectedElementInfo.pagePath" class="element-item">
                  {{ t('chat.pagePath', { path: selectedElementInfo.pagePath }) }}
                </div>
                <div class="element-item">
                  {{ t('chat.selector') }}
                  <code class="element-selector-code">{{ selectedElementInfo.selector }}</code>
                </div>
              </div>
            </div>
          </template>
        </a-alert>

        <!-- 用户消息输入框 -->
        <div class="input-container">
          <div class="input-wrapper">
            <a-tooltip v-if="!isOwner" :title="t('chat.notOwnerTip')" placement="top">
              <a-textarea
                v-model:value="userInput"
                :placeholder="getInputPlaceholder()"
                :rows="4"
                :maxlength="1000"
                @keydown.enter.prevent="sendMessage"
                :disabled="isGenerating || !isOwner"
              />
            </a-tooltip>
            <a-textarea
              v-else
              v-model:value="userInput"
              :placeholder="getInputPlaceholder()"
              :rows="4"
              :maxlength="1000"
              @keydown.enter.prevent="sendMessage"
              :disabled="isGenerating"
            />
            <div class="input-actions">
              <a-button
                type="primary"
                @click="sendMessage"
                :loading="isGenerating"
                :disabled="!isOwner"
              >
                <template #icon>
                  <SendOutlined />
                </template>
              </a-button>
            </div>
          </div>
        </div>
      </div>
      <!-- 拖拽分隔条 -->
      <div
        class="layout-splitter"
        :class="{ 'layout-splitter--dragging': isDragging }"
        @pointerdown="onSplitterDown"
        @pointermove="onSplitterMove"
        @pointerup="onSplitterUp"
        @pointercancel="onSplitterUp"
      >
        <i></i>
      </div>
      <!-- 右侧网页展示区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <h3>{{ t('chat.previewTitle') }}</h3>
          <div class="preview-actions">
            <a-button
              v-if="isOwner && previewUrl"
              type="link"
              :danger="isEditMode"
              @click="toggleEditMode"
              :class="{ 'edit-mode-active': isEditMode }"
              style="padding: 0; height: auto; margin-right: 12px"
            >
              <template #icon>
                <EditOutlined />
              </template>
              {{ isEditMode ? t('chat.exitEditMode') : t('chat.editMode') }}
            </a-button>
            <a-button v-if="previewUrl" type="link" @click="openInNewTab">
              <template #icon>
                <ExportOutlined />
              </template>
              {{ t('chat.openNewTab') }}
            </a-button>
          </div>
        </div>
        <div class="preview-content">
          <div v-if="isGenerating" class="preview-loading">
            <a-spin size="large" />
            <p>{{ t('chat.generating') }}</p>
          </div>
          <div v-else-if="isBuilding" class="preview-building">
            <a-spin size="large" />
            <p class="build-status-text">{{ buildPhaseText || t('chat.building') }}</p>
            <div v-if="buildLogs.length" class="build-log-panel">
              <div v-for="(log, index) in buildLogs.slice(-8)" :key="index" class="build-log-line">
                {{ log }}
              </div>
            </div>
          </div>
          <div v-else-if="buildError" class="preview-error">
            <div class="error-icon">⚠️</div>
            <p class="error-text">{{ t('chat.buildFailed', { msg: buildError }) }}</p>
            <p class="error-hint">{{ t('chat.buildFailedHint') }}</p>
          </div>
          <div v-else-if="!previewUrl" class="preview-placeholder">
            <div class="placeholder-icon">🌐</div>
            <p>{{ t('chat.placeholderIdle') }}</p>
          </div>
          <iframe
            v-else
            :src="previewUrl"
            class="preview-iframe"
            frameborder="0"
            @load="onIframeLoad"
          ></iframe>
        </div>
      </div>
    </div>

    <!-- 应用详情弹窗 -->
    <AppDetailModal
      v-model:open="appDetailVisible"
      :app="appInfo"
      :show-actions="isOwner || isAdmin"
      @edit="editApp"
      @delete="deleteApp"
    />

    <!-- 部署成功弹窗 -->
    <DeploySuccessModal
      v-model:open="deployModalVisible"
      :deploy-url="deployUrl"
      @open-site="openDeployedSite"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  getAppVoById,
  deployApp as deployAppApi,
  deleteApp as deleteAppApi,
} from '@/api/appController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import { CodeGenTypeEnum, formatCodeGenType } from '@/utils/codeGenTypes'
import request from '@/request'

import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import BrandLogo from '@/components/BrandLogo.vue'
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'
import { VisualEditor, type ElementInfo } from '@/utils/visualEditor'

import {
  CloudUploadOutlined,
  SendOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  DownloadOutlined,
  EditOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const { t } = useI18n({ useScope: 'global' })

// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref<any>()

// 对话相关
interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
}

// 构建进度 SSE 消息（与后端 BuildProgressMessage 协议对应）
interface BuildProgressPayload {
  type: 'progress' | 'log' | 'done'
  appId?: number
  phase?: string
  status?: 'running' | 'success' | 'failed' | 'cancelled' | 'idle'
  message?: string
  line?: string
  timestamp?: number
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
// 正在流式输出的 AI 气泡下标：该气泡的 MarkdownRenderer 跳过语法高亮（流式性能优化），
// 流结束/失败时置回 -1，组件会一次性补上最终高亮
const streamingMessageIndex = ref(-1)

// 流式内容渲染节流间隔（毫秒）：chunk 只累加，渲染按此间隔批量刷新。
// 巨型回复（如整站 HTML，可达数百 KB）若每个 chunk 都全量 markdown 重渲染，
// 会以 O(n²) 的主线程开销把页面卡死（本次修复的 Bug 根因）。
const STREAM_RENDER_THROTTLE_MS = 150

// 构建进度相关
const isBuilding = ref(false)
const buildPhaseText = ref('')
const buildLogs = ref<string[]>([])
const buildError = ref('')
const BUILD_LOG_MAX = 200

// SSE 连接（组件卸载时需手动关闭）
let generationEventSource: EventSource | null = null
let buildEventSource: EventSource | null = null
const messagesContainer = ref<HTMLElement>()

// 对话历史相关
const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string>()
const historyLoaded = ref(false)

// 预览相关
const previewUrl = ref('')
const previewReady = ref(false)

// 部署相关
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

// 下载相关
const downloading = ref(false)

// 可视化编辑相关
const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
  },
})

// 左右分栏拖拽调宽（百分比，限制在 26%–72%）
const mainContentRef = ref<HTMLElement>()
const chatWidthPct = ref(42)
const isDragging = ref(false)

const onSplitterDown = (event: PointerEvent) => {
  isDragging.value = true
  ;(event.currentTarget as HTMLElement).setPointerCapture(event.pointerId)
}

const onSplitterMove = (event: PointerEvent) => {
  if (!isDragging.value || !mainContentRef.value) return
  const rect = mainContentRef.value.getBoundingClientRect()
  const pct = ((event.clientX - rect.left) / rect.width) * 100
  chatWidthPct.value = Math.min(72, Math.max(26, pct))
}

const onSplitterUp = () => {
  isDragging.value = false
}

// 权限相关
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// 是否为 Vue 项目模式（需要异步构建 + 进度监听）
const isVueProject = computed(() => {
  return appInfo.value?.codeGenType === CodeGenTypeEnum.VUE_PROJECT
})

// 应用详情相关
const appDetailVisible = ref(false)

// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

// 加载对话历史
const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return
  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId.value,
      pageSize: 10,
    }
    // 如果是加载更多，传递最后一条消息的创建时间作为游标
    if (isLoadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }
    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const chatHistories = res.data.data.records || []
      if (chatHistories.length > 0) {
        // 将对话历史转换为消息格式，并按时间正序排列（老消息在前）
        const historyMessages: Message[] = chatHistories
          .map((chat) => ({
            type: (chat.messageType === 'user' ? 'user' : 'ai') as 'user' | 'ai',
            content: chat.message || '',
            createTime: chat.createTime,
          }))
          .reverse() // 反转数组，让老消息在前
        if (isLoadMore) {
          // 加载更多时，将历史消息添加到开头
          messages.value.unshift(...historyMessages)
        } else {
          // 初始加载，直接设置消息列表
          messages.value = historyMessages
        }
        // 更新游标
        lastCreateTime.value = chatHistories[chatHistories.length - 1]?.createTime
        // 检查是否还有更多历史
        hasMoreHistory.value = chatHistories.length === 10
      } else {
        hasMoreHistory.value = false
      }
      historyLoaded.value = true
    }
  } catch (error) {
    console.error('加载对话历史失败：', error)
    message.error(t('chat.loadHistoryFailed'))
  } finally {
    loadingHistory.value = false
  }
}

// 加载更多历史消息
const loadMoreHistory = async () => {
  await loadChatHistory(true)
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error(t('chat.noAppId'))
    router.push('/')
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 先加载对话历史
      await loadChatHistory()
      // 检查是否有进行中的生成（生成过程中离开页面后，服务端仍在独立生成），有则续接流式输出。
      // 即将自动发送初始提示词的场景跳过（避免与新生成的直连流重复渲染）。
      const willAutoSendInitial = !!(
        appInfo.value.initPrompt &&
        isOwner.value &&
        messages.value.length === 0 &&
        historyLoaded.value
      )
      if (!willAutoSendInitial) {
        resumeGenerationIfRunning()
      }
      // 如果有至少2条对话记录，展示对应的网站
      if (messages.value.length >= 2) {
        updatePreview()
      }
      // 检查是否需要自动发送初始提示词
      // 只有在是自己的应用且没有对话历史时才自动发送
      if (willAutoSendInitial) {
        await sendInitialMessage(appInfo.value.initPrompt!)
      }
    } else {
      message.error(t('chat.fetchAppFailed'))
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error(t('chat.fetchAppFailed'))
    router.push('/')
  }
}

// 发送初始消息
const sendInitialMessage = async (prompt: string) => {
  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: prompt,
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) {
    return
  }

  let message = userInput.value.trim()
  // 如果有选中的元素，将元素信息添加到提示词中
  if (selectedElementInfo.value) {
    let elementContext = t('chat.elementContext')
    if (selectedElementInfo.value.pagePath) {
      elementContext += t('chat.elementPagePath', { path: selectedElementInfo.value.pagePath })
    }
    elementContext += t('chat.elementTag', {
      tag: selectedElementInfo.value.tagName.toLowerCase(),
      selector: selectedElementInfo.value.selector,
    })
    if (selectedElementInfo.value.textContent) {
      elementContext += t('chat.elementContent', {
        text: selectedElementInfo.value.textContent.substring(0, 100),
      })
    }
    message += elementContext
  }
  userInput.value = ''
  // 添加用户消息（包含元素信息）
  messages.value.push({
    type: 'user',
    content: message,
  })

  // 发送消息后，清除选中元素并退出编辑模式
  if (selectedElementInfo.value) {
    clearSelectedElement()
    if (isEditMode.value) {
      toggleEditMode()
    }
  }

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(message, aiMessageIndex)
}

// 生成代码 - 使用 EventSource 处理流式响应
const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  let streamCompleted = false

  // 新一轮生成会让后端重新构建，先关闭旧的构建进度监听并重置构建状态
  closeBuildWatch()
  isBuilding.value = false
  buildError.value = ''

  try {
    // 获取 axios 配置的 baseURL
    const baseURL = request.defaults.baseURL || API_BASE_URL

    // 构建URL参数
    const params = new URLSearchParams({
      appId: appId.value || '',
      message: userMessage,
    })

    const url = `${baseURL}/app/chat/gen/code?${params}`

    // 创建 EventSource 连接
    generationEventSource = new EventSource(url, {
      withCredentials: true,
    })

    // 本轮流式输出指向该 AI 气泡：MarkdownRenderer 流式期间跳过语法高亮
    streamingMessageIndex.value = aiMessageIndex

    let fullContent = ''
    let lastFlushAt = 0

    // 流式内容节流刷新：chunk 只累加到 fullContent，按间隔批量渲染 + 滚动。
    // 直接每个 chunk 全量渲染会让巨型回复拖死主线程（页面卡死 bug 的根因）。
    const flushStreamingContent = (force = false) => {
      const now = Date.now()
      if (!force && now - lastFlushAt < STREAM_RENDER_THROTTLE_MS) return
      lastFlushAt = now
      messages.value[aiMessageIndex].content = fullContent
      messages.value[aiMessageIndex].loading = false
      scrollToBottom()
    }

    // 处理接收到的消息
    generationEventSource.onmessage = function (event) {
      if (streamCompleted) return

      try {
        // 解析JSON包装的数据
        const parsed = JSON.parse(event.data)
        const content = parsed.d

        // 拼接内容（渲染按 STREAM_RENDER_THROTTLE_MS 节流批量刷新）
        if (content !== undefined && content !== null) {
          fullContent += content
          flushStreamingContent()
        }
      } catch (error) {
        console.error('解析消息失败:', error)
        handleError(error, aiMessageIndex)
      }
    }

    // 处理done事件
    generationEventSource.addEventListener('done', function () {
      if (streamCompleted) return

      streamCompleted = true
      // 收尾：把节流缓冲的尾部内容一次性刷进气泡，并退出流式模式（补做最终语法高亮）
      flushStreamingContent(true)
      streamingMessageIndex.value = -1
      isGenerating.value = false
      generationEventSource?.close()
      generationEventSource = null

      onGenerationFinished()
    })

    // 处理business-error事件（后端限流等错误）
    generationEventSource.addEventListener('business-error', function (event: MessageEvent) {
      if (streamCompleted) return

      try {
        const errorData = JSON.parse(event.data)
        console.error('SSE业务错误事件:', errorData)

        // 显示具体的错误信息
        const errorMessage = errorData.message || t('chat.genError')
        messages.value[aiMessageIndex].content = `❌ ${errorMessage}`
        messages.value[aiMessageIndex].loading = false
        message.error(errorMessage)

        streamCompleted = true
        streamingMessageIndex.value = -1
        isGenerating.value = false
        generationEventSource?.close()
      } catch (parseError) {
        console.error('解析错误事件失败:', parseError, '原始数据:', event.data)
        handleError(new Error(t('chat.serverError')), aiMessageIndex)
      }
    })

    // 处理错误
    generationEventSource.onerror = function () {
      if (streamCompleted || !isGenerating.value) return
      // 检查是否是正常的连接关闭
      if (generationEventSource?.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        flushStreamingContent(true)
        streamingMessageIndex.value = -1
        isGenerating.value = false
        generationEventSource?.close()
        generationEventSource = null

        onGenerationFinished()
      } else {
        handleError(new Error(t('chat.sseError')), aiMessageIndex)
      }
    }
  } catch (error) {
    console.error('创建 EventSource 失败：', error)
    handleError(error, aiMessageIndex)
  }
}

// 续接进行中的生成（SSE）：生成过程中离开页面后回来，服务端仍在独立生成，此处续看流式输出。
// 三种后端响应：idle（无任务，静默关闭）/ finished（已完成，重拉历史）/ 进行中（快照 + 实时流 + done）。
let resumeEventSource: EventSource | null = null

const resumeGenerationIfRunning = () => {
  const baseURL = request.defaults.baseURL || API_BASE_URL
  const url = `${baseURL}/app/chat/gen/progress?appId=${appId.value}`
  resumeEventSource = new EventSource(url, {
    withCredentials: true,
  })

  let aiMessageIndex = -1
  let fullContent = ''
  let lastFlushAt = 0

  // 与 generateCode 相同的流式渲染节流（快照帧可能很大，实时增量 chunk 密集）
  const flushStreamingContent = (force = false) => {
    if (aiMessageIndex === -1) return
    const now = Date.now()
    if (!force && now - lastFlushAt < STREAM_RENDER_THROTTLE_MS) return
    lastFlushAt = now
    messages.value[aiMessageIndex].content = fullContent
    messages.value[aiMessageIndex].loading = false
    scrollToBottom()
  }

  // 无生成任务：静默关闭
  resumeEventSource.addEventListener('idle', () => {
    resumeEventSource?.close()
    resumeEventSource = null
  })

  // 生成已结束（AI 回复已入库）：重新拉取对话历史展示完整回复
  resumeEventSource.addEventListener('finished', () => {
    resumeEventSource?.close()
    resumeEventSource = null
    lastCreateTime.value = undefined
    loadChatHistory()
  })

  // 进行中：首帧为已生成内容的合并快照，之后为实时增量（与生成流相同的数据格式 {"d": ...}）
  resumeEventSource.onmessage = function (event) {
    try {
      const parsed = JSON.parse(event.data)
      const content = parsed.d
      if (content === undefined || content === null) return
      if (aiMessageIndex === -1) {
        // 首次收到内容：创建 AI 气泡
        aiMessageIndex = messages.value.length
        messages.value.push({
          type: 'ai',
          content: '',
          loading: true,
        })
        isGenerating.value = true
        // 本轮流式输出指向该 AI 气泡：MarkdownRenderer 流式期间跳过语法高亮
        streamingMessageIndex.value = aiMessageIndex
      }
      fullContent += content
      flushStreamingContent()
    } catch (error) {
      console.error('解析续接消息失败:', error)
    }
  }

  // 生成完成：与 generateCode 相同的收尾（Vue 项目转构建进度监听，其余刷新预览）
  resumeEventSource.addEventListener('done', () => {
    resumeEventSource?.close()
    resumeEventSource = null
    // 收尾：把节流缓冲的尾部内容一次性刷进气泡，并退出流式模式（补做最终语法高亮）
    flushStreamingContent(true)
    streamingMessageIndex.value = -1
    isGenerating.value = false
    onGenerationFinished()
  })

  // 生成失败（服务端 business-error 事件，与 generateCode 相同的展示方式）
  resumeEventSource.addEventListener('business-error', (event: MessageEvent) => {
    try {
      const errorData = JSON.parse(event.data)
      const errorMessage = errorData.message || t('chat.genError')
      if (aiMessageIndex !== -1) {
        messages.value[aiMessageIndex].content = `❌ ${errorMessage}`
        messages.value[aiMessageIndex].loading = false
      }
      message.error(errorMessage)
    } catch (parseError) {
      console.error('解析续接错误事件失败:', parseError)
    }
    resumeEventSource?.close()
    resumeEventSource = null
    streamingMessageIndex.value = -1
    isGenerating.value = false
  })

  // 连接异常：静默关闭（回看失败不影响已加载的对话历史）
  resumeEventSource.onerror = () => {
    resumeEventSource?.close()
    resumeEventSource = null
    if (aiMessageIndex !== -1) {
      // 已有流式气泡时退出流式模式，补做最终语法高亮（内容保持已收到的部分）
      flushStreamingContent(true)
      streamingMessageIndex.value = -1
    }
  }
}

// 生成结束后的处理：Vue 项目转异步构建进度监听，其余类型直接刷新预览
const onGenerationFinished = () => {
  if (isVueProject.value) {
    // 后端已开始异步构建，订阅构建进度流
    buildLogs.value = []
    buildError.value = ''
    watchBuildProgress()
  } else {
    // AI 回复已通过流式输出在本地气泡中完整展示（无需重拉历史），
    // 应用信息（codeGenType 等）页面加载时已就绪（无需重新拉取），
    // 生成进度订阅是「离开页面后回来」场景专用的，正常收尾不该再订阅一次。
    // 仅需延迟刷新预览加载站点最新产物（带时间戳绕过 iframe 缓存）。
    setTimeout(() => updatePreview(true), 1000)
  }
}

// 关闭构建进度监听
const closeBuildWatch = () => {
  buildEventSource?.close()
  buildEventSource = null
}

// 订阅构建进度（SSE）：收到 done 终态事件后由服务端结束流
const watchBuildProgress = () => {
  if (!appId.value) return
  closeBuildWatch()

  const baseURL = request.defaults.baseURL || API_BASE_URL
  const url = `${baseURL}/build/progress?appId=${appId.value}`

  buildEventSource = new EventSource(url, {
    withCredentials: true,
  })

  // progress / log 消息走默认事件
  buildEventSource.onmessage = function (event) {
    try {
      const payload: BuildProgressPayload = JSON.parse(event.data)
      if (payload.type === 'progress') {
        isBuilding.value = true
        buildError.value = ''
        if (payload.message) {
          buildPhaseText.value = payload.message
        }
      } else if (payload.type === 'log' && payload.line) {
        buildLogs.value.push(payload.line)
        // 限制日志条数，避免内存无限增长
        if (buildLogs.value.length > BUILD_LOG_MAX) {
          buildLogs.value.splice(0, buildLogs.value.length - BUILD_LOG_MAX)
        }
      }
    } catch (error) {
      console.error('解析构建进度消息失败:', error)
    }
  }

  // 终态事件
  buildEventSource.addEventListener('done', (event) => {
    closeBuildWatch()
    isBuilding.value = false

    let status = ''
    let text = ''
    try {
      const payload: BuildProgressPayload = JSON.parse((event as MessageEvent).data)
      status = payload.status || ''
      text = payload.message || ''
    } catch (error) {
      console.error('解析构建终态消息失败:', error)
    }

    if (status === 'success') {
      buildError.value = ''
      message.success(t('chat.buildDone'))
      // 构建产物已更新，强制刷新预览避免 iframe 缓存
      updatePreview(true)
    } else if (status === 'failed') {
      buildError.value = text || t('chat.buildFailedDefault')
      message.error(t('chat.buildFailedMsg'))
    }
    // cancelled / idle：静默结束，不做额外处理
  })

  buildEventSource.onerror = function () {
    // 连接异常：关闭监听。构建仍由后端继续执行，刷新页面可重新订阅进度
    closeBuildWatch()
  }
}

// 错误处理函数
const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error('生成代码失败：', error)
  messages.value[aiMessageIndex].content = t('chat.genFailedBubble')
  messages.value[aiMessageIndex].loading = false
  message.error(t('chat.genFailedRetry'))
  isGenerating.value = false
  streamingMessageIndex.value = -1
}

// 更新预览（forceRefresh 时附加时间戳，强制 iframe 绕过缓存加载最新构建产物）
const updatePreview = (forceRefresh = false) => {
  if (appId.value) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    let newPreviewUrl = getStaticPreviewUrl(codeGenType, appId.value)
    if (forceRefresh) {
      newPreviewUrl += (newPreviewUrl.includes('?') ? '&' : '?') + `t=${Date.now()}`
    }
    previewUrl.value = newPreviewUrl
    previewReady.value = true
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 下载代码
const downloadCode = async () => {
  if (!appId.value) {
    message.error(t('chat.noAppId'))
    return
  }
  downloading.value = true
  try {
    const API_BASE_URL = request.defaults.baseURL || ''
    const url = `${API_BASE_URL}/app/download/${appId.value}`
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(t('chat.downloadFailedStatus', { status: response.status }))
    }
    // 获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName = contentDisposition?.match(/filename="(.+)"/)?.[1] || `app-${appId.value}.zip`
    // 下载文件
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    // 清理
    URL.revokeObjectURL(downloadUrl)
    message.success(t('chat.downloadSuccess'))
  } catch (error) {
    console.error('下载失败：', error)
    message.error(t('chat.downloadFailed'))
  } finally {
    downloading.value = false
  }
}

// 部署应用
const deployApp = async () => {
  if (!appId.value) {
    message.error(t('chat.noAppId'))
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value as unknown as number,
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success(t('chat.deploySuccess'))
    } else {
      message.error(t('chat.deployFailedWithReason', { msg: res.data.message }))
    }
  } catch (error) {
    console.error('部署失败：', error)
    message.error(t('chat.deployFailed'))
  } finally {
    deploying.value = false
  }
}

// 在新窗口打开预览
const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 打开部署的网站
const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

// iframe加载完成
const onIframeLoad = () => {
  previewReady.value = true
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (iframe) {
    visualEditor.init(iframe)
    visualEditor.onIframeLoad()
  }
}

// 编辑应用
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
const deleteApp = async () => {
  if (!appInfo.value?.id) return

  try {
    const res = await deleteAppApi({ id: appInfo.value.id })
    if (res.data.code === 0) {
      message.success(t('chat.deleteSuccess'))
      appDetailVisible.value = false
      router.push('/')
    } else {
      message.error(t('chat.deleteFailed', { msg: res.data.message }))
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error(t('chat.deleteFailedPlain'))
  }
}

// 可视化编辑相关函数
const toggleEditMode = () => {
  // 检查 iframe 是否已经加载
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (!iframe) {
    message.warning(t('chat.waitLoaded'))
    return
  }
  // 确保 visualEditor 已初始化
  if (!previewReady.value) {
    message.warning(t('chat.waitLoaded'))
    return
  }
  const newEditMode = visualEditor.toggleEditMode()
  isEditMode.value = newEditMode
}

const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

const getInputPlaceholder = () => {
  if (selectedElementInfo.value) {
    return t('chat.placeholderEditing', { tag: selectedElementInfo.value.tagName.toLowerCase() })
  }
  return t('chat.placeholder')
}

// 页面加载时获取应用信息
onMounted(async () => {
  await fetchAppInfo()

  // Vue 项目：订阅构建进度（覆盖刷新页面时构建仍在进行 / 刚完成的情况）
  if (isVueProject.value) {
    watchBuildProgress()
  }

  // 监听 iframe 消息
  window.addEventListener('message', (event) => {
    visualEditor.handleIframeMessage(event)
  })
})

// 清理资源
onUnmounted(() => {
  // 手动关闭 SSE 连接（EventSource 不会随组件卸载自动关闭）
  generationEventSource?.close()
  generationEventSource = null
  resumeEventSource?.close()
  resumeEventSource = null
  closeBuildWatch()
})
</script>

<style scoped>
#appChatPage {
  height: calc(100vh - var(--header-h, 72px));
  display: flex;
  flex-direction: column;
  padding: 12px 16px 16px;
  background: var(--bg-inset);
  overflow: hidden;
}

#appChatPage.is-dragging {
  cursor: col-resize;
  user-select: none;
}

/* 顶部栏 */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 4px 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.code-gen-type-tag {
  font-size: 12px;
  color: var(--brand-text);
  background: var(--brand-soft);
  border-color: var(--brand-soft);
}

.app-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-right {
  display: flex;
  gap: 12px;
}

/* 编辑模式激活态（顶栏按钮） */
.edit-mode-active {
  background-color: var(--success) !important;
  border-color: var(--success) !important;
  color: #fff !important;
}

.edit-mode-active:hover {
  background-color: color-mix(in srgb, var(--success) 80%, #fff) !important;
  border-color: color-mix(in srgb, var(--success) 80%, #fff) !important;
}

/* 主要内容区域：外围固定，两栏各自滚动 */
.main-content {
  flex: 1;
  min-height: 0;
  display: flex;
  padding: 0;
  overflow: hidden;
}

/* 左侧对话区域 */
.chat-section {
  width: var(--chat-w, 42%);
  flex: none;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* 拖拽分隔条 */
.layout-splitter {
  flex: none;
  width: 10px;
  cursor: col-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  touch-action: none;
}

.layout-splitter i {
  width: 2px;
  height: 36px;
  border-radius: 2px;
  background: var(--border-strong);
  opacity: 0.4;
  transition:
    height var(--dur-fast) var(--ease),
    background var(--dur-fast) var(--ease),
    opacity var(--dur-fast) var(--ease);
}

.layout-splitter:hover i,
.layout-splitter--dragging i {
  background: var(--brand);
  opacity: 1;
  height: 56px;
}

.messages-container {
  flex: 1;
  min-height: 0;
  padding: 16px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.message-item {
  margin-bottom: 12px;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 8px;
}

.ai-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 8px;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.5;
  word-wrap: break-word;
}

.user-message .message-content {
  background: var(--brand);
  color: var(--brand-on);
}

.ai-message .message-content {
  background: var(--bg-surface-muted);
  color: var(--text-primary);
  padding: 8px 12px;
}

.message-avatar {
  flex-shrink: 0;
}

/* AI 头像：品牌 logo 圆形底（完整显示，不裁切） */
.ai-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
}

/* 加载更多按钮 */
.load-more-container {
  text-align: center;
  padding: 8px 0;
  margin-bottom: 16px;
}

/* 输入区域 */
.input-container {
  padding: 16px;
  background: var(--bg-surface);
}

.input-wrapper {
  position: relative;
}

.input-wrapper .ant-input {
  padding-right: 50px;
}

.input-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
}

/* 右侧预览区域 */
.preview-section {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
}

.editing-tag {
  padding: 2px 10px;
  border-radius: var(--radius-full);
  background: var(--brand-soft);
  color: var(--brand-text);
  font-size: var(--text-micro);
  font-weight: var(--weight-medium);
}

.preview-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-secondary);
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-secondary);
}

.preview-loading p {
  margin-top: 16px;
}

/* 构建进行中 */
.preview-building {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-secondary);
  padding: 16px;
}

.build-status-text {
  margin-top: 16px;
  font-size: 14px;
}

.build-log-panel {
  margin-top: 12px;
  width: 100%;
  max-width: 560px;
  max-height: 180px;
  overflow-y: auto;
  background: var(--code-bg);
  color: var(--code-text);
  border-radius: 6px;
  padding: 10px 14px;
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.6;
  text-align: left;
}

.build-log-line {
  white-space: pre-wrap;
  word-break: break-all;
}

/* 构建失败 */
.preview-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-secondary);
  padding: 16px;
}

.error-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.error-text {
  color: var(--danger);
  font-size: 14px;
}

.error-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-muted);
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.selected-element-alert {
  margin: 0 16px;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
    overflow-y: auto;
  }

  .layout-splitter {
    display: none;
  }

  .chat-section,
  .preview-section {
    flex: none;
    width: 100%;
    height: 50vh;
  }
}

@media (max-width: 768px) {
  .header-bar {
    padding: 12px 16px;
  }

  .app-name {
    font-size: 16px;
  }

  .main-content {
    padding: 8px;
    gap: 8px;
  }

  .message-content {
    max-width: 85%;
  }

  /* 选中元素信息样式 */
  .selected-element-alert {
    margin: 0 16px;
  }

  .selected-element-info {
    line-height: 1.4;
  }

  .element-header {
    margin-bottom: 8px;
  }

  .element-details {
    margin-top: 8px;
  }

  .element-item {
    margin-bottom: 4px;
    font-size: 13px;
  }

  .element-item:last-child {
    margin-bottom: 0;
  }

  .element-tag {
    font-family: var(--font-mono);
    font-size: 14px;
    font-weight: 600;
    color: var(--info);
  }

  .element-id {
    color: var(--success);
    margin-left: 4px;
  }

  .element-class {
    color: var(--warning);
    margin-left: 4px;
  }

  .element-selector-code {
    font-family: var(--font-mono);
    background: var(--bg-inset);
    padding: 2px 4px;
    border-radius: 3px;
    font-size: 12px;
    color: var(--danger);
    border: 1px solid var(--border);
  }
}
</style>
