import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/pages/Dashboard.vue'),
    meta: { title: '仪表盘', icon: '📊' }
  },
  {
    path: '/products',
    name: 'Products',
    component: () => import('@/pages/Products.vue'),
    meta: { title: '商品管理', icon: '📦' }
  },
  {
    path: '/products/add',
    name: 'ProductAdd',
    component: () => import('@/pages/ProductForm.vue'),
    meta: { title: '添加商品', icon: '➕' }
  },
  {
    path: '/products/edit/:id',
    name: 'ProductEdit',
    component: () => import('@/pages/ProductForm.vue'),
    meta: { title: '编辑商品', icon: '✏️' }
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('@/pages/Orders.vue'),
    meta: { title: '订单管理', icon: '📋' }
  },
  {
    path: '/orders/:id',
    name: 'OrderDetail',
    component: () => import('@/pages/OrderDetail.vue'),
    meta: { title: '订单详情', icon: '🔍' }
  },
  {
    path: '/users',
    name: 'Users',
    component: () => import('@/pages/Users.vue'),
    meta: { title: '用户管理', icon: '👥' }
  },
  {
    path: '/categories',
    name: 'Categories',
    component: () => import('@/pages/Categories.vue'),
    meta: { title: '分类管理', icon: '🏷️' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
