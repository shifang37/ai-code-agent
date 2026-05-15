<template>
  <div class="admin-page">
    <PageHeader title="应用管理" description="管理系统中的所有应用，支持搜索、编辑、删除和精选操作" />

    <a-card class="search-card">
      <a-form layout="inline" :model="queryParams" @finish="handleSearch">
        <a-form-item label="应用名称">
          <a-input
            v-model:value="queryParams.appName"
            placeholder="搜索应用名称"
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
          <template v-if="column.key === 'cover'">
            <a-avatar v-if="record.cover" :src="record.cover" shape="square" :size="48" />
            <AppstoreOutlined v-else style="font-size: 36px; color: #d9d9d9" />
          </template>
          <template v-if="column.key === 'priority'">
            <a-tag :color="record.priority >= 99 ? 'gold' : 'default'">
              {{ record.priority >= 99 ? '精选' : record.priority }}
            </a-tag>
          </template>
          <template v-if="column.key === 'createTime'">
            {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm') }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
              <a-button
                v-if="record.priority < 99"
                type="link"
                size="small"
                @click="handleFeature(record)"
              >
                精选
              </a-button>
              <a-popconfirm
                title="确定要删除该应用吗？"
                description="删除后不可恢复"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  AppstoreOutlined,
} from '@ant-design/icons-vue'
import { listAppVoByPageByAdmin, deleteAppByAdmin, updateAppByAdmin } from '@/api/appController'
import type { AppVO, AppQueryRequest } from '@/models'
import PageHeader from '@/components/PageHeader.vue'
import dayjs from 'dayjs'

const router = useRouter()

const loading = ref(false)
const tableData = ref<AppVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const queryParams = reactive<AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
})

const columns: TableColumnsType = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80, ellipsis: true },
  { title: '封面', key: 'cover', width: 80 },
  { title: '应用名称', dataIndex: 'appName', key: 'appName', width: 160 },
  { title: '生成类型', dataIndex: 'codeGenType', key: 'codeGenType', width: 100 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 80 },
  { title: '用户 ID', dataIndex: 'userId', key: 'userId', width: 80 },
  { title: '创建时间', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' },
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

async function fetchData() {
  loading.value = true
  try {
    const params: AppQueryRequest = {
      ...queryParams,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    }
    const res = await listAppVoByPageByAdmin(params)
    if (res.data.code === 0 && res.data.data) {
      const page = res.data.data
      tableData.value = (page.records || []) as unknown as AppVO[]
      total.value = Number(page.totalRow) || 0
      pagination.current = Number(page.pageNumber) || 1
      pagination.total = Number(page.totalRow) || 0
    } else {
      message.error(res.data.message || '获取应用列表失败')
    }
  } catch {
    message.error('获取应用列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchData()
}

function handleReset() {
  queryParams.appName = undefined
  queryParams.userId = undefined
  currentPage.value = 1
  fetchData()
}

function handleTableChange(pag: TablePaginationConfig) {
  currentPage.value = pag.current || 1
  pageSize.value = pag.pageSize || 10
  fetchData()
}

function openEdit(record: AppVO) {
  router.push(`/app/edit/${record.id}`)
}

async function handleFeature(record: AppVO) {
  try {
    const res = await updateAppByAdmin({
      id: record.id as unknown as number,
      priority: 99,
    })
    if (res.data.code === 0) {
      message.success('已设为精选')
      fetchData()
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch {
    message.error('操作失败')
  }
}

async function handleDelete(id: string) {
  try {
    const res = await deleteAppByAdmin({ id: id as unknown as number })
    if (res.data.code === 0) {
      message.success('删除成功')
      fetchData()
    } else {
      message.error(res.data.message || '删除失败')
    }
  } catch {
    message.error('删除失败')
  }
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
</style>
