
<template>
  <main class="app-shell">
    <section v-if="route !== '/dashboard'" class="auth-page">
      <div class="auth-panel">
        <div class="brand-row">
          <el-icon class="brand-icon"><Reading /></el-icon>
          <div>
            <h1>论文推荐系统</h1>
            <p>登录后查看推荐工作台和服务状态</p>
          </div>
        </div>

        <el-tabs v-model="authTab" stretch @tab-change="handleAuthTabChange">
          <el-tab-pane label="登录" name="/login">
            <el-form label-position="top" @submit.prevent>
              <el-form-item label="账号">
                <el-input
                  v-model="loginForm.username"
                  autocomplete="username"
                  placeholder="fjy"
                  @keyup.enter="handleLogin"
                />
              </el-form-item>
              <el-form-item label="密码">
                <el-input
                  v-model="loginForm.password"
                  autocomplete="current-password"
                  placeholder="123456"
                  show-password
                  type="password"
                  @keyup.enter="handleLogin"
                />
              </el-form-item>
              <el-button :loading="authLoading" class="full-button" type="primary" @click="handleLogin">
                <el-icon><Unlock /></el-icon>
                登录
              </el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="/register">
            <el-form label-position="top" @submit.prevent>
              <el-form-item label="账号">
                <el-input
                  v-model="registerForm.username"
                  autocomplete="username"
                  placeholder="输入新账号"
                  @keyup.enter="handleRegister"
                />
              </el-form-item>
              <el-form-item label="密码">
                <el-input
                  v-model="registerForm.password"
                  autocomplete="new-password"
                  placeholder="输入密码"
                  show-password
                  type="password"
                  @keyup.enter="handleRegister"
                />
              </el-form-item>
              <el-button :loading="authLoading" class="full-button" type="primary" @click="handleRegister">
                <el-icon><CirclePlus /></el-icon>
                注册并登录
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </section>

    <section v-else class="dashboard">
      <aside class="side-nav">
        <div class="side-brand">
          <el-icon><Reading /></el-icon>
          <span>论文推荐系统</span>
        </div>
        <el-button plain type="primary" @click="refreshDashboard">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button plain @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出
        </el-button>
      </aside>

      <section class="workbench">
        <header class="top-bar">
          <div>
            <p class="eyebrow">当前用户</p>
            <h2>{{ currentUser?.username || '未登录' }}</h2>
          </div>
          <el-tag :type="currentUser?.role === 'ADMIN' ? 'success' : 'info'" size="large">
            {{ currentUser?.role || 'UNKNOWN' }}
          </el-tag>
        </header>

        <div class="service-panel">
          <div class="card-title">
            <el-icon><Connection /></el-icon>
            服务状态
          </div>
          <div class="status-list">
            <div v-for="item in healthItems" :key="item.label" class="status-item">
              <span>{{ item.label }}</span>
              <el-tag :type="item.value === 'UP' ? 'success' : 'danger'">{{ item.value || 'UNKNOWN' }}</el-tag>
            </div>
          </div>
        </div>

        <PaperWorkbench :current-user="currentUser" />

        <el-alert
          v-if="adminMessage"
          :title="adminMessage"
          :type="currentUser?.role === 'ADMIN' ? 'success' : 'warning'"
          show-icon
        />
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  CirclePlus,
  Connection,
  Reading,
  Refresh,
  SwitchButton,
  Unlock
} from '@element-plus/icons-vue'
import PaperWorkbench from './components/PaperWorkbench.vue'
import {
  clearToken,
  fetchCurrentUser,
  fetchHealth,
  getToken,
  loginUser,
  pingAdmin,
  registerUser,
  saveToken
} from './api'

const route = ref(normalizeRoute(window.location.pathname))
const authTab = ref(route.value === '/register' ? '/register' : '/login')
const authLoading = ref(false)
const currentUser = ref(null)
const health = ref(null)
const adminMessage = ref('')

const loginForm = reactive({
  username: 'fjy',
  password: '123456'
})

const registerForm = reactive({
  username: '',
  password: ''
})

const healthItems = computed(() => [
  { label: 'Java 服务', value: health.value?.status },
  { label: 'MySQL', value: health.value?.mysql },
  { label: 'Redis', value: health.value?.redis },
  { label: 'Python 推理', value: health.value?.pythonInfer }
])

onMounted(async () => {
  window.addEventListener('popstate', syncRouteFromLocation)
  if (getToken()) {
    await restoreSession()
  } else if (route.value === '/dashboard') {
    navigate('/login')
  }
})

async function handleLogin() {
  authLoading.value = true
  try {
    const response = await loginUser(loginForm)
    saveToken(response.token)
    currentUser.value = response.user
    ElMessage.success('登录成功')
    navigate('/dashboard')
    await refreshDashboard()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    authLoading.value = false
  }
}

async function handleRegister() {
  authLoading.value = true
  try {
    await registerUser(registerForm)
    const response = await loginUser(registerForm)
    saveToken(response.token)
    currentUser.value = response.user
    ElMessage.success('注册成功')
    navigate('/dashboard')
    await refreshDashboard()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    authLoading.value = false
  }
}

async function restoreSession() {
  try {
    currentUser.value = await fetchCurrentUser()
    navigate('/dashboard', true)
    await refreshDashboard()
  } catch (error) {
    clearToken()
    currentUser.value = null
    if (route.value === '/dashboard') {
      navigate('/login')
    }
  }
}

async function refreshDashboard() {
  health.value = await fetchHealth()
  adminMessage.value = ''
  if (currentUser.value?.role === 'ADMIN') {
    await pingAdmin()
    adminMessage.value = '管理员接口可访问'
  } else {
    adminMessage.value = '普通用户无管理员权限'
  }
}

function handleLogout() {
  clearToken()
  currentUser.value = null
  health.value = null
  navigate('/login')
}

function handleAuthTabChange(name) {
  navigate(name)
}

function navigate(path, replace = false) {
  route.value = normalizeRoute(path)
  authTab.value = route.value === '/register' ? '/register' : '/login'
  const method = replace ? 'replaceState' : 'pushState'
  window.history[method]({}, '', route.value)
}

function syncRouteFromLocation() {
  route.value = normalizeRoute(window.location.pathname)
  authTab.value = route.value === '/register' ? '/register' : '/login'
}

function normalizeRoute(path) {
  if (path === '/register' || path === '/dashboard') {
    return path
  }
  return '/login'
}
</script>
