<template>
  <div class="p-4">
    <h3>个人信息</h3>

    <!-- 头像区域 -->
    <div style="display:flex; align-items:center; gap:20px; margin:20px 0;">
      <el-avatar :size="80" :src="avatarUrl" style="cursor:pointer" @click="triggerUpload">
        {{ user.realName ? user.realName.charAt(0) : '?' }}
      </el-avatar>
      <div>
        <el-button size="small" @click="triggerUpload">更换头像</el-button>
        <input ref="avatarInput" type="file" accept="image/*" style="display:none" @change="onAvatarSelect" />
        <div style="color:#909399; font-size:12px; margin-top:4px">点击头像或按钮上传</div>
      </div>
    </div>

    <el-form :model="form" label-width="100px" style="max-width:500px">
      <el-form-item label="员工编号">
        <el-input v-model="form.username" disabled />
        <span style="color:#909399;font-size:12px">员工编号不可修改</span>
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="form.realName" />
      </el-form-item>
      <el-form-item label="性别">
        <el-radio-group v-model="form.gender">
          <el-radio label="男">男</el-radio>
          <el-radio label="女">女</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="年龄">
        <el-input-number v-model="form.age" :min="1" :max="150" />
      </el-form-item>
      <el-form-item label="住址">
        <div style="display:flex; align-items:center; gap:8px; margin-bottom:6px">
          <el-switch v-model="useRegionPicker" active-text="省市区选择" inactive-text="手动输入" size="small" />
        </div>
        <template v-if="useRegionPicker">
          <el-cascader
            v-model="selectedRegion"
            :options="regionOptions"
            :props="{ expandTrigger: 'hover' }"
            placeholder="请选择省/市/区"
            clearable
            filterable
            style="width:100%; margin-bottom:6px"
            @change="onRegionChange"
          />
          <el-input v-model="addressDetail" placeholder="乡镇/街道-小区-门牌号" />
        </template>
        <el-input v-else v-model="form.address" placeholder="请输入完整住址" />
      </el-form-item>
      <el-form-item label="电话">
        <span>{{ user.phone || '-' }}</span>
      </el-form-item>
      <el-form-item label="工资">
        <span>{{ user.salary != null ? user.salary : '-' }}</span>
      </el-form-item>
      <el-form-item label="身份">
        <el-tag>{{ role === '1' ? (user.username ? user.username.substring(0,2) : '') + '管理员' : '普通用户' }}</el-tag>
      </el-form-item>
      <el-form-item label="会员等级" v-if="memberLevel">
        <el-tag :type="memberLevel === 'SVIP' ? 'danger' : memberLevel === 'VIP' ? 'warning' : 'info'">{{ memberLevel }}</el-tag>
        <span style="color:#909399;font-size:12px;margin-left:8px">会员等级不可修改</span>
      </el-form-item>

      <el-divider>修改密码（选填）</el-divider>
      <el-form-item label="旧密码">
        <el-input v-model="oldPassword" type="password" show-password placeholder="如需修改密码请输入旧密码" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="newPassword" type="password" show-password placeholder="至少8位，含字母+数字+特殊字符" />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input v-model="confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="submit">保存修改</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { getUserById, updateProfile } from '@/api/user'
import { getMemberList as apiGetMemberList } from '@/api/member'
import { regionData } from 'element-china-area-data'

const role = ref(localStorage.getItem('role') || '')
const user = ref({})
const form = ref({})
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const avatarUrl = ref('')
const memberLevel = ref('')
const avatarInput = ref(null)
const useRegionPicker = ref(true)
const selectedRegion = ref([])
const addressDetail = ref('')
const regionOptions = regionData

const onRegionChange = (value) => {
  // value is array of region codes, labels are auto-resolved by cascader
  const labels = getRegionLabels(value)
  form.value.address = labels.join(' ') + (addressDetail.value ? ' ' + addressDetail.value : '')
}

const getRegionLabels = (codes) => {
  if (!codes || codes.length === 0) return []
  const labels = []
  let options = regionOptions
  for (const code of codes) {
    const found = options.find(o => o.value === code)
    if (found) {
      labels.push(found.label)
      options = found.children || []
    }
  }
  return labels
}

// 监听详细地址变化，更新完整地址
watch(addressDetail, (val) => {
  if (useRegionPicker.value && selectedRegion.value.length > 0) {
    const labels = getRegionLabels(selectedRegion.value)
    form.value.address = labels.join(' ') + (val ? ' ' + val : '')
  }
})

const loadUserInfo = async () => {
  const userId = localStorage.getItem('userId')
  if (userId) {
    const res = await getUserById(userId)
    if (res.code === 200) {
      user.value = res.data
      form.value = { ...res.data }
      // 已有地址时默认显示手动输入模式
      if (res.data.address) {
        useRegionPicker.value = false
      }
      if (res.data.avatar) {
        avatarUrl.value = res.data.avatar
      }
      // 加载会员等级
      try {
        const memberRes = await apiGetMemberList()
        if (memberRes.code === 200) {
          const username = localStorage.getItem('username')
          const found = memberRes.data.find(m => m.memberNo === 'M' + username)
          if (found) memberLevel.value = found.level
        }
      } catch (e) { /* ignore */ }
    }
  }
}

const triggerUpload = () => {
  avatarInput.value.click()
}

const onAvatarSelect = (event) => {
  const file = event.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (e) => {
    avatarUrl.value = e.target.result
  }
  reader.readAsDataURL(file)
  event.target.value = ''
}

const submit = async () => {
  if (newPassword.value && newPassword.value !== confirmPassword.value) {
    alert('两次输入的新密码不一致')
    return
  }
  if (newPassword.value && !oldPassword.value) {
    alert('修改密码需要输入旧密码')
    return
  }

  const submitData = {
    ...form.value,
    avatar: avatarUrl.value || null,
    password: newPassword.value || null
  }

  const userId = localStorage.getItem('userId')
  const res = await updateProfile(submitData, userId, oldPassword.value || '', confirmPassword.value || '')
  alert(res.msg)
  if (res.code === 200) {
    // 同步头像到 localStorage，使右上角头像即时更新
    if (avatarUrl.value) {
      localStorage.setItem('avatarCache', avatarUrl.value)
    }
    oldPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    loadUserInfo()
  }
}

onMounted(loadUserInfo)
</script>