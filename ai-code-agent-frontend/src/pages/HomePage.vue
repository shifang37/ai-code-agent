<template>
  <div id="homePage">
    <a-typography-title :level="3">首页</a-typography-title>
    <a-typography-paragraph>
      欢迎使用 AI 零代码应用生成平台前端工程。菜单可切换「关于」页面；联调后端后可在控制台查看健康检查接口返回。
    </a-typography-paragraph>
    <a-space>
      <a-button type="primary" @click="router.push('/about')">前往关于</a-button>
    </a-space>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
#homePage {
  min-height: 200px;
}
</style>
