<template>
  <div class="register-container">
    <div class="bg-animation">
      <div class="bg-circle circle-1"></div>
      <div class="bg-circle circle-2"></div>
      <div class="bg-circle circle-3"></div>
      <div class="bg-circle circle-4"></div>
    </div>

    <div class="register-card">
      <div class="card-glow"></div>

      <div class="register-header">
        <div class="logo-icon">
          <el-icon :size="40"><User /></el-icon>
        </div>
        <h1 class="register-title">创建账号</h1>
        <p class="register-subtitle">开启你的 AI 虚拟伙伴之旅</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请设置用户名 (3-32位)"
            size="large"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="请设置密码 (6-32位)"
            size="large"
            :prefix-icon="Lock"
          >
            <template #suffix>
              <el-icon
                class="toggle-icon"
                @click="showPassword = !showPassword"
              >
                <View v-if="!showPassword" />
                <Hide v-else />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            :type="showConfirmPassword ? 'text' : 'password'"
            placeholder="请再次输入密码"
            size="large"
            :prefix-icon="Lock"
          >
            <template #suffix>
              <el-icon
                class="toggle-icon"
                @click="showConfirmPassword = !showConfirmPassword"
              >
                <View v-if="!showConfirmPassword" />
                <Hide v-else />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="nickname">
          <el-input
            v-model="form.nickname"
            placeholder="请输入昵称 (可选)"
            size="large"
            :prefix-icon="UserFilled"
            clearable
          />
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          class="register-btn"
          :loading="loading"
          @click="handleRegister"
        >
          <span v-if="!loading">注 册</span>
          <span v-else>注册中...</span>
        </el-button>

        <div class="register-footer">
          <span class="footer-text">已有账号？</span>
          <router-link to="/login" class="login-link">返回登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, View, Hide, UserFilled } from '@element-plus/icons-vue'
import { register as registerApi } from '../api/user'

const router = useRouter()

const formRef = ref(null)
const loading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度为 3-32 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await registerApi({
        username: form.username,
        password: form.password,
        nickname: form.nickname || undefined
      })
      ElMessage.success('注册成功，请登录')
      router.push({ path: '/login', query: { username: form.username } })
    } catch (e) {
      // error handled by request interceptor
    } finally {
      loading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.register-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 25%, #667eea 50%, #764ba2 75%, #4facfe 100%);
  background-size: 400% 400%;
  animation: gradientShift 15s ease infinite;
  overflow: hidden;
  padding: 40px 0;
}

@keyframes gradientShift {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

.bg-animation {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  animation: float 20s infinite ease-in-out;
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -80px;
  right: -80px;
  animation-delay: 0s;
}

.circle-2 {
  width: 200px;
  height: 200px;
  bottom: 10%;
  left: -60px;
  animation-delay: -5s;
  background: rgba(255, 255, 255, 0.12);
}

.circle-3 {
  width: 150px;
  height: 150px;
  top: 40%;
  right: 10%;
  animation-delay: -10s;
  background: rgba(255, 255, 255, 0.1);
}

.circle-4 {
  width: 100px;
  height: 100px;
  bottom: 20%;
  right: 30%;
  animation-delay: -15s;
  background: rgba(255, 255, 255, 0.08);
}

@keyframes float {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(-30px, 40px) scale(1.05);
  }
  50% {
    transform: translate(20px, -20px) scale(0.95);
  }
  75% {
    transform: translate(-40px, -30px) scale(1.02);
  }
}

.register-card {
  position: relative;
  width: 460px;
  max-width: 92vw;
  padding: 44px 40px 32px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(24px) saturate(1.8);
  -webkit-backdrop-filter: blur(24px) saturate(1.8);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  animation: cardIn 0.8s cubic-bezier(0.16, 1, 0.3, 1);
  z-index: 1;
}

@keyframes cardIn {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.card-glow {
  position: absolute;
  top: -1px;
  left: 20%;
  right: 20%;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.8), transparent);
  animation: glow 3s ease-in-out infinite;
}

@keyframes glow {
  0%,
  100% {
    opacity: 0.3;
  }
  50% {
    opacity: 1;
  }
}

.register-header {
  text-align: center;
  margin-bottom: 28px;
}

.logo-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  margin-bottom: 14px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.4), rgba(255, 255, 255, 0.1));
  border-radius: 20px;
  color: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  animation: iconPulse 3s ease-in-out infinite;
}

@keyframes iconPulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.register-title {
  font-size: 26px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
  margin-bottom: 8px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}

.register-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  letter-spacing: 0.5px;
}

.register-form {
  :deep(.el-form-item) {
    margin-bottom: 18px;
  }

  :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.25);
    border: 1px solid rgba(255, 255, 255, 0.4);
    border-radius: 12px;
    box-shadow: none;
    transition: all 0.3s ease;

    &:hover {
      background: rgba(255, 255, 255, 0.35);
      border-color: rgba(255, 255, 255, 0.6);
    }

    &.is-focus {
      background: rgba(255, 255, 255, 0.4);
      border-color: rgba(255, 255, 255, 0.8);
    }
  }

  :deep(.el-input__inner) {
    color: #fff;
    font-size: 15px;
    height: 46px;

    &::placeholder {
      color: rgba(255, 255, 255, 0.65);
    }
  }

  :deep(.el-input__prefix .el-icon) {
    color: rgba(255, 255, 255, 0.8);
    font-size: 17px;
  }

  :deep(.el-input__suffix .el-icon) {
    color: rgba(255, 255, 255, 0.8);
  }
}

.toggle-icon {
  cursor: pointer;
  transition: color 0.2s;

  &:hover {
    color: #fff !important;
  }
}

.register-btn {
  width: 100%;
  height: 48px;
  margin-top: 4px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #f5576c 0%, #667eea 100%);
  box-shadow: 0 8px 24px rgba(245, 87, 108, 0.4);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 32px rgba(245, 87, 108, 0.55);
  }

  &:active {
    transform: translateY(0);
  }
}

.register-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.25);
}

.footer-text {
  color: rgba(255, 255, 255, 0.75);
  font-size: 14px;
}

.login-link {
  margin-left: 8px;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  border-bottom: 1px solid rgba(255, 255, 255, 0.5);
  transition: all 0.2s;

  &:hover {
    color: #fff;
    border-bottom-color: #fff;
    text-shadow: 0 0 8px rgba(255, 255, 255, 0.5);
  }
}
</style>