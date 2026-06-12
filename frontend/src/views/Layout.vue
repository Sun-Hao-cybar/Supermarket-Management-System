<template>
  <el-container style="height:100vh">
    <!-- 移动端遮罩 -->
    <div v-if="mobileMenuOpen" class="mobile-overlay" @click="closeMenu" />

    <!-- 侧边栏 -->
    <el-aside :class="['layout-sidebar', { 'sidebar-open': mobileMenuOpen }]">
      <!-- Logo -->
      <router-link to="/layout/user-info" class="sidebar-logo" @click="closeMenu">
        <span class="logo-icon">🏪</span>
        <span class="logo-text">
          <span class="logo-title">进销存系统</span>
          <span v-if="isAdmin && adminType" class="logo-subtitle">{{ adminType }}</span>
        </span>
      </router-link>

      <!-- 分割线 -->
      <div class="sidebar-divider"></div>

      <!-- 导航菜单 -->
      <nav class="sidebar-nav">
        <router-link to="/layout/user-info" class="nav-item" :class="{ active: route.path === '/layout/user-info' }" @click="closeMenu">
          <span class="nav-icon">👤</span>
          <span class="nav-label">个人信息</span>
        </router-link>

        <template v-if="role=='0'">
          <router-link to="/layout/goods" class="nav-item" :class="{ active: route.path === '/layout/goods' }" @click="closeMenu">
            <span class="nav-icon">📦</span>
            <span class="nav-label">商品信息</span>
          </router-link>
          <router-link to="/layout/purchase" class="nav-item" :class="{ active: route.path === '/layout/purchase' }" @click="closeMenu">
            <span class="nav-icon">🧾</span>
            <span class="nav-label">采购信息</span>
          </router-link>
        </template>

        <router-link v-if="canManageSupplier" to="/layout/supplier" class="nav-item" :class="{ active: route.path === '/layout/supplier' }" @click="closeMenu">
          <span class="nav-icon">🏢</span>
          <span class="nav-label">供应商管理</span>
        </router-link>
        <router-link v-if="canManageGoods" to="/layout/goods" class="nav-item" :class="{ active: route.path === '/layout/goods' }" @click="closeMenu">
          <span class="nav-icon">📦</span>
          <span class="nav-label">商品管理</span>
        </router-link>
        <router-link v-if="canManageEmployee" to="/layout/employee" class="nav-item" :class="{ active: route.path === '/layout/employee' }" @click="closeMenu">
          <span class="nav-icon">👨‍💼</span>
          <span class="nav-label">员工管理</span>
        </router-link>
        <router-link v-if="role=='1'" to="/layout/member" class="nav-item" :class="{ active: route.path === '/layout/member' }" @click="closeMenu">
          <span class="nav-icon">👥</span>
          <span class="nav-label">会员管理</span>
        </router-link>
        <router-link v-if="canManagePurchase" to="/layout/purchase" class="nav-item" :class="{ active: route.path === '/layout/purchase' }" @click="closeMenu">
          <span class="nav-icon">🧾</span>
          <span class="nav-label">采购管理</span>
        </router-link>

        <!-- 只读菜单项 -->
        <router-link v-if="role=='1' && !canManageSupplier && canViewSupplier" to="/layout/supplier" class="nav-item readonly" :class="{ active: route.path === '/layout/supplier' }" @click="closeMenu">
          <span class="nav-icon">👁️</span>
          <span class="nav-label">供应商管理</span>
        </router-link>
        <router-link v-if="role=='1' && !canManageGoods && canViewGoods" to="/layout/goods" class="nav-item readonly" :class="{ active: route.path === '/layout/goods' }" @click="closeMenu">
          <span class="nav-icon">👁️</span>
          <span class="nav-label">商品管理</span>
        </router-link>
        <router-link v-if="role=='1' && !canManageEmployee && canViewEmployee" to="/layout/employee" class="nav-item readonly" :class="{ active: route.path === '/layout/employee' }" @click="closeMenu">
          <span class="nav-icon">👁️</span>
          <span class="nav-label">员工管理</span>
        </router-link>
        <router-link v-if="role=='1' && !canManagePurchase && canViewPurchase" to="/layout/purchase" class="nav-item readonly" :class="{ active: route.path === '/layout/purchase' }" @click="closeMenu">
          <span class="nav-icon">👁️</span>
          <span class="nav-label">采购管理</span>
        </router-link>
      </nav>

      <!-- 退出登录 -->
      <div class="sidebar-footer">
        <div class="sidebar-divider"></div>
        <a class="nav-item logout" @click="logout">
          <span class="nav-icon">🚪</span>
          <span class="nav-label">退出登录</span>
        </a>
      </div>
    </el-aside>

    <!-- 右侧主体 -->
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-button class="hamburger-btn" text @click="toggleMenu">
            <span style="font-size:22px">☰</span>
          </el-button>
          <span class="header-title">进销存系统</span>
        </div>
        <div class="header-right">
          <el-avatar :size="32" :src="avatarUrl" style="cursor:pointer;margin-right:6px" @click="goToProfile">
            {{ currentUserName ? currentUserName.charAt(0) : '?' }}
          </el-avatar>
          <span class="header-name" @click="goToProfile">{{ currentUserName }}</span>
          <span class="header-extra">
            <span class="header-username">{{ currentUsername }}</span>
            <el-tag :type="role === '1' ? 'danger' : 'info'" size="small" style="margin-left:8px">{{ role === '1' ? '管理员' : '普通用户' }}</el-tag>
            <span v-if="role === '1' && adminType" class="header-admin-type">{{ adminType }}</span>
          </span>
        </div>
      </el-header>
      <el-main class="layout-main"><router-view /></el-main>
    </el-container>
    <!-- 智能体：白色小猫助手 -->
    <CatAgent />
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { checkHasEmployees, getUserById } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import CatAgent from '@/components/cat/CatAgent.vue'

const router = useRouter()
const route = useRoute()
const role = ref('')
const adminLevel = ref(0)
const avatarUrl = ref('')
const currentUserName = ref('')
const currentUsername = ref('')
const mobileMenuOpen = ref(false)

const toggleMenu = () => { mobileMenuOpen.value = !mobileMenuOpen.value }
const closeMenu = () => { mobileMenuOpen.value = false }

// 路由变化时关闭侧边栏
watch(() => route.path, () => { mobileMenuOpen.value = false })

const goToProfile = () => {
  mobileMenuOpen.value = false
  if (router.currentRoute.value.path !== '/layout/user-info') {
    router.push('/layout/user-info')
  }
}

const loadAvatar = async () => {
  const cached = localStorage.getItem('avatarCache')
  if (cached) avatarUrl.value = cached

  const userId = localStorage.getItem('userId')
  if (userId) {
    try {
      const res = await getUserById(userId)
      if (res.code === 200 && res.data) {
        currentUserName.value = res.data.realName || ''
        currentUsername.value = res.data.username || ''
        if (res.data.avatar && !cached) {
          avatarUrl.value = res.data.avatar
        }
      }
    } catch (e) { /* ignore */ }
  }
}

const isAdmin = computed(() => role.value === '1')

const adminType = computed(() => {
  if (adminLevel.value === 1) return '一号管理员'
  if (adminLevel.value === 2) return '二号管理员'
  if (adminLevel.value === 3) return '三号管理员'
  return ''
})

const canManageSupplier = computed(() => role.value === '1' && (adminLevel.value === 1 || adminLevel.value === 2))
const canManageGoods = computed(() => role.value === '1' && (adminLevel.value === 1 || adminLevel.value === 2))
const canManageEmployee = computed(() => role.value === '1' && (adminLevel.value === 1 || adminLevel.value === 3))
const canManagePurchase = computed(() => role.value === '1' && (adminLevel.value === 1 || adminLevel.value === 3))

const canViewSupplier = computed(() => role.value === '1')
const canViewGoods = computed(() => role.value === '1')
const canViewEmployee = computed(() => role.value === '1')
const canViewPurchase = computed(() => role.value === '1')

onMounted(async () => {
  role.value = localStorage.getItem('role') || ''
  adminLevel.value = parseInt(localStorage.getItem('adminLevel') || '0')
  loadAvatar()

  if (role.value === '0') {
    try {
      const res = await checkHasEmployees()
      if (res.code !== 200) {
        ElMessage.error(res.msg)
        localStorage.clear()
        router.push('/')
      }
    } catch (error) {
      console.error('检查员工表失败:', error)
    }
  }
})

const logout = async () => {
  mobileMenuOpen.value = false
  if (adminLevel.value === 1 || adminLevel.value === 3) {
    try {
      await ElMessageBox.confirm(
        '是否有新的员工需要录入？',
        '退出确认',
        { confirmButtonText: '去录入员工', cancelButtonText: '直接退出', type: 'info' }
      )
      router.push('/layout/employee')
      return
    } catch {
      // 取消 = 直接退出
    }
  }
  localStorage.clear()
  router.push('/')
}
</script>

<style scoped>
/* ========== 侧边栏 - 暖白主题 ========== */
.layout-sidebar {
  position: fixed;
  left: -260px;
  top: 0;
  bottom: 0;
  z-index: 999;
  width: 260px !important;
  background: linear-gradient(180deg, rgba(255, 249, 242, 0.9) 0%, rgba(255, 245, 234, 0.9) 50%, rgba(254, 247, 240, 0.9) 100%);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  display: flex;
  flex-direction: column;
  transition: left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  user-select: none;
  border-right: 1px solid rgba(180, 130, 80, 0.12);
}
.layout-sidebar.sidebar-open {
  left: 0;
}

/* 自定义滚动条 */
.layout-sidebar::-webkit-scrollbar { width: 4px; }
.layout-sidebar::-webkit-scrollbar-thumb { background: rgba(180, 130, 80, 0.2); border-radius: 4px; }

/* ========== Logo 区域 ========== */
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 18px 16px;
  text-decoration: none;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(200, 150, 100, 0.15);
}
.sidebar-logo:hover .logo-icon {
  transform: scale(1.08);
}
.logo-icon {
  font-size: 28px;
  line-height: 1;
  flex-shrink: 0;
  transition: transform 0.2s ease;
}
.logo-text {
  overflow: hidden;
  white-space: nowrap;
}
.logo-title {
  color: #5a3e28;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.5px;
  line-height: 1.3;
}
.logo-subtitle {
  display: block;
  color: #d4843b;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1px;
  margin-top: 2px;
}

/* ========== 分割线 ========== */
.sidebar-divider {
  height: 1px;
  margin: 4px 16px 0;
  background: linear-gradient(90deg, transparent, rgba(200, 150, 100, 0.15) 20%, rgba(200, 150, 100, 0.15) 80%, transparent);
  flex-shrink: 0;
}

/* ========== 导航区域 ========== */
.sidebar-nav {
  flex: 1;
  padding: 6px 0;
  overflow-y: auto;
  overflow-x: hidden;
}
.sidebar-nav::-webkit-scrollbar { width: 4px; }
.sidebar-nav::-webkit-scrollbar-thumb { background: rgba(180, 130, 80, 0.15); border-radius: 4px; }

/* ========== 导航项 ========== */
.nav-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 11px 18px;
  margin: 2px 10px;
  border-radius: 10px;
  text-decoration: none;
  color: #6b4d34;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  white-space: nowrap;
  overflow: hidden;
}
.nav-item:hover {
  background: rgba(220, 160, 90, 0.12);
  color: #5a3e28;
}
.nav-item.active {
  background: linear-gradient(135deg, rgba(240, 160, 60, 0.18), rgba(240, 180, 80, 0.12));
  color: #b8661e;
  font-weight: 600;
}
.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  background: #d4843b;
  border-radius: 0 3px 3px 0;
}

/* 只读菜单项 */
.nav-item.readonly {
  opacity: 0.5;
}
.nav-item.readonly:hover {
  opacity: 0.75;
}

/* 退出登录 */
.sidebar-footer {
  flex-shrink: 0;
  padding: 0 0 12px;
}
.nav-item.logout {
  color: #9b8570;
}
.nav-item.logout:hover {
  background: rgba(220, 80, 60, 0.1);
  color: #c0392b;
}

/* 导航图标 */
.nav-icon {
  font-size: 18px;
  width: 24px;
  text-align: center;
  flex-shrink: 0;
  line-height: 1;
}
.nav-label {
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ========== 移动端遮罩 ========== */
.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  z-index: 998;
  backdrop-filter: blur(2px);
}

/* ========== 顶部栏 ========== */
.layout-header {
  height: 50px !important;
  background: rgba(255, 252, 247, 0.88);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(180, 130, 80, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px !important;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.header-title {
  font-weight: 600;
  font-size: 15px;
  color: #5a3e28;
}
.header-right {
  display: flex;
  align-items: center;
}
.header-name {
  cursor: pointer;
  color: #4a3522;
  font-size: 13px;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.header-extra {
  display: none;
  align-items: center;
  margin-left: 6px;
}
.header-username {
  color: #9b8570;
  font-size: 12px;
  margin-left: 8px;
}
.header-admin-type {
  color: #d4843b;
  font-size: 12px;
  margin-left: 8px;
  font-weight: bold;
}
.hamburger-btn {
  display: inline-flex;
  padding: 4px;
}
.layout-main {
  background: rgba(255, 252, 247, 0.65);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  min-height: calc(100vh - 50px);
  padding: 12px !important;
}

/* ================================================================ */
/* ========== PC 端：侧边栏收缩 + 悬浮展开 ======================== */
/* ================================================================ */
@media (min-width: 768px) {
  .layout-sidebar {
    position: fixed !important;
    left: 0 !important;
    top: 0;
    bottom: 0;
    z-index: 100;
    width: 64px !important;
    transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.3s ease;
  }

  .layout-sidebar:hover {
    width: 230px !important;
    box-shadow: 4px 0 28px rgba(120, 80, 30, 0.15), 1px 0 0 rgba(180, 130, 80, 0.1);
  }

  /* ---- Logo 区域收缩 ---- */
  .sidebar-logo {
    padding: 18px 16px 14px;
    gap: 14px;
    border-bottom: 1px solid rgba(200, 150, 100, 0.12);
  }
  .logo-text {
    opacity: 0;
    transition: opacity 0.2s ease;
    width: 0;
  }
  .layout-sidebar:hover .logo-text {
    opacity: 1;
    width: auto;
  }
  .logo-icon {
    font-size: 26px;
  }

  /* ---- 分割线 ---- */
  .sidebar-divider {
    margin: 2px 14px 0;
    transition: margin 0.3s ease;
  }
  .layout-sidebar:hover .sidebar-divider {
    margin: 2px 16px 0;
  }

  /* ---- 导航项收缩 ---- */
  .nav-item {
    padding: 11px 16px;
    margin: 2px 10px;
    gap: 14px;
    border-radius: 10px;
    justify-content: flex-start;
  }
  .nav-label {
    opacity: 0;
    width: 0;
    transition: opacity 0.2s ease, width 0.01s 0.25s;
  }
  .layout-sidebar:hover .nav-label {
    opacity: 1;
    width: auto;
    transition: opacity 0.25s ease 0.08s, width 0.01s;
  }

  /* 激活指示条 */
  .nav-item.active::before {
    left: -2px;
    transition: left 0.3s ease;
  }
  .layout-sidebar:hover .nav-item.active::before {
    left: 0;
  }

  /* ---- 底部 ---- */
  .sidebar-footer {
    padding: 0 0 14px;
  }

  /* ---- 顶部栏 ---- */
  .hamburger-btn { display: none; }
  .header-title { display: none; }

  .layout-header {
    height: 60px !important;
    padding: 0 20px !important;
    justify-content: flex-end;
    margin-left: 64px;
    transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }
  .header-name { max-width: none; font-size: 14px; }
  .header-extra { display: flex; }

  .layout-main {
    padding: 20px !important;
    margin-left: 64px;
    transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }
}
</style>