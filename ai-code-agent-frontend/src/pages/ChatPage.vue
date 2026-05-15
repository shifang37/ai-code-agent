<template>
  <div class="chat-page">
    <div class="chat-topbar">
      <div class="chat-topbar-left">
        <a-button type="text" @click="router.push('/')">
          <template #icon><ArrowLeftOutlined /></template>
        </a-button>
        <h2 class="chat-app-name">{{ appInfo?.appName || '加载中...' }}</h2>
      </div>
      <div class="chat-topbar-right">
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
          <div v-if="messages.length === 0 && !streaming" class="chat-empty">
            <a-spin />
            <p>正在初始化对话...</p>
          </div>
          <div
            v-for="(msg, idx) in messages"
            :key="idx"
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

        <div class="chat-input-area">
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

      <div class="chat-right">
        <div v-if="!previewReady" class="preview-placeholder">
          <AppstoreOutlined style="font-size: 48px; color: #d9d9d9" />
          <p>AI 代码生成完成后将在此展示效果</p>
        </div>
        <iframe
          v-else
          :src="previewUrl"
          class="preview-iframe"
          sandbox="allow-scripts allow-same-origin"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  RocketOutlined,
  ExpandOutlined,
  RobotOutlined,
  SendOutlined,
  AppstoreOutlined,
} from '@ant-design/icons-vue'
import { getAppVoById, deployApp } from '@/api/appController'
import type { AppVO } from '@/models'
import { useUserStore } from '@/stores/user'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

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
const inputText = ref('')
const streaming = ref(false)
const streamContent = ref('')
const messagesRef = ref<HTMLElement>()

let abortController: AbortController | null = null

onMounted(async () => {
  try {
    const res = await getAppVoById({ id: appId } as unknown as API.getAppVOByIdParams)
    if (res.data.code === 0) {
      appInfo.value = res.data.data as unknown as AppVO
      const prompt = appInfo.value?.initPrompt
      if (prompt) {
        await sendMessage(prompt)
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
  await sendMessage(text)
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
      previewUrl.value = `http://localhost:8123/api/static/${appInfo.value.codeGenType}_${appId}/`
    }

    scrollToBottom()
  }
}

async function handleDeploy() {
  deploying.value = true
  try {
    const res = await deployApp({ appId } as unknown as API.AppDeployRequest)
    if (res.data.code === 0) {
      message.success(`部署成功！访问地址：${res.data.data}`)
    } else {
      message.error(res.data.message || '部署失败')
    }
  } catch {
    message.error('部署失败')
  } finally {
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

.chat-input-area {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  align-items: flex-end;
}

.chat-input-area :deep(.ant-btn) {
  flex-shrink: 0;
  height: 40px;
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
