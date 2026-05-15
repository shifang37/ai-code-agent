<template>
  <a-layout-header class="header">
    <a-row :wrap="false" align="middle">
      <a-col flex="220px">
        <RouterLink to="/" class="header-left-link">
          <div class="header-left">
            <img class="logo" src="/logo.svg" alt="Logo" width="48" height="48" />
            <h1 class="site-title">AI 零代码应用</h1>
          </div>
        </RouterLink>
      </a-col>
      <a-col flex="auto">
        <a-menu
          v-model:selected-keys="selectedKeys"
          mode="horizontal"
          :items="menuItems"
          class="header-menu"
          @click="handleMenuClick"
        />
      </a-col>
      <a-col>
        <div class="user-login-status">
          <template v-if="userStore.isLoggedIn">
            <a-dropdown>
              <a-space class="user-dropdown-trigger">
                <a-avatar :size="32" :src="userStore.loginUser?.userAvatar">
                  {{ (userStore.loginUser?.userName || '用')[0] }}
                </a-avatar>
                <span class="user-name">{{ userStore.loginUser?.userName }}</span>
                <DownOutlined />
              </a-space>
              <template #overlay>
                <a-menu @click="handleMenuAction">
                  <a-menu-item key="profile">
                    <UserOutlined />
                    个人中心
                  </a-menu-item>
                  <a-menu-item v-if="userStore.isAdmin" key="admin">
                    <SettingOutlined />
                    用户管理
                  </a-menu-item>
                  <a-menu-item v-if="userStore.isAdmin" key="adminApp">
                    <AppstoreOutlined />
                    应用管理
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="logout" danger>
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
          <a-button v-else type="primary" @click="handleLogin">登录</a-button>
        </div>
      </a-col>
    </a-row>
  </a-layout-header>
</template>

<script setup lang="ts">
import { h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { MenuProps } from 'ant-design-vue'
import {
  DownOutlined,
  UserOutlined,
  SettingOutlined,
  LogoutOutlined,
  AppstoreOutlined,
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const selectedKeys = ref<string[]>([router.currentRoute.value.path])

router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

const menuItems = ref<NonNullable<MenuProps['items']>>([
  {
    key: '/',
    label: '首页',
    title: '首页',
  },
  {
    key: '/about',
    label: '关于',
    title: '关于我们',
  },
  {
    key: 'others',
    label: h('a', { href: 'https://www.codefather.cn', target: '_blank' }, '编程导航'),
    title: '编程导航',
  },
])

const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  if (key.startsWith('/')) {
    void router.push(key)
  }
}

function handleLogin() {
  router.push('/user/login')
}

async function handleMenuAction({ key }: { key: string }) {
  if (key === 'profile') {
    await router.push('/user/profile')
  } else if (key === 'admin') {
    await router.push('/admin/user')
  } else if (key === 'adminApp') {
    await router.push('/admin/app')
  } else if (key === 'logout') {
    const res = await userStore.logout()
    if (res.code === 0) {
      message.success('已退出登录')
      window.location.href = '/'
    }
  }
}
</script>

<style scoped>
.header {
  background: #fff;
  padding: 8px 24px;
  line-height: normal;
  height: auto;
}

.header-left-link {
  color: inherit;
  text-decoration: none;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  display: block;
  height: 48px;
  width: 48px;
}

.site-title {
  margin: 0;
  font-size: 18px;
  color: #1677ff;
  font-weight: 600;
}

.header-menu {
  border-bottom: none !important;
  flex: 1;
  min-width: 0;
  justify-content: flex-end;
}

.user-login-status {
  margin-left: 16px;
}

.user-dropdown-trigger {
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-dropdown-trigger:hover {
  background: #f5f5f5;
}

.user-name {
  font-size: 14px;
  color: #333;
}
</style>
