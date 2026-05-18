<template>
  <div class="admin-page">
    <PageHeader title="对话管理" description="管理系统中的所有对话历史，支持按应用、用户和消息类型筛选" />

    <a-card class="search-card">
      <a-form layout="inline" :model="queryParams" @finish="handleSearch">
        <a-form-item label="应用 ID">
          <a-input
            v-model:value="queryParams.appId"
            placeholder="搜索应用 ID"
            allow-clear
            @press-enter="handleSearch"
          />
        </a-form-item>
        <a-form-item label="用户 ID">
          <a-input
            v-model:value="queryParams.userId"
            placeholder="搜索用户 ID"
            allow-clear
            @press-enter="handleSearch"
          />
        </a-form-item>
        <a-form-item label="消息类型">
          <a-select
            v-model:value="queryParams.messageType"
            placeholder="选择消息类型"
            allow-clear
            style="width: 140px"
          >
            <a-select-option value="user">用户</a-select-option>
            <a-select-option value="ai">AI</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">
              <template #icon><SearchOutlined /></template>
              搜索
            </a-button>
            <a-button @click="handleReset">
              <template #icon><ReloadOutlined /></template>
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'messageType'">
            <a-tag :color="record.messageType === 'user' ? 'blue' : 'green'">
              {{ record.messageType === 'user' ? '用户' : 'AI' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'message'">
            <span class="message-ellipsis">{{ record.message }}</span>
          </template>
          <template v-if="column.key === 'createTime'">
            {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record)">查看</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="detailVisible" title="消息详情" :footer="null" width="600px">
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="ID">{{ detailRecord?.id }}</a-descriptions-item>
        <a-descriptions-item label="应用 ID">{{ detailRecord?.appId }}</a-descriptions-item>
        <a-descriptions-item label="用户 ID">{{ detailRecord?.userId }}</a-descriptions-item>
        <a-descriptions-item label="消息类型">
          <a-tag :color="detailRecord?.messageType === 'user' ? 'blue' : 'green'">
            {{ detailRecord?.messageType === 'user' ? '用户' : 'AI' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">
          {{ detailRecord?.createTime ? dayjs(detailRecord.createTime).format('YYYY-MM-DD HH:mm:ss') : '' }}
        </a-descriptions-item>
        <a-descriptions-item label="消息内容">
          <div style="max-height: 300px; overflow-y: auto; white-space: pre-wrap; word-break: break-word;">
            {{ detailRecord?.message }}
          </div>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
} from '@ant-design/icons-vue'
import { listAllChatHistoryByPageForAdmin } from '@/api/chatHistoryController'
import PageHeader from '@/components/PageHeader.vue'
import dayjs from 'dayjs'

const loading = ref(false)
const tableData = ref<API.ChatHistory[]>([])
const currentPage = ref(1)
const pageSize = ref(10)

const queryParams = reactive<API.ChatHistoryQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
})

const columns: TableColumnsType = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 100, ellipsis: true },
  { title: '应用 ID', dataIndex: 'appId', key: 'appId', width: 100 },
  { title: '用户 ID', dataIndex: 'userId', key: 'userId', width: 100 },
  { title: '消息类型', key: 'messageType', width: 100 },
  { title: '消息内容', key: 'message', ellipsis: true },
  { title: '创建时间', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' },
]

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  pageSizeOptions: ['10', '20', '50'],
  showTotal: (t: number) => `共 ${t} 条`,
})

const detailVisible = ref(false)
const detailRecord = ref<API.ChatHistory | null>(null)

async function fetchData() {
  loading.value = true
  try {
    const params: API.ChatHistoryQueryRequest = {
      ...queryParams,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    }
    const res = await listAllChatHistoryByPageForAdmin(params)
    if (res.data.code === 0 && res.data.data) {
      const page = res.data.data
      tableData.value = (page.records || []) as unknown as API.ChatHistory[]
      pagination.current = Number(page.pageNumber) || 1
      pagination.total = Number(page.totalRow) || 0
    } else {
      message.error(res.data.message || '获取对话历史列表失败')
    }
  } catch {
    message.error('获取对话历史列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchData()
}

function handleReset() {
  queryParams.appId = undefined
  queryParams.userId = undefined
  queryParams.messageType = undefined
  currentPage.value = 1
  fetchData()
}

function handleTableChange(pag: TablePaginationConfig) {
  currentPage.value = pag.current || 1
  pageSize.value = pag.pageSize || 10
  fetchData()
}

function openDetail(record: API.ChatHistory) {
  detailRecord.value = record
  detailVisible.value = true
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.admin-page {
  margin: -24px;
}

.search-card {
  margin: 20px 32px 16px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.search-card :deep(.ant-card-body) {
  padding: 20px 24px 4px;
}

.search-card :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.table-card {
  margin: 0 32px 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.message-ellipsis {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
