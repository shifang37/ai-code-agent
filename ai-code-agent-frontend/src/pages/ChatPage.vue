<template>
  <div class="chat-page">
    <div class="chat-topbar">
      <div class="chat-topbar-left">
        <a-button type="text" @click="router.push('/')">
          <template #icon><ArrowLeftOutlined /></template>
        </a-button>
        <h2 class="chat-app-name">{{ appInfo?.appName || '加载中...' }}</h2>
        <a-tag v-if="codeGenTypeLabel" color="blue" class="chat-codegen-tag">
          {{ codeGenTypeLabel }}
        </a-tag>
      </div>
      <div class="chat-topbar-right">
        <a-button
          v-if="canDownload"
          style="margin-right: 8px"
          @click="handleDownload"
        >
          <template #icon><DownloadOutlined /></template>
          下载代码
        </a-button>
        <a-button
          v-if="previewReady"
          type="primary"
          :loading="deploying"
          @click="handleDeploy"
        >
          <template #icon><RocketOutlined /></template>
          部署应用
        </a-button>
        <a-button v-if="previewReady" style="margin-left: 8px" @click="openPreview">
          <template #icon><ExpandOutlined /></template>
          新窗口预览
        </a-button>
      </div>
    </div>

    <div class="chat-main">
      <div class="chat-left">
        <div class="chat-messages" ref="messagesRef">
          <div v-if="historyMessages.length === 0 && messages.length === 0 && !streaming" class="chat-empty">
            <a-spin />
            <p>正在初始化对话...</p>
          </div>
          <div v-if="hasMoreHistory" class="load-more-area">
            <a-button :loading="loadingHistory" @click="loadChatHistory">
              加载更多历史消息
            </a-button>
          </div>
          <div
            v-for="(msg, idx) in historyMessages"
            :key="'hist-' + idx"
            :class="['chat-message', msg.role === 'user' ? 'chat-message-user' : 'chat-message-ai']"
          >
            <div class="chat-message-avatar">
              <a-avatar v-if="msg.role === 'user'" :size="32">
                {{ (userStore.loginUser?.userName || '用')[0] }}
              </a-avatar>
              <a-avatar v-else :size="32" style="background: #1677ff">
                <RobotOutlined />
              </a-avatar>
            </div>
            <div class="chat-message-bubble">
              <MarkdownRenderer :content="msg.content" />
            </div>
          </div>
          <div
            v-for="(msg, idx) in messages"
            :key="'msg-' + idx"
            :class="['chat-message', msg.role === 'user' ? 'chat-message-user' : 'chat-message-ai']"
          >
            <div class="chat-message-avatar">
              <a-avatar v-if="msg.role === 'user'" :size="32">
                {{ (userStore.loginUser?.userName || '用')[0] }}
              </a-avatar>
              <a-avatar v-else :size="32" style="background: #1677ff">
                <RobotOutlined />
              </a-avatar>
            </div>
            <div class="chat-message-bubble">
              <MarkdownRenderer :content="msg.content" />
            </div>
          </div>
          <div v-if="streaming" class="chat-message chat-message-ai">
            <div class="chat-message-avatar">
              <a-avatar :size="32" style="background: #1677ff">
                <RobotOutlined />
              </a-avatar>
            </div>
            <div class="chat-message-bubble">
              <MarkdownRenderer :content="streamContent" />
              <span class="streaming-cursor">|</span>
            </div>
          </div>
        </div>

        <div class="chat-input-wrapper">
          <a-alert
            v-if="selectedElement"
            class="chat-selected-alert"
            type="info"
            show-icon
            closable
            @close="clearSelection"
          >
            <template #message>
              已选中元素：
              <strong>{{ selectedElement.tagName }}</strong>
              <span v-if="selectedElement.id"> #{{ selectedElement.id }}</span>
              <span v-else-if="selectedElement.className"> .{{ selectedElement.className.split(' ')[0] }}</span>
              <span v-if="selectedElement.textContent" class="chat-selected-text">
                "{{ selectedElement.textContent }}"
              </span>
            </template>
          </a-alert>
          <div class="chat-input-area">
            <a-tooltip :title="editMode ? '退出编辑模式' : '进入可视化编辑模式'">
              <a-button
                :type="editMode ? 'primary' : 'default'"
                :disabled="!previewReady || streaming || !initialized"
                @click="toggleEditMode"
              >
                <template #icon><EditOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-textarea
              v-model:value="inputText"
              placeholder="输入你的需求，让 AI 帮你调整应用..."
              :rows="2"
              :disabled="streaming || !initialized"
              @press-enter="handleSend"
            />
            <a-button
              type="primary"
              :loading="streaming"
              :disabled="!inputText.trim() || streaming || !initialized"
              @click="handleSend"
            >
              <template #icon><SendOutlined /></template>
              发送
            </a-button>
          </div>
        </div>
      </div>

      <div class="chat-right">
        <div v-if="!previewReady" class="preview-placeholder">
          <AppstoreOutlined style="font-size: 48px; color: #d9d9d9" />
          <p>AI 代码生成完成后将在此展示效果</p>
        </div>
        <iframe
          v-else
          ref="previewIframeRef"
          :src="previewUrl"
          class="preview-iframe"
          :class="{ 'preview-iframe-edit': editMode }"
          sandbox="allow-scripts allow-same-origin"
          @load="handleIframeLoad"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  RocketOutlined,
  ExpandOutlined,
  RobotOutlined,
  SendOutlined,
  AppstoreOutlined,
  DownloadOutlined,
  EditOutlined,
} from '@ant-design/icons-vue'
import { getAppVoById, deployApp } from '@/api/appController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import { CodeGenTypeEnum, CodeGenTypeText, type AppVO } from '@/models'
import { useUserStore } from '@/stores/user'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { useVisualEditor, formatElementForPrompt } from '@/utils/visualEditor'

interface ChatMessage {
  role: 'user' | 'ai'
  content: string
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const appId = route.params.appId as string
const appInfo = ref<AppVO | null>(null)
const initialized = ref(false)
const previewReady = ref(false)
const previewUrl = ref('')
const deploying = ref(false)

const messages = ref<ChatMessage[]>([])
const historyMessages = ref<ChatMessage[]>([])
const hasMoreHistory = ref(false)
const loadingHistory = ref(false)
const lastCreateTime = ref<string | undefined>(undefined)
const inputText = ref('')
const streaming = ref(false)
const streamContent = ref('')
const messagesRef = ref<HTMLElement>()
const previewIframeRef = ref<HTMLIFrameElement>()

const {
  editMode,
  selectedElement,
  enable: enableEditMode,
  disable: disableEditMode,
  clearSelection,
  dispose: disposeVisualEditor,
} = useVisualEditor(previewIframeRef)

function toggleEditMode() {
  if (editMode.value) {
    disableEditMode()
  } else {
    enableEditMode()
  }
}

function handleIframeLoad() {
  if (editMode.value) {
    enableEditMode()
  }
}

let abortController: AbortController | null = null

const codeGenTypeLabel = computed(() => {
  const type = appInfo.value?.codeGenType
  return type ? CodeGenTypeText[type] : ''
})

const canDownload = computed(() => {
  if (!appInfo.value || !userStore.loginUser) return false
  return String(appInfo.value.userId) === String(userStore.loginUser.id)
})

function handleDownload() {
  const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api'
  const a = document.createElement('a')
  a.href = `${baseURL}/app/download/${appId}`
  a.target = '_blank'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

async function loadChatHistory() {
  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId,
      pageSize: 10,
    }
    if (lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }
    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const page = res.data.data
      const records = page.records || []
      if (records.length > 0) {
        // API returns DESC order, reverse to ASC for display
        const newMessages: ChatMessage[] = [...records]
          .reverse()
          .map((r: API.ChatHistory) => ({
            role: (r.messageType === 'user' ? 'user' : 'ai') as 'user' | 'ai',
            content: r.message || '',
          }))
        historyMessages.value = [...newMessages, ...historyMessages.value]
        // Set cursor to the oldest record's createTime for next page
        lastCreateTime.value = records[records.length - 1].createTime
      }
      const totalRow = Number(page.totalRow) || 0
      hasMoreHistory.value = historyMessages.value.length < totalRow
    }
  } catch {
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getAppVoById({ id: appId })
    if (res.data.code === 0) {
      appInfo.value = res.data.data as unknown as AppVO

      await loadChatHistory()

      // Show preview if app has at least 2 chat history records
      if (historyMessages.value.length >= 2 && appInfo.value?.codeGenType) {
        previewReady.value = true
        previewUrl.value = `/api/static/${appInfo.value.codeGenType}_${appId}/`
      }

      // Auto-send initPrompt only if own app and no chat history
      const isOwnApp = String(appInfo.value?.userId) === String(userStore.loginUser?.id)
      if (isOwnApp && historyMessages.value.length === 0) {
        const prompt = appInfo.value?.initPrompt
        if (prompt) {
          await sendMessage(prompt)
        }
      }
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch {
    message.error('获取应用信息失败')
    router.push('/')
  } finally {
    initialized.value = true
  }
})

onUnmounted(() => {
  if (abortController) {
    abortController.abort()
  }
  disposeVisualEditor()
})

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || streaming.value) return
  inputText.value = ''
  let finalText = text
  if (selectedElement.value) {
    finalText = `${text}\n\n${formatElementForPrompt(selectedElement.value)}`
  }
  if (editMode.value) {
    disableEditMode()
  } else {
    clearSelection()
  }
  await sendMessage(finalText)
}

async function sendMessage(text: string) {
  messages.value.push({ role: 'user', content: text })
  scrollToBottom()

  streaming.value = true
  streamContent.value = ''
  abortController = new AbortController()

  try {
    const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api'
    const url = `${baseURL}/app/chat/gen/code?appId=${appId}&message=${encodeURIComponent(text)}`

    const response = await fetch(url, {
      method: 'GET',
      headers: {
        Accept: 'text/event-stream',
      },
      credentials: 'include',
      signal: abortController.signal,
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('无法读取流式响应')
    }

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (!data) continue
          try {
            const parsed = JSON.parse(data)
            if (parsed.d) {
              streamContent.value += parsed.d
            }
          } catch {
            streamContent.value += data
          }
          scrollToBottom()
        }
      }
    }
  } catch (err: any) {
    if (err?.name !== 'AbortError') {
      message.error('对话连接中断')
    }
  } finally {
    if (streamContent.value) {
      messages.value.push({ role: 'ai', content: streamContent.value })
    }
    streamContent.value = ''
    streaming.value = false
    abortController = null

    if (appInfo.value?.codeGenType) {
      previewReady.value = true
      previewUrl.value = `/api/static/${appInfo.value.codeGenType}_${appId}/`
    }

    scrollToBottom()
  }
}

async function handleDeploy() {
  deploying.value = true
  const isVueProject = appInfo.value?.codeGenType === CodeGenTypeEnum.VUE_PROJECT
  if (isVueProject) {
    message.loading({
      content: 'Vue 工程构建中，可能需要 30 秒以上，请耐心等待...',
      key: 'deploy-loading',
      duration: 0,
    })
  }
  try {
    const res = await deployApp({ appId })
    if (res.data.code === 0) {
      message.success(`部署成功！访问地址：${res.data.data}`)
    } else {
      message.error(res.data.message || '部署失败')
    }
  } catch {
    message.error('部署失败')
  } finally {
    if (isVueProject) {
      message.destroy('deploy-loading')
    }
    deploying.value = false
  }
}

function openPreview() {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

watch(streamContent, () => {
  scrollToBottom()
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px);
  margin: -24px;
}

.chat-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.chat-topbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-app-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.chat-codegen-tag {
  margin-left: 4px;
}

.chat-topbar-right {
  display: flex;
  align-items: center;
}

.chat-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.chat-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  border-right: 1px solid #f0f0f0;
  background: #fafafa;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.load-more-area {
  text-align: center;
  padding: 12px 0;
}

.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.chat-message-user {
  flex-direction: row-reverse;
}

.chat-message-bubble {
  max-width: 75%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.chat-message-user .chat-message-bubble {
  background: #1677ff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.chat-message-ai .chat-message-bubble {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-bottom-left-radius: 4px;
}

.streaming-cursor {
  animation: blink 0.8s infinite;
  font-weight: bold;
  color: #1677ff;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.chat-input-wrapper {
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

.chat-selected-alert {
  margin: 8px 16px 0;
}

.chat-selected-text {
  margin-left: 8px;
  color: #666;
  font-style: italic;
}

.chat-input-area {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  align-items: flex-end;
}

.chat-input-area :deep(.ant-btn) {
  flex-shrink: 0;
  height: 40px;
}

.preview-iframe-edit {
  outline: 2px solid #1677ff;
  outline-offset: -2px;
}

.chat-right {
  width: 45%;
  flex-shrink: 0;
  background: #fff;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  gap: 12px;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
