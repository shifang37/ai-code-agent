<script setup lang="ts">
import { onMounted } from 'vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import BasicLayout from '@/layouts/BasicLayout.vue'
import { healthCheck } from '@/api/healthController'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

onMounted(() => {
  healthCheck()
    .then((res) => {
      console.log(res)
    })
    .catch(() => {
      /* 后端未启动时忽略 */
    })

  userStore.fetchLoginUser()
})
</script>

<template>
  <a-config-provider :locale="zhCN">
    <BasicLayout />
  </a-config-provider>
</template>

<style></style>
