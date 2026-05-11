<template>
  <div id="aboutPage">
    <a-typography-title :level="3">关于</a-typography-title>
    <a-typography-paragraph>
      本页对应路由 <code>/about</code>，用于验证菜单高亮与路由懒加载是否正常。
    </a-typography-paragraph>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped>
#aboutPage {
  min-height: 200px;
}
</style>
