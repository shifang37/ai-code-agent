<template>
  <div class="profile-page">
    <div class="profile-banner">
      <div class="banner-bg"></div>
      <div class="banner-content">
        <a-avatar :size="80" :src="userStore.loginUser?.userAvatar" class="profile-avatar">
          {{ (userStore.loginUser?.userName || '用')[0] }}
        </a-avatar>
        <div class="banner-info">
          <h1 class="banner-name">{{ userStore.loginUser?.userName || '未登录' }}</h1>
          <p class="banner-account">{{ userStore.loginUser?.userAccount }}</p>
          <a-tag :color="userStore.isAdmin ? 'blue' : 'green'">
            {{ userStore.isAdmin ? '管理员' : '普通用户' }}
          </a-tag>
        </div>
      </div>
    </div>

    <div class="profile-content">
      <a-row :gutter="24">
        <a-col :span="16">
          <a-card title="基本信息" class="profile-card">
            <a-descriptions :column="2" size="middle" bordered>
              <a-descriptions-item label="用户名">
                {{ userStore.loginUser?.userName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="账号">
                {{ userStore.loginUser?.userAccount || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="角色">
                {{ userStore.loginUser?.userRole === 'admin' ? '管理员' : '普通用户' }}
              </a-descriptions-item>
              <a-descriptions-item label="用户 ID">
                {{ userStore.loginUser?.id || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="个人简介">
                {{ userStore.loginUser?.userProfile || '暂无简介' }}
              </a-descriptions-item>
              <a-descriptions-item label="注册时间">
                {{ formatTime(userStore.loginUser?.createTime) }}
              </a-descriptions-item>
              <a-descriptions-item label="最后更新" :span="2">
                {{ formatTime(userStore.loginUser?.updateTime) }}
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <a-col :span="8">
          <a-card title="快捷操作" class="profile-card">
            <a-space direction="vertical" style="width: 100%">
              <a-button block @click="router.push('/')">
                <template #icon><HomeOutlined /></template>
                返回首页
              </a-button>
              <a-button block danger @click="handleLogout">
                <template #icon><LogoutOutlined /></template>
                退出登录
              </a-button>
            </a-space>
          </a-card>

          <a-card title="角色权限" class="profile-card" style="margin-top: 16px">
            <div class="permission-list">
              <div class="permission-item">
                <CheckCircleOutlined style="color: #52c41a" />
                <span>浏览首页</span>
              </div>
              <div class="permission-item">
                <CheckCircleOutlined style="color: #52c41a" />
                <span>查看个人信息</span>
              </div>
              <div class="permission-item">
                <CheckCircleOutlined style="color: #52c41a" />
                <span>修改个人资料</span>
              </div>
              <div class="permission-item" v-if="userStore.isAdmin">
                <CrownOutlined style="color: #1677ff" />
                <span>用户管理</span>
              </div>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  HomeOutlined,
  LogoutOutlined,
  CheckCircleOutlined,
  CrownOutlined,
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()

function formatTime(time?: string) {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

async function handleLogout() {
  const res = await userStore.logout()
  if (res.code === 0) {
    message.success('已退出登录')
    await router.push('/')
  }
}
</script>

<style scoped>
.profile-page {
  margin: -24px;
}

.profile-banner {
  position: relative;
  padding: 40px 32px;
  overflow: hidden;
}

.banner-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1677ff 0%, #69b1ff 100%);
  border-radius: 0 0 16px 16px;
}

.banner-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 30% 50%, rgba(255, 255, 255, 0.12) 0%, transparent 50%),
    radial-gradient(circle at 70% 80%, rgba(255, 255, 255, 0.08) 0%, transparent 40%);
}

.banner-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 24px;
}

.profile-avatar {
  border: 3px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

.banner-info {
  color: #fff;
}

.banner-name {
  margin: 0 0 4px;
  font-size: 24px;
  font-weight: 600;
}

.banner-account {
  margin: 0 0 8px;
  font-size: 14px;
  opacity: 0.85;
}

.profile-content {
  padding: 24px 32px 16px;
}

.profile-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.permission-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.permission-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #555;
}
</style>
