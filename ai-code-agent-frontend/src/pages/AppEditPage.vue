<template>
  <div class="edit-page">
    <PageHeader title="编辑应用" description="修改应用的基本信息" />

    <a-card class="form-card">
      <a-spin :spinning="loading">
        <a-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          layout="vertical"
          class="edit-form"
          @finish="handleSubmit"
        >
          <a-form-item label="应用 ID">
            <a-input :value="formData.id" disabled />
          </a-form-item>

          <a-form-item name="appName" label="应用名称">
            <a-input
              v-model:value="formData.appName"
              placeholder="请输入应用名称"
              allow-clear
            />
          </a-form-item>

          <template v-if="userStore.isAdmin">
            <a-form-item name="cover" label="应用封面">
              <a-input
                v-model:value="formData.cover"
                placeholder="请输入封面图片 URL（可选）"
                allow-clear
              />
            </a-form-item>

            <a-form-item name="priority" label="优先级">
              <a-input-number
                v-model:value="formData.priority"
                :min="0"
                placeholder="设置优先级"
                style="width: 200px"
              />
            </a-form-item>
          </template>

          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit" :loading="submitting">
                保存修改
              </a-button>
              <a-button @click="router.back()">取消</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import {
  getAppVoById,
  updateApp,
  updateAppByAdmin,
  getAppVoByIdByAdmin,
} from '@/api/appController'
import { useUserStore } from '@/stores/user'
import PageHeader from '@/components/PageHeader.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)

const appId = route.params.id as string

const formData = reactive({
  id: '',
  appName: '',
  cover: '',
  priority: 0,
})

const rules: Record<string, Rule[]> = {
  appName: [{ required: true, message: '请输入应用名称', trigger: 'blur' }],
}

onMounted(async () => {
  loading.value = true
  try {
    let res
    if (userStore.isAdmin) {
      res = await getAppVoByIdByAdmin({ id: appId })
    } else {
      res = await getAppVoById({ id: appId })
    }
    if (res.data.code === 0 && res.data.data) {
      const app = res.data.data
      formData.id = String(app.id ?? '')
      formData.appName = app.appName || ''
      formData.cover = app.cover || ''
      formData.priority = app.priority || 0
    } else {
      message.error('获取应用信息失败')
      router.back()
    }
  } catch {
    message.error('获取应用信息失败')
    router.back()
  } finally {
    loading.value = false
  }
})

async function handleSubmit() {
  submitting.value = true
  try {
    let res
    if (userStore.isAdmin) {
      res = await updateAppByAdmin({
        id: formData.id,
        appName: formData.appName,
        cover: formData.cover || undefined,
        priority: formData.priority,
      })
    } else {
      res = await updateApp({
        id: formData.id,
        appName: formData.appName,
      })
    }
    if (res.data.code === 0) {
      message.success('保存成功')
      router.back()
    } else {
      message.error(res.data.message || '保存失败')
    }
  } catch {
    message.error('保存失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.edit-page {
  margin: -24px;
}

.form-card {
  margin: 20px 32px 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  max-width: 600px;
}

.edit-form {
  padding: 8px 0;
}
</style>
