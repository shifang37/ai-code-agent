<template>
  <header class="header">
    <div class="header-left">
      <button class="menu-btn" @click="$emit('toggle-sidebar')">☰</button>
      <h2 class="page-title">{{ currentTitle }}</h2>
    </div>
    <div class="header-right">
      <div class="header-actions">
        <span class="notification">🔔</span>
        <span class="notification-badge">3</span>
      </div>
      <div class="user-info">
        <img src="https://picsum.photos/seed/admin/36/36" alt="头像" class="user-avatar" />
        <span class="user-name">管理员</span>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

defineEmits(['toggle-sidebar'])

const route = useRoute()
const currentTitle = computed(() => route.meta?.title || '电商后台管理')
</script>

<style scoped>
.header {
  height: var(--header-height);
  background: #fff;
  border-bottom: 1px solid var(--gray-200);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 50;
  box-shadow: var(--shadow);
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.menu-btn {
  display: none;
  background: none;
  font-size: 22px;
  color: var(--gray-600);
  padding: 4px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--gray-800);
}
.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.header-actions {
  position: relative;
}
.notification {
  font-size: 20px;
  cursor: pointer;
}
.notification-badge {
  position: absolute;
  top: -6px;
  right: -8px;
  background: var(--danger);
  color: #fff;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 10px;
  font-weight: 600;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}
.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--gray-700);
}
@media (max-width: 768px) {
  .menu-btn {
    display: block;
  }
  .user-name {
    display: none;
  }
}
</style>
