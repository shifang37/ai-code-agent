<template>
  <a-layout-footer class="footer">
    <div class="footer-content">
      <p class="copyright">© AI 零代码应用</p>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts"></script>

<style scoped>
.footer {
  background: #efefef;
  text-align: center;
  padding: 20px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 10;
}

.copyright {
  margin: 0;
  color: #666;
  font-size: 14px;
}
</style>
