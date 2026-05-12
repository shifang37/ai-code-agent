import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getLoginUser, userLogin, userLogout, userRegister } from '@/api/userController'
import type { LoginUserVO, UserLoginRequest, UserRegisterRequest } from '@/models'

export const useUserStore = defineStore('user', () => {
  const loginUser = ref<LoginUserVO | null>(null)
  const loading = ref(false)

  const isLoggedIn = computed(() => loginUser.value !== null)
  const isAdmin = computed(() => loginUser.value?.userRole === 'admin')

  async function fetchLoginUser() {
    loading.value = true
    try {
      const res = await getLoginUser()
      if (res.data.code === 0) {
        loginUser.value = res.data.data
      }
    } catch {
      loginUser.value = null
    } finally {
      loading.value = false
    }
  }

  async function login(data: UserLoginRequest) {
    const res = await userLogin(data)
    if (res.data.code === 0) {
      loginUser.value = res.data.data
    }
    return res.data
  }

  async function register(data: UserRegisterRequest) {
    const res = await userRegister(data)
    return res.data
  }

  async function logout() {
    const res = await userLogout()
    if (res.data.code === 0) {
      loginUser.value = null
    }
    return res.data
  }

  function clearUser() {
    loginUser.value = null
  }

  return {
    loginUser,
    loading,
    isLoggedIn,
    isAdmin,
    fetchLoginUser,
    login,
    register,
    logout,
    clearUser,
  }
})
