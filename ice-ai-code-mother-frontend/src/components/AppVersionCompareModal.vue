<template>
  <a-modal
    v-model:open="visible"
    title="版本对比"
    :footer="null"
    width="92vw"
    class="version-compare-modal"
  >
    <div class="compare-toolbar">
      <a-space wrap>
        <a-select
          v-model:value="oldVersionKey"
          class="version-select"
          placeholder="选择旧版本"
          :options="versionOptions"
          @change="loadCompare"
        />
        <SwapOutlined class="swap-icon" />
        <a-select
          v-model:value="newVersionKey"
          class="version-select"
          placeholder="选择新版本"
          :options="versionOptions"
          @change="loadCompare"
        />
        <a-select
          v-model:value="selectedFile"
          class="file-select"
          placeholder="选择文件"
          :options="fileOptions"
          @change="loadCompare"
        />
      </a-space>
      <a-space class="diff-summary">
        <span class="removal-count">{{ compareResult?.removals || 0 }} removals</span>
        <span class="addition-count">{{ compareResult?.additions || 0 }} additions</span>
      </a-space>
    </div>

    <a-empty v-if="!versions.length && !loading" description="暂无历史版本，请至少重新生成一次应用" />

    <a-spin v-else :spinning="loading">
      <div class="diff-board">
        <div class="diff-pane">
          <div class="pane-header">
            <span>{{ oldLines.length }} lines</span>
            <a-button type="link" size="small" @click="copyContent(oldContent)">Copy</a-button>
          </div>
          <div class="code-list">
            <div
              v-for="(line, index) in oldRows"
              :key="`old-${index}`"
              class="code-line"
              :class="line.type"
            >
              <span class="line-no">{{ line.no || '' }}</span>
              <pre>{{ line.text }}</pre>
            </div>
          </div>
        </div>

        <div class="diff-pane">
          <div class="pane-header">
            <span>{{ newLines.length }} lines</span>
            <a-button type="link" size="small" @click="copyContent(newContent)">Copy</a-button>
          </div>
          <div class="code-list">
            <div
              v-for="(line, index) in newRows"
              :key="`new-${index}`"
              class="code-line"
              :class="line.type"
            >
              <span class="line-no">{{ line.no || '' }}</span>
              <pre>{{ line.text }}</pre>
            </div>
          </div>
        </div>
      </div>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { SwapOutlined } from '@ant-design/icons-vue'
import { compareAppVersion, listAppVersions } from '@/api/appController'

interface Props {
  open: boolean
  appId?: number
}

interface Emits {
  (e: 'update:open', value: boolean): void
}

type DiffType = 'same' | 'removed' | 'added' | 'empty'

interface DiffLine {
  no?: number
  text: string
  type: DiffType
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
})

const loading = ref(false)
const versions = ref<API.AppVersionVO[]>([])
const compareResult = ref<API.AppVersionCompareVO>()
const oldVersionKey = ref<string>()
const newVersionKey = ref<string>()
const selectedFile = ref<string>()

const oldContent = computed(() => compareResult.value?.oldContent || '')
const newContent = computed(() => compareResult.value?.newContent || '')
const oldLines = computed(() => splitLines(oldContent.value))
const newLines = computed(() => splitLines(newContent.value))
const diffRows = computed(() => buildDiffRows(oldLines.value, newLines.value))
const oldRows = computed(() => diffRows.value.oldRows)
const newRows = computed(() => diffRows.value.newRows)

const versionOptions = computed(() =>
  versions.value.map((version) => ({
    label: version.versionName || version.versionKey,
    value: version.versionKey,
  })),
)

const fileOptions = computed(() =>
  (compareResult.value?.fileList || []).map((file) => ({
    label: file,
    value: file,
  })),
)

watch(
  () => props.open,
  (open) => {
    if (open) {
      loadVersions()
    }
  },
)

const loadVersions = async () => {
  if (!props.appId) {
    message.error('应用 ID 不存在')
    return
  }
  loading.value = true
  try {
    const res = await listAppVersions({ appId: props.appId })
    if (res.data.code !== 0 || !res.data.data) {
      message.error('加载版本列表失败：' + res.data.message)
      return
    }
    versions.value = res.data.data
    const currentVersion = versions.value.find((version) => version.current)
    const historyVersion = versions.value.find((version) => !version.current)
    newVersionKey.value = currentVersion?.versionKey
    oldVersionKey.value = historyVersion?.versionKey || currentVersion?.versionKey
    if (oldVersionKey.value && newVersionKey.value) {
      await loadCompare()
    }
  } catch (error) {
    console.error('加载版本列表失败：', error)
    message.error('加载版本列表失败')
  } finally {
    loading.value = false
  }
}

const loadCompare = async () => {
  if (!props.appId || !oldVersionKey.value || !newVersionKey.value) {
    return
  }
  loading.value = true
  try {
    const res = await compareAppVersion({
      appId: props.appId,
      oldVersionKey: oldVersionKey.value,
      newVersionKey: newVersionKey.value,
      filePath: selectedFile.value,
    })
    if (res.data.code !== 0 || !res.data.data) {
      message.error('加载版本对比失败：' + res.data.message)
      return
    }
    compareResult.value = res.data.data
    selectedFile.value = res.data.data.filePath
  } catch (error) {
    console.error('加载版本对比失败：', error)
    message.error('加载版本对比失败')
  } finally {
    loading.value = false
  }
}

const splitLines = (content: string) => {
  return content ? content.split(/\r?\n/) : []
}

const buildDiffRows = (oldLineList: string[], newLineList: string[]) => {
  const oldLength = oldLineList.length
  const newLength = newLineList.length
  const dp = Array.from({ length: oldLength + 1 }, () => Array(newLength + 1).fill(0))

  for (let i = oldLength - 1; i >= 0; i--) {
    for (let j = newLength - 1; j >= 0; j--) {
      dp[i][j] =
        oldLineList[i] === newLineList[j]
          ? dp[i + 1][j + 1] + 1
          : Math.max(dp[i + 1][j], dp[i][j + 1])
    }
  }

  const oldRows: DiffLine[] = []
  const newRows: DiffLine[] = []
  let i = 0
  let j = 0
  while (i < oldLength || j < newLength) {
    if (i < oldLength && j < newLength && oldLineList[i] === newLineList[j]) {
      oldRows.push({ no: i + 1, text: oldLineList[i], type: 'same' })
      newRows.push({ no: j + 1, text: newLineList[j], type: 'same' })
      i++
      j++
    } else if (j >= newLength || (i < oldLength && dp[i + 1][j] >= dp[i][j + 1])) {
      oldRows.push({ no: i + 1, text: oldLineList[i], type: 'removed' })
      newRows.push({ text: '', type: 'empty' })
      i++
    } else {
      oldRows.push({ text: '', type: 'empty' })
      newRows.push({ no: j + 1, text: newLineList[j], type: 'added' })
      j++
    }
  }
  return { oldRows, newRows }
}

const copyContent = async (content: string) => {
  await navigator.clipboard.writeText(content)
  message.success('已复制')
}
</script>

<style scoped>
.compare-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.version-select {
  width: 180px;
}

.file-select {
  width: 260px;
}

.swap-icon {
  color: #8c8c8c;
}

.diff-summary {
  font-weight: 600;
}

.removal-count {
  color: #a8071a;
}

.addition-count {
  color: #237804;
}

.diff-board {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  min-height: 520px;
}

.diff-pane {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.pane-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 44px;
  padding: 0 16px;
  border-bottom: 1px solid #f0f0f0;
  color: #5f6b7a;
  font-weight: 600;
}

.code-list {
  height: 520px;
  overflow: auto;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 13px;
}

.code-line {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  min-height: 28px;
  line-height: 28px;
}

.line-no {
  padding-right: 12px;
  text-align: right;
  color: #8c8c8c;
  user-select: none;
}

pre {
  margin: 0;
  padding: 0 12px;
  white-space: pre;
}

.removed {
  background: #fff1f0;
}

.added {
  background: #f6ffed;
}

.empty {
  background: #fafafa;
}

@media (max-width: 900px) {
  .compare-toolbar,
  .diff-board {
    display: flex;
    flex-direction: column;
  }

  .version-select,
  .file-select {
    width: 100%;
  }
}
</style>
