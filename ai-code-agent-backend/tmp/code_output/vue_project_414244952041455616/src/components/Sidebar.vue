<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="sidebar-logo">
      <span class="logo-icon">🛒</span>
      <span v-show="!collapsed" class="logo-text">电商管理</span>
    </div>
    <nav class="sidebar-nav">
      <router-link
        v-for="item in menuItems"
        :key="item.path"
        :to="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
      >
        <span class="nav-icon">{{ item.icon }}</span>
        <span v-show="!collapsed" class="nav-label">{{ item.title }}</span>
      </router-link>
    </nav>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const props = defineProps({
  collapsed: Boolean
})
defineEmits(['toggle'])

const route = useRoute()

const menuItems = [
  { path: '/dashboard', title: '仪表盘', icon: '📊' },
  { path: '/products', title: '商品管理', icon: '📦' },
  { path: '/orders', title: '订单管理', icon: '📋' },
  { path: '/users', title: '用户管理', icon: '👥' },
  { path: '/categories', title: '分类管理', icon: '🏷️' }
]

const isActive = (path) => {
  if (path === '/dashboard') return route.path === '/dashboard'
  return route.path.startsWith(path)
}
</script>

<style scoped>
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: var(--sidebar-width);
  background: var(--gray-900);
  color: #fff;
  transition: width 0.3s;
  z-index: 100;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.sidebar.collapsed {
  width: 64px;
}
.sidebar-logo {
  height: var(--header-height);
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 10px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  flex-shrink: 0;
}
.logo-icon {
  font-size: 24px;
}
.logo-text {
  font-size: 18px;
  font-weight: 700;
  white-space: nowrap;
}
.sidebar-nav {
  flex: 1;
  padding: 12px 0;
  overflow-y: auto;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  color: rgba(255,255,255,0.7);
  font-size: 14px;
  transition: all 0.2s;
  white-space: nowrap;
  cursor: pointer;
}
.nav-item:hover {
  background: rgba(255,255,255,0.08);
  color: #fff;
}
.nav-item.active {
  background: var(--primary);
  color: #fff;
}
.nav-icon {
  font-size: 18px;
  width: 24px;
  text-align: center;
  flex-shrink: 0;
}
.nav-label {
  font-weight: 500;
}
@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
  }
  .sidebar:not(.collapsed) {
    transform: translateX(0);
    width: var(--sidebar-width);
  }
}
</style>
