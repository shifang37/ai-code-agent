<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import BasicLayout from '@/layouts/BasicLayout.vue'
import { healthCheck } from '@/api/healthController'

healthCheck()
  .then((res) => {
    console.log(res)
  })
  .catch(() => {
    /* 后端未启动时忽略，与后端联调时可删除 catch 观察完整响应 */
  })
</script>

<template>
  <a-config-provider :locale="zhCN">
    <BasicLayout />
  </a-config-provider>
</template>

<style></style>
