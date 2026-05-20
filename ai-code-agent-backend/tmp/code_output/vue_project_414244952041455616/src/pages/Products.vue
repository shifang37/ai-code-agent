<template>
  <div class="products-page">
    <div class="card">
      <div class="card-header">
        <h3>商品列表</h3>
        <router-link to="/products/add" class="btn btn-primary">+ 添加商品</router-link>
      </div>
      <div class="search-bar">
        <input v-model="searchQuery" placeholder="搜索商品名称..." @input="onSearch" />
        <select v-model="filterCategory" @change="onSearch">
          <option value="">全部分类</option>
          <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
        </select>
        <select v-model="filterStatus" @change="onSearch">
          <option value="">全部状态</option>
          <option value="上架">上架</option>
          <option value="下架">下架</option>
        </select>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width:60px">图片</th>
              <th>商品名称</th>
              <th>分类</th>
              <th>价格</th>
              <th>库存</th>
              <th>销量</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="paginatedData.length === 0">
              <td colspan="8" style="text-align:center;padding:40px;color:var(--gray-400)">暂无数据</td>
            </tr>
            <tr v-for="item in paginatedData" :key="item.id">
              <td>
                <img :src="item.image" :alt="item.name" class="product-thumb" />
              </td>
              <td>
                <span class="product-name">{{ item.name }}</span>
              </td>
              <td>{{ item.category }}</td>
              <td><strong>¥{{ item.price.toLocaleString() }}</strong></td>
              <td>{{ item.stock }}</td>
              <td>{{ item.sales.toLocaleString() }}</td>
              <td>
                <span class="status-tag" :class="item.status === '上架' ? 'status-active' : 'status-inactive'">
                  {{ item.status }}
                </span>
              </td>
              <td>
                <div class="action-btns">
                  <router-link :to="'/products/edit/' + item.id" class="btn btn-sm btn-outline">编辑</router-link>
                  <button class="btn btn-sm btn-danger" @click="deleteProduct(item.id)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination" v-if="totalPages > 1">
        <button :disabled="currentPage === 1" @click="currentPage--">上一页</button>
        <button v-for="p in totalPages" :key="p" :class="{ active: p === currentPage }" @click="currentPage = p">{{ p }}</button>
        <button :disabled="currentPage === totalPages" @click="currentPage++">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { products as allProducts } from '@/utils/mockData.js'

const router = useRouter()
const searchQuery = ref('')
const filterCategory = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = 8

const categories = [...new Set(allProducts.map(p => p.category))]

const filteredProducts = computed(() => {
  return allProducts.filter(p => {
    const matchSearch = !searchQuery.value || p.name.includes(searchQuery.value)
    const matchCategory = !filterCategory.value || p.category === filterCategory.value
    const matchStatus = !filterStatus.value || p.status === filterStatus.value
    return matchSearch && matchCategory && matchStatus
  })
})

const totalPages = computed(() => Math.ceil(filteredProducts.value.length / pageSize))

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredProducts.value.slice(start, start + pageSize)
})

const onSearch = () => {
  currentPage.value = 1
}

const deleteProduct = (id) => {
  if (confirm('确定要删除该商品吗？')) {
    const idx = allProducts.findIndex(p => p.id === id)
    if (idx > -1) allProducts.splice(idx, 1)
  }
}
</script>

<style scoped>
.products-page {
  max-width: 1200px;
}
.product-thumb {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  object-fit: cover;
}
.product-name {
  font-weight: 500;
  color: var(--gray-800);
}
.action-btns {
  display: flex;
  gap: 6px;
}
</style>
