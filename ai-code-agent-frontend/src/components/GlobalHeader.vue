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
          <a-button type="primary">登录</a-button>
        </div>
      </a-col>
    </a-row>
  </a-layout-header>
</template>

<script setup lang="ts">
import { h, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { MenuProps } from 'ant-design-vue'

const router = useRouter()

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
</style>
