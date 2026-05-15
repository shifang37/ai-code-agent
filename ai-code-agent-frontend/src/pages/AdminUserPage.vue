<template>
  <div class="admin-page">
    <PageHeader title="用户管理" description="管理系统中的所有用户，支持搜索、新增、编辑和删除操作">
      <template #extra>
        <a-button type="primary" size="large" @click="openAddModal">
          <template #icon><PlusOutlined /></template>
          新增用户
        </a-button>
      </template>
    </PageHeader>

    <!-- Search Form -->
    <a-card class="search-card">
      <a-form layout="inline" :model="queryParams" @finish="handleSearch">
        <a-form-item label="账号">
          <a-input
            v-model:value="queryParams.userAccount"
            placeholder="搜索账号"
            allow-clear
            @press-enter="handleSearch"
          />
        </a-form-item>
        <a-form-item label="用户名">
          <a-input
            v-model:value="queryParams.userName"
            placeholder="搜索用户名"
            allow-clear
            @press-enter="handleSearch"
          />
        </a-form-item>
        <a-form-item label="角色">
          <a-select
            v-model:value="queryParams.userRole"
            placeholder="选择角色"
            allow-clear
            style="width: 140px"
          >
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
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

    <!-- Data Table -->
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
          <template v-if="column.key === 'userRole'">
            <a-tag :color="record.userRole === 'admin' ? 'blue' : 'default'">
              {{ record.userRole === 'admin' ? '管理员' : '普通用户' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'createTime'">
            {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm') }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
              <a-popconfirm
                title="确定要删除该用户吗？"
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

    <!-- Add/Edit Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="modalSubmitting"
      width="520px"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="modalFormRef"
        :model="modalForm"
        :rules="modalRules"
        layout="vertical"
        class="modal-form"
      >
        <a-form-item v-if="isEditMode" label="用户 ID">
          <a-input :value="modalForm.id" disabled />
        </a-form-item>
        <a-form-item name="userAccount" label="账号">
          <a-input
            v-model:value="modalForm.userAccount"
            placeholder="请输入账号（至少4位）"
            :disabled="isEditMode"
            allow-clear
          />
        </a-form-item>
        <a-form-item name="userName" label="用户名">
          <a-input v-model:value="modalForm.userName" placeholder="请输入用户名" allow-clear />
        </a-form-item>
        <a-form-item name="userRole" label="角色">
          <a-select v-model:value="modalForm.userRole" placeholder="请选择角色">
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="userAvatar" label="头像 URL">
          <a-input v-model:value="modalForm.userAvatar" placeholder="请输入头像 URL（可选）" allow-clear />
        </a-form-item>
        <a-form-item name="userProfile" label="个人简介">
          <a-textarea
            v-model:value="modalForm.userProfile"
            placeholder="请输入个人简介（可选）"
            :rows="3"
            allow-clear
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import { PlusOutlined, SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { listUserVoByPage, addUser, updateUser, deleteUser } from '@/api/userController'
import type { UserVO, UserQueryRequest, UserAddRequest, UserUpdateRequest } from '@/models'
import PageHeader from '@/components/PageHeader.vue'
import dayjs from 'dayjs'

const loading = ref(false)
const tableData = ref<UserVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const queryParams = reactive<UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
})

const columns: TableColumnsType = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 100, ellipsis: true },
  { title: '账号', dataIndex: 'userAccount', key: 'userAccount', width: 140 },
  { title: '用户名', dataIndex: 'userName', key: 'userName', width: 120 },
  { title: '角色', key: 'userRole', width: 100 },
  { title: '个人简介', dataIndex: 'userProfile', key: 'userProfile', ellipsis: true },
  { title: '注册时间', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' },
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
    const res = await listUserVoByPage({ ...queryParams, pageNum: currentPage.value, pageSize: pageSize.value })
    if (res.data.code === 0 && res.data.data) {
      const page = res.data.data
      tableData.value = page.records
      total.value = Number(page.totalRow) || 0
      pagination.current = Number(page.pageNumber) || 1
      pagination.total = Number(page.totalRow) || 0
    } else {
      message.error(res.data.message || '获取用户列表失败')
    }
  } catch {
    message.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchData()
}

function handleReset() {
  queryParams.userAccount = undefined
  queryParams.userName = undefined
  queryParams.userRole = undefined
  currentPage.value = 1
  fetchData()
}

function handleTableChange(pag: TablePaginationConfig) {
  currentPage.value = pag.current || 1
  pageSize.value = pag.pageSize || 10
  fetchData()
}

// ===== Modal Logic =====

const modalVisible = ref(false)
const modalSubmitting = ref(false)
const modalFormRef = ref<FormInstance>()
const isEditMode = ref(false)

const modalForm = reactive<UserUpdateRequest & { userAccount: string }>({
  id: '',
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
})

const modalTitle = ref('新增用户')

const modalRules: Record<string, Rule[]> = {
  userAccount: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, message: '账号至少4位', trigger: 'blur' },
  ],
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  userRole: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

function openAddModal() {
  isEditMode.value = false
  modalTitle.value = '新增用户'
  modalForm.id = ''
  modalForm.userAccount = ''
  modalForm.userName = ''
  modalForm.userAvatar = ''
  modalForm.userProfile = ''
  modalForm.userRole = 'user'
  modalVisible.value = true
}

function openEditModal(record: UserVO) {
  isEditMode.value = true
  modalTitle.value = '编辑用户'
  modalForm.id = record.id
  modalForm.userAccount = record.userAccount
  modalForm.userName = record.userName
  modalForm.userAvatar = record.userAvatar || ''
  modalForm.userProfile = record.userProfile || ''
  modalForm.userRole = record.userRole
  modalVisible.value = true
}

function handleModalCancel() {
  modalVisible.value = false
  modalFormRef.value?.resetFields()
}

async function handleModalOk() {
  try {
    await modalFormRef.value?.validate()
  } catch {
    return
  }

  modalSubmitting.value = true
  try {
    let res
    if (isEditMode.value) {
      const updateData: UserUpdateRequest = {
        id: modalForm.id,
        userName: modalForm.userName,
        userAvatar: modalForm.userAvatar,
        userProfile: modalForm.userProfile,
        userRole: modalForm.userRole,
      }
      res = await updateUser(updateData)
    } else {
      const addData: UserAddRequest = {
        userAccount: modalForm.userAccount,
        userName: modalForm.userName,
        userAvatar: modalForm.userAvatar,
        userProfile: modalForm.userProfile,
        userRole: modalForm.userRole,
      }
      res = await addUser(addData)
    }
    if (res.data.code === 0) {
      message.success(isEditMode.value ? '更新成功' : '新增成功')
      modalVisible.value = false
      fetchData()
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch {
    message.error('操作失败')
  } finally {
    modalSubmitting.value = false
  }
}

async function handleDelete(id: string) {
  try {
    const res = await deleteUser({ id })
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

.modal-form {
  padding: 8px 0;
}
</style>
