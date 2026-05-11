<template>
  <a-layout class="basic-layout">
    <GlobalHeader />

    <a-layout-content class="main-content">
      <router-view />
    </a-layout-content>

    <GlobalFooter />
  </a-layout>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
  background: #f5f5f5;
}

.main-content {
  max-width: 1200px;
  width: 100%;
  padding: 24px;
  background: #fff;
  margin: 16px auto 88px;
  box-sizing: border-box;
}
</style>
