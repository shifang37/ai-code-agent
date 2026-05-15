<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="login-brand">
        <div class="brand-overlay"></div>
        <div class="brand-content">
          <div class="brand-logo">
            <img src="/logo.svg" alt="Logo" width="64" height="64" />
          </div>
          <h1 class="brand-title">AI 零代码应用</h1>
          <p class="brand-desc">智能生成，高效开发。让 AI 成为你的编程伙伴。</p>
          <div class="brand-features">
            <div class="feature-item">
              <span class="feature-icon">⚡</span>
              <span>零代码生成应用</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">🤖</span>
              <span>多智能体协作</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">🚀</span>
              <span>极速开发体验</span>
            </div>
          </div>
        </div>
      </div>

      <div class="login-form-wrapper">
        <div class="login-form-container">
          <a-tabs v-model:active-key="activeTab" centered size="large">
            <a-tab-pane key="login" tab="登录" />
            <a-tab-pane key="register" tab="注册" />
          </a-tabs>

          <!-- Login Form -->
          <a-form
            v-if="activeTab === 'login'"
            :model="loginForm"
            :rules="loginRules"
            layout="vertical"
            class="login-form"
            @finish="handleLogin"
          >
            <a-form-item name="userAccount">
              <a-input
                v-model:value="loginForm.userAccount"
                size="large"
                placeholder="请输入账号"
                allow-clear
              >
                <template #prefix>
                  <UserOutlined />
                </template>
              </a-input>
            </a-form-item>

            <a-form-item name="userPassword">
              <a-input-password
                v-model:value="loginForm.userPassword"
                size="large"
                placeholder="请输入密码"
              >
                <template #prefix>
                  <LockOutlined />
                </template>
              </a-input-password>
            </a-form-item>

            <a-form-item>
              <a-button
                type="primary"
                html-type="submit"
                size="large"
                :loading="submitting"
                block
              >
                登 录
              </a-button>
            </a-form-item>

            <div class="form-tip">
              还没有账号？
              <a-button type="link" @click="activeTab = 'register'">立即注册</a-button>
            </div>
          </a-form>

          <!-- Register Form -->
          <a-form
            v-else
            :model="registerForm"
            :rules="registerRules"
            layout="vertical"
            class="login-form"
            @finish="handleRegister"
          >
            <a-form-item name="userAccount">
              <a-input
                v-model:value="registerForm.userAccount"
                size="large"
                placeholder="请输入账号（至少4位）"
                allow-clear
              >
                <template #prefix>
                  <UserOutlined />
                </template>
              </a-input>
            </a-form-item>

            <a-form-item name="userPassword">
              <a-input-password
                v-model:value="registerForm.userPassword"
                size="large"
                placeholder="请输入密码（至少8位）"
              >
                <template #prefix>
                  <LockOutlined />
                </template>
              </a-input-password>
            </a-form-item>

            <a-form-item name="checkPassword">
              <a-input-password
                v-model:value="registerForm.checkPassword"
                size="large"
                placeholder="请确认密码"
              >
                <template #prefix>
                  <SafetyOutlined />
                </template>
              </a-input-password>
            </a-form-item>

            <a-form-item>
              <a-button
                type="primary"
                html-type="submit"
                size="large"
                :loading="submitting"
                block
              >
                注 册
              </a-button>
            </a-form-item>

            <div class="form-tip">
              已有账号？
              <a-button type="link" @click="activeTab = 'login'">立即登录</a-button>
            </div>
          </a-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import type { Rule } from 'ant-design-vue/es/form'

const route = useRoute()
const userStore = useUserStore()
const activeTab = ref<string>('login')
const submitting = ref(false)

const loginForm = reactive({
  userAccount: '',
  userPassword: '',
})

const registerForm = reactive({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const loginRules: Record<string, Rule[]> = {
  userAccount: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  userPassword: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const registerRules: Record<string, Rule[]> = {
  userAccount: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, message: '账号至少4位', trigger: 'blur' },
  ],
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少8位', trigger: 'blur' },
  ],
  checkPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value) => {
        if (value !== registerForm.userPassword) {
          return Promise.reject('两次输入的密码不一致')
        }
        return Promise.resolve()
      },
      trigger: 'blur',
    },
  ],
}

async function handleLogin() {
  submitting.value = true
  try {
    const res = await userStore.login({
      userAccount: loginForm.userAccount,
      userPassword: loginForm.userPassword,
    })
    if (res.code === 0) {
      message.success('登录成功')
      const redirect = (route.query.redirect as string) || '/'
      window.location.href = redirect
    } else {
      message.error(res.message || '登录失败')
    }
  } catch {
    message.error('登录失败，请检查网络连接')
  } finally {
    submitting.value = false
  }
}

async function handleRegister() {
  submitting.value = true
  try {
    const res = await userStore.register({
      userAccount: registerForm.userAccount,
      userPassword: registerForm.userPassword,
      checkPassword: registerForm.checkPassword,
    })
    if (res.code === 0) {
      message.success('注册成功，请登录')
      activeTab.value = 'login'
      loginForm.userAccount = registerForm.userAccount
    } else {
      message.error(res.message || '注册失败')
    }
  } catch {
    message.error('注册失败，请检查网络连接')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 144px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-panel {
  display: flex;
  width: 900px;
  min-height: 560px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.1);
}

/* Left Brand Panel */
.login-brand {
  position: relative;
  width: 420px;
  padding: 48px 40px;
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 40%, #002c8c 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}

.brand-overlay {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 20% 80%, rgba(255, 255, 255, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.05) 0%, transparent 40%);
}

.brand-content {
  position: relative;
  z-index: 1;
}

.brand-logo {
  margin-bottom: 24px;
}

.brand-logo img {
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.15));
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 12px;
  letter-spacing: 2px;
}

.brand-desc {
  font-size: 14px;
  opacity: 0.85;
  line-height: 1.6;
  margin: 0 0 36px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  opacity: 0.9;
}

.feature-icon {
  font-size: 20px;
  width: 28px;
  text-align: center;
}

/* Right Form Panel */
.login-form-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  background: #fff;
}

.login-form-container {
  width: 100%;
  max-width: 320px;
}

.login-form {
  margin-top: 16px;
}

.login-form :deep(.ant-tabs-nav) {
  margin-bottom: 32px;
}

.login-form :deep(.ant-tabs-tab) {
  font-size: 16px;
  padding: 8px 24px;
}

.login-form :deep(.ant-form-item) {
  margin-bottom: 20px;
}

.form-tip {
  text-align: center;
  font-size: 13px;
  color: #999;
}

.form-tip .ant-btn {
  padding: 0 4px;
  font-size: 13px;
}

@media (max-width: 768px) {
  .login-panel {
    flex-direction: column;
    width: 100%;
    max-width: 400px;
    min-height: auto;
  }

  .login-brand {
    width: 100%;
    padding: 32px 24px;
  }

  .brand-features {
    display: none;
  }

  .login-form-wrapper {
    padding: 32px 24px;
  }
}
</style>
