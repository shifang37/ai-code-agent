<template>
  <div class="layout">
    <Sidebar :collapsed="sidebarCollapsed" @toggle="sidebarCollapsed = !sidebarCollapsed" />
    <div class="layout-main" :class="{ expanded: sidebarCollapsed }">
      <Header @toggle-sidebar="sidebarCollapsed = !sidebarCollapsed" />
      <main class="layout-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'

const sidebarCollapsed = ref(false)
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
}
.layout-main {
  flex: 1;
  margin-left: var(--sidebar-width);
  transition: margin-left 0.3s;
  display: flex;
  flex-direction: column;
}
.layout-main.expanded {
  margin-left: 64px;
}
.layout-content {
  flex: 1;
  padding: 24px;
  background: var(--gray-100);
}
@media (max-width: 768px) {
  .layout-main {
    margin-left: 0;
  }
}
</style>
