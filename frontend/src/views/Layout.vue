<template>
  <el-container style="height:100vh">
    <el-aside width="220px" background="#2f4050">
      <div style="padding: 20px;">
        <h3 style="color:#fff;text-align:center;padding:10px 0">🏪 进销存系统</h3>
        <div v-if="isAdmin && adminType" style="text-align:center; color:#409EFF; font-size:13px; padding-bottom:10px; border-bottom:1px solid #3d5067;">
          {{ adminType }}
        </div>
      </div>
      <el-menu router background="#2f4050" text-color="#fff" active-text-color="#409EFF">
        <el-menu-item index="/layout/user-info">👤 个人信息</el-menu-item>
        <el-menu-item index="/layout/goods" v-if="role=='0'">📦 商品信息</el-menu-item>
        <el-menu-item index="/layout/purchase" v-if="role=='0'">🧾 采购信息</el-menu-item>

        <el-menu-item index="/layout/supplier" v-if="canManageSupplier">🏢 供应商管理</el-menu-item>
        <el-menu-item index="/layout/goods" v-if="canManageGoods">📦 商品管理</el-menu-item>
        <el-menu-item index="/layout/employee" v-if="canManageEmployee">👨‍💼 员工管理</el-menu-item>
        <el-menu-item index="/layout/member" v-if="role=='1'">👥 会员管理</el-menu-item>
        <el-menu-item index="/layout/purchase" v-if="canManagePurchase">🧾 采购管理</el-menu-item>
        <el-menu-item index="/layout/supplier" v-if="role=='1' && !canManageSupplier && canViewSupplier">👁️ 供应商管理</el-menu-item>
        <el-menu-item index="/layout/goods" v-if="role=='1' && !canManageGoods && canViewGoods">👁️ 商品管理</el-menu-item>
        <el-menu-item index="/layout/employee" v-if="role=='1' && !canManageEmployee && canViewEmployee">👁️ 员工管理</el-menu-item>
        <el-menu-item index="/layout/purchase" v-if="role=='1' && !canManagePurchase && canViewPurchase">👁️ 采购管理</el-menu-item>

        <el-menu-item style="margin-top:30px" @click="logout">🚪 退出登录</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="height:60px; background:#fff; border-bottom:1px solid #eee; display:flex; align-items:center; justify-content:flex-end; padding:0 20px;">
        <el-avatar :size="36" :src="avatarUrl" style="cursor:pointer; margin-right:8px" @click="goToProfile">
          {{ currentUserName ? currentUserName.charAt(0) : '?' }}
        </el-avatar>
        <span style="cursor:pointer; color:#333" @click="goToProfile">{{ currentUserName }}</span>
      </el-header>
      <el-main style="background: rgba(255, 255, 255, 0.30); min-height: calc(100vh - 60px);"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { checkHasEmployees, getUserById } from '@/api/user'

const router = useRouter()
const role = ref('')
const adminLevel = ref(0)
const avatarUrl = ref('')
const currentUserName = ref('')

const goToProfile = () => {
  if (router.currentRoute.value.path !== '/layout/user-info') {
    router.push('/layout/user-info')
  }
}

const loadAvatar = async () => {
  // 优先读取缓存的头像（UserInfo页面保存后同步的）
  const cached = localStorage.getItem('avatarCache')
  if (cached) avatarUrl.value = cached

  const userId = localStorage.getItem('userId')
  if (userId) {
    try {
      const res = await getUserById(userId)
      if (res.code === 200 && res.data) {
        currentUserName.value = res.data.realName || ''
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

  // 检查员工表是否为空
  if (role.value === '0') {
    try {
      const res = await checkHasEmployees()
      if (res.code !== 200) {
        alert(res.msg)
        localStorage.clear()
        router.push('/')
      }
    } catch (error) {
      console.error('检查员工表失败:', error)
    }
  }
})

const logout = async () => {
  if (adminLevel.value === 1 || adminLevel.value === 3) {
    const confirmed = confirm('是否有新的员工需要录入？\n\n选择"确定"将跳转到员工管理页面\n选择"取消"将直接退出')
    if (confirmed) {
      router.push('/layout/employee')
      return
    }
  }
  localStorage.clear()
  router.push('/')
}
</script>