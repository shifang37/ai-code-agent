<template>
  <div class="home-page">
    <div class="hero-section">
      <h1 class="hero-title">AI 零代码应用生成平台</h1>
      <p class="hero-desc">输入你的想法，让 AI 帮你生成完整的网站应用</p>
      <div class="prompt-input-area">
        <a-textarea
          v-model:value="promptText"
          placeholder="请描述你想要创建的应用，例如：帮我生成一个在线商城..."
          :rows="3"
          :maxlength="500"
          class="prompt-textarea"
          @press-enter="handleCreateApp"
        />
        <a-button
          type="primary"
          size="large"
          :loading="creating"
          class="create-btn"
          @click="handleCreateApp"
        >
          <template #icon><ThunderboltOutlined /></template>
          开始生成应用
        </a-button>
      </div>
    </div>

    <div class="list-section">
      <a-card title="我的应用" class="app-list-card">
        <template #extra>
          <a-input-search
            v-model:value="mySearch"
            placeholder="搜索应用名称"
            style="width: 220px"
            @search="handleMySearch"
          />
        </template>
        <a-spin :spinning="myLoading">
          <a-row v-if="myApps.length" :gutter="[16, 16]">
            <a-col v-for="app in myApps" :key="app.id" :xs="24" :sm="12" :md="8" :lg="6">
              <AppCard :app="app" show-actions @click="goChat">
                <template #actions="{ app }">
                  <a-button size="small" type="text" @click="goEdit(app)">
                    <template #icon><EditOutlined /></template>
                  </a-button>
                  <a-popconfirm
                    title="确定要删除该应用吗？"
                    ok-text="确定"
                    cancel-text="取消"
                    @confirm="handleMyDelete(app.id)"
                  >
                    <a-button size="small" type="text" danger>
                      <template #icon><DeleteOutlined /></template>
                    </a-button>
                  </a-popconfirm>
                </template>
              </AppCard>
            </a-col>
          </a-row>
          <a-empty v-else description="暂无应用，快去创建一个吧" />
        </a-spin>
        <div v-if="myTotal > pageSize" class="list-pagination">
          <a-pagination
            v-model:current="myPage"
            :page-size="pageSize"
            :total="myTotal"
            size="small"
            @change="fetchMyApps"
          />
        </div>
      </a-card>

      <a-card title="精选应用" class="app-list-card">
        <template #extra>
          <a-input-search
            v-model:value="goodSearch"
            placeholder="搜索应用名称"
            style="width: 220px"
            @search="handleGoodSearch"
          />
        </template>
        <a-spin :spinning="goodLoading">
          <a-row v-if="goodApps.length" :gutter="[16, 16]">
            <a-col v-for="app in goodApps" :key="app.id" :xs="24" :sm="12" :md="8" :lg="6">
              <AppCard :app="app" @click="goChat" />
            </a-col>
          </a-row>
          <a-empty v-else description="暂无精选应用" />
        </a-spin>
        <div v-if="goodTotal > pageSize" class="list-pagination">
          <a-pagination
            v-model:current="goodPage"
            :page-size="pageSize"
            :total="goodTotal"
            size="small"
            @change="fetchGoodApps"
          />
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ThunderboltOutlined,
  EditOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'
import { addApp, listMyAppVoByPage, listGoodAppVoByPage, deleteApp, getAppVoById } from '@/api/appController'
import { CodeGenTypeText, type AppVO, type AppQueryRequest } from '@/models'
import { useUserStore } from '@/stores/user'
import AppCard from '@/components/AppCard.vue'

const router = useRouter()
const userStore = useUserStore()

const promptText = ref('')
const creating = ref(false)
const pageSize = 20

const myApps = ref<AppVO[]>([])
const myLoading = ref(false)
const myPage = ref(1)
const myTotal = ref(0)
const mySearch = ref('')

const goodApps = ref<AppVO[]>([])
const goodLoading = ref(false)
const goodPage = ref(1)
const goodTotal = ref(0)
const goodSearch = ref('')

async function handleCreateApp() {
  const text = promptText.value.trim()
  if (!text) {
    message.warning('请输入应用描述')
    return
  }
  if (!userStore.isLoggedIn) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }
  creating.value = true
  try {
    const res = await addApp({ initPrompt: text })
    if (res.data.code === 0 && res.data.data != null) {
      const appId = res.data.data
      try {
        const detail = await getAppVoById({ id: appId })
        const codeGenType = detail.data?.data?.codeGenType
        if (codeGenType) {
          const typeText = CodeGenTypeText[codeGenType] ?? codeGenType
          message.success(`AI 已为你选择「${typeText}」模式`)
        }
      } catch {
        // 取详情失败不阻塞跳转
      }
      router.push(`/app/chat/${appId}`)
    } else {
      message.error(res.data.message || '创建应用失败')
    }
  } catch {
    message.error('创建应用失败，请检查网络连接')
  } finally {
    creating.value = false
  }
}

async function fetchMyApps() {
  myLoading.value = true
  try {
    const params: AppQueryRequest = {
      pageNum: myPage.value,
      pageSize,
      sortField: 'createTime',
      sortOrder: 'descend',
    }
    if (mySearch.value.trim()) {
      params.appName = mySearch.value.trim()
    }
    const res = await listMyAppVoByPage(params)
    if (res.data.code === 0 && res.data.data) {
      myApps.value = (res.data.data.records || []) as unknown as AppVO[]
      myTotal.value = res.data.data.totalRow || 0
    }
  } catch {
    // ignore
  } finally {
    myLoading.value = false
  }
}

function handleMySearch() {
  myPage.value = 1
  fetchMyApps()
}

async function fetchGoodApps() {
  goodLoading.value = true
  try {
    const params: AppQueryRequest = {
      pageNum: goodPage.value,
      pageSize,
      sortField: 'createTime',
      sortOrder: 'descend',
    }
    if (goodSearch.value.trim()) {
      params.appName = goodSearch.value.trim()
    }
    const res = await listGoodAppVoByPage(params)
    if (res.data.code === 0 && res.data.data) {
      goodApps.value = (res.data.data.records || []) as unknown as AppVO[]
      goodTotal.value = res.data.data.totalRow || 0
    }
  } catch {
    // ignore
  } finally {
    goodLoading.value = false
  }
}

function handleGoodSearch() {
  goodPage.value = 1
  fetchGoodApps()
}

function goChat(app: AppVO) {
  router.push(`/app/chat/${app.id}`)
}

function goEdit(app: AppVO) {
  router.push(`/app/edit/${app.id}`)
}

async function handleMyDelete(id: string) {
  try {
    const res = await deleteApp({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      fetchMyApps()
    } else {
      message.error(res.data.message || '删除失败')
    }
  } catch {
    message.error('删除失败')
  }
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    fetchMyApps()
  }
  fetchGoodApps()
})
</script>

<style scoped>
.home-page {
  margin: -24px;
}

.hero-section {
  text-align: center;
  padding: 48px 32px 40px;
  background: linear-gradient(135deg, #f0f5ff 0%, #e6f7ff 50%, #f0f5ff 100%);
  border-radius: 0 0 24px 24px;
}

.hero-title {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px;
}

.hero-desc {
  font-size: 16px;
  color: #666;
  margin: 0 0 28px;
}

.prompt-input-area {
  max-width: 640px;
  margin: 0 auto;
}

.prompt-textarea {
  border-radius: 12px;
  margin-bottom: 16px;
}

.create-btn {
  border-radius: 8px;
  padding: 0 32px;
  height: 44px;
  font-size: 16px;
}

.list-section {
  padding: 24px 32px;
}

.app-list-card {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
}

.list-pagination {
  margin-top: 16px;
  text-align: center;
}
</style>
