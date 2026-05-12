import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import { useUserStore } from '@/stores/user'
import { message } from 'ant-design-vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage,
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('@/pages/AboutPage.vue'),
    },
    {
      path: '/user/login',
      name: 'login',
      component: () => import('@/pages/LoginPage.vue'),
      meta: { guest: true },
    },
    {
      path: '/user/profile',
      name: 'profile',
      component: () => import('@/pages/UserProfilePage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/user',
      name: 'adminUser',
      component: () => import('@/pages/AdminUserPage.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
  ],
})

let storeInitialized = false

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  if (!storeInitialized) {
    await userStore.fetchLoginUser()
    storeInitialized = true
  }

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    message.warning('请先登录')
    next({ name: 'login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    message.error('没有访问权限')
    next({ name: 'home' })
    return
  }

  if (to.meta.guest && userStore.isLoggedIn) {
    next({ name: 'home' })
    return
  }

  next()
})

export default router
