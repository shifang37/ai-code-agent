<template>
  <div class="app-card" @click="$emit('click', app)">
    <div class="app-card-cover">
      <img v-if="app.cover" :src="app.cover" alt="cover" />
      <div v-else class="app-card-cover-placeholder">
        <AppstoreOutlined />
      </div>
      <a-tag v-if="codeGenTypeLabel" color="blue" class="app-card-type-tag">
        {{ codeGenTypeLabel }}
      </a-tag>
      <div v-if="showActions" class="app-card-actions" @click.stop>
        <slot name="actions" :app="app" />
      </div>
    </div>
    <div class="app-card-body">
      <h4 class="app-card-name">{{ app.appName || '未命名应用' }}</h4>
      <p class="app-card-desc">{{ app.initPrompt?.slice(0, 50) || '暂无描述' }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { AppstoreOutlined } from '@ant-design/icons-vue'
import { CodeGenTypeText, type AppVO } from '@/models'

const props = defineProps<{
  app: AppVO
  showActions?: boolean
}>()

defineEmits<{
  click: [app: AppVO]
}>()

const codeGenTypeLabel = computed(() => {
  const type = props.app?.codeGenType
  return type ? CodeGenTypeText[type] : ''
})
</script>

<style scoped>
.app-card {
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
  transition: box-shadow 0.2s, transform 0.2s;
}

.app-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.app-card-cover {
  position: relative;
  height: 120px;
  overflow: hidden;
  background: #f5f5f5;
}

.app-card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-card-cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 36px;
  color: #ccc;
}

.app-card-actions {
  position: absolute;
  top: 6px;
  right: 6px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 6px;
  padding: 2px;
}

.app-card-type-tag {
  position: absolute;
  bottom: 6px;
  right: 6px;
  margin: 0;
  font-size: 12px;
}

.app-card-cover:hover .app-card-actions {
  opacity: 1;
}

.app-card-body {
  padding: 12px 16px;
}

.app-card-name {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-card-desc {
  margin: 0;
  font-size: 13px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
