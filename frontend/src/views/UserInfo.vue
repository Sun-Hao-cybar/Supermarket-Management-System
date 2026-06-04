<template>
  <div class="p-4">
    <h3>个人信息</h3>
    <el-descriptions :column="1" border style="margin-top:15px">
      <el-descriptions-item label="账号">{{ user.username }}</el-descriptions-item>
      <el-descriptions-item label="姓名">{{ user.realName }}</el-descriptions-item>
      <el-descriptions-item label="电话">{{ user.phone }}</el-descriptions-item>
      <el-descriptions-item label="工资">{{ user.salary }}</el-descriptions-item>
      <el-descriptions-item label="身份">{{ role == 1 ? '管理员' : '普通用户' }}</el-descriptions-item>
      <el-descriptions-item label="备注">{{ user.remark }}</el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserById } from '@/api/user'
const role = ref(localStorage.getItem('role'))
const user = ref({})

const loadUserInfo = async () => {
  const userId = localStorage.getItem('userId')
  if (userId) {
    const res = await getUserById(userId)
    if (res.code === 200) {
      user.value = res.data
    }
  }
}

onMounted(loadUserInfo)
</script>