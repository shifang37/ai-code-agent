<template>
  <div class="product-form-page">
    <div class="card" style="max-width: 720px;">
      <div class="card-header">
        <h3>{{ isEdit ? '编辑商品' : '添加商品' }}</h3>
        <router-link to="/products" class="btn btn-sm btn-outline">返回列表</router-link>
      </div>
      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label>商品名称</label>
          <input v-model="form.name" placeholder="请输入商品名称" required />
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>分类</label>
            <select v-model="form.category" required>
              <option value="">请选择</option>
              <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>价格（元）</label>
            <input v-model.number="form.price" type="number" min="0" step="0.01" placeholder="0.00" required />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>库存数量</label>
            <input v-model.number="form.stock" type="number" min="0" placeholder="0" required />
          </div>
          <div class="form-group">
            <label>商品状态</label>
            <select v-model="form.status" required>
              <option value="上架">上架</option>
              <option value="下架">下架</option>
            </select>
          </div>
        </div>
        <div class="form-group">
          <label>商品描述</label>
          <textarea v-model="form.desc" placeholder="请输入商品描述信息" rows="4"></textarea>
        </div>
        <div class="form-group">
          <label>商品图片地址</label>
          <input v-model="form.image" placeholder="https://picsum.photos/seed/..." />
        </div>
        <div style="display:flex;gap:12px;margin-top:20px;">
          <button type="submit" class="btn btn-primary">{{ isEdit ? '保存修改' : '添加商品' }}</button>
          <router-link to="/products" class="btn btn-outline">取消</router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { products, categories as catList } from '@/utils/mockData.js'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)

const categories = catList.map(c => c.name)

const form = ref({
  name: '',
  category: '',
  price: 0,
  stock: 0,
  status: '上架',
  desc: '',
  image: 'https://picsum.photos/seed/product/200/200'
})

if (isEdit.value) {
  const product = products.find(p => p.id === Number(route.params.id))
  if (product) {
    form.value = { ...product, desc: product.desc || '' }
  }
}

const handleSubmit = () => {
  if (isEdit.value) {
    const idx = products.findIndex(p => p.id === Number(route.params.id))
    if (idx > -1) {
      products[idx] = { ...products[idx], ...form.value }
    }
    alert('商品修改成功！')
  } else {
    const newId = Math.max(...products.map(p => p.id)) + 1
    products.unshift({
      id: newId,
      ...form.value,
      sales: 0,
      created: new Date().toISOString().slice(0, 10)
    })
    alert('商品添加成功！')
  }
  router.push('/products')
}
</script>

<style scoped>
.product-form-page {
  max-width: 720px;
}
</style>
