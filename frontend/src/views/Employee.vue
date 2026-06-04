<template>
  <div class="p-4">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
      <h3>员工管理</h3>
      <div v-if="isAdmin && adminType" style="color:#409EFF; font-size:14px; font-weight:bold;">
        当前身份：{{ adminType }}
      </div>
    </div>
    <div style="display: flex; gap: 10px; margin-bottom: 15px;">
      <el-button type="primary" @click="openAdd" v-if="hasManagePermission">新增</el-button>
      <el-button @click="handleImport" v-if="hasManagePermission">导入Excel</el-button>
      <el-button @click="handleExport">导出Excel</el-button>
      <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="onFileSelect" />
    </div>
    <el-table :data="list" border style="margin-top:15px;">
      <el-table-column label="编号" prop="id"/>
      <el-table-column label="账号" prop="username"/>
      <el-table-column label="姓名" prop="realName"/>
      <el-table-column label="电话" prop="phone"/>
      <el-table-column label="工资" prop="salary"/>
      <el-table-column label="角色">
        <template #default="scope">{{ scope.row.role === 1 ? '管理员' : '普通用户' }}</template>
      </el-table-column>
      <el-table-column label="备注" prop="remark"/>
      <el-table-column label="创建时间" prop="createTime"/>
      <el-table-column label="操作" v-if="hasManagePermission">
        <template #default="scope">
          <el-button @click="openEdit(scope.row)">编辑</el-button>
          <el-button type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="show" title="员工" @close="show=false">
      <el-form :model="form">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="管理员6位(11/10/01开头)，普通用户9位(00开头)" @input="truncateUsername" />
          <div v-if="usernameError" class="error-text">{{ usernameError }}</div>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" show-password placeholder="至少8位，含大小写字母、数字、特殊字符" />
          <div class="password-strength">
            <div class="strength-label">密码强度：</div>
            <div class="strength-bars">
              <div class="bar" :class="{ weak: passwordStrength >= 1, medium: passwordStrength >= 2, strong: passwordStrength >= 3, veryStrong: passwordStrength >= 4 }"></div>
              <div class="bar" :class="{ weak: passwordStrength >= 1, medium: passwordStrength >= 2, strong: passwordStrength >= 3, veryStrong: passwordStrength >= 4 }"></div>
              <div class="bar" :class="{ medium: passwordStrength >= 2, strong: passwordStrength >= 3, veryStrong: passwordStrength >= 4 }"></div>
              <div class="bar" :class="{ strong: passwordStrength >= 3, veryStrong: passwordStrength >= 4 }"></div>
            </div>
            <div class="strength-text" :class="passwordStrengthClass">{{ passwordStrengthText }}</div>
            <div class="password-hints">
              <div :class="{ valid: hasLowercase }">✓ 包含小写字母</div>
              <div :class="{ valid: hasUppercase }">✓ 包含大写字母</div>
              <div :class="{ valid: hasNumber }">✓ 包含数字</div>
              <div :class="{ valid: hasSpecialChar }">✓ 包含特殊字符(@$!%*?&)</div>
              <div :class="{ valid: form.password.length >= 8 }">✓ 至少8位</div>
            </div>
          </div>
          <div v-if="passwordError" class="error-text">{{ passwordError }}</div>
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="电话">
          <div class="phone-input">
            <el-select v-model="form.areaCode" placeholder="区号" style="width: 100px; margin-right: 10px">
              <el-option v-for="area in areaCodes" :key="area.code" :label="area.code" :value="area.code" />
            </el-select>
            <el-input v-model="form.phoneNum" :placeholder="'手机号 (' + getPhoneLengthHint() + ')'" />
          </div>
          <div v-if="phoneError" class="error-text">{{ phoneError }}</div>
        </el-form-item>
        <el-form-item label="工资">
          <el-input v-model="form.salary" type="number" placeholder="请输入工资" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role">
            <el-option label="管理员" :value="1"/>
            <el-option label="普通用户" :value="2"/>
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="show=false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getUserList, addUser, updateUser, deleteUser, importUser, exportUser } from '@/api/user'

const list = ref([])
const show = ref(false)
const form = ref({ role: 2, areaCode: '+86' })
const isEdit = ref(false)
const fileInput = ref(null)
const role = ref('')
const adminLevel = ref(0)

const areaCodes = [
  { code: '+86', label: '+86 中国大陆' },
  { code: '+852', label: '+852 中国香港' },
  { code: '+853', label: '+853 中国澳门' },
  { code: '+886', label: '+886 中国台湾' },
  { code: '+81', label: '+81 日本' },
  { code: '+82', label: '+82 韩国' },
  { code: '+65', label: '+65 新加坡' },
  { code: '+66', label: '+66 泰国' },
  { code: '+60', label: '+60 马来西亚' },
  { code: '+84', label: '+84 越南' },
  { code: '+91', label: '+91 印度' },
  { code: '+971', label: '+971 阿联酋' },
  { code: '+966', label: '+966 沙特' },
  { code: '+62', label: '+62 印尼' },
  { code: '+63', label: '+63 菲律宾' },
  { code: '+1', label: '+1 美国/加拿大' },
  { code: '+7', label: '+7 俄罗斯' },
  { code: '+44', label: '+44 英国' },
  { code: '+49', label: '+49 德国' },
  { code: '+33', label: '+33 法国' },
  { code: '+39', label: '+39 意大利' },
  { code: '+34', label: '+34 西班牙' },
  { code: '+41', label: '+41 瑞士' },
  { code: '+46', label: '+46 瑞典' },
  { code: '+47', label: '+47 挪威' },
  { code: '+61', label: '+61 澳大利亚' },
  { code: '+64', label: '+64 新西兰' },
  { code: '+55', label: '+55 巴西' },
  { code: '+54', label: '+54 阿根廷' }
]

const isAdmin = computed(() => role.value === '1')

const adminType = computed(() => {
  if (adminLevel.value === 1) return '一号管理员'
  if (adminLevel.value === 2) return '二号管理员'
  if (adminLevel.value === 3) return '三号管理员'
  return ''
})

const hasManagePermission = computed(() => {
  return role.value === '1' && (adminLevel.value === 1 || adminLevel.value === 3)
})

const usernameError = computed(() => {
  const username = form.value.username || ''
  const role = form.value.role
  
  if (!username) return ''
  
  if (role === 1) {
    if (username.length !== 6) return '管理员账号必须是6位'
    if (!['11', '10', '01'].includes(username.substring(0, 2))) return '管理员账号必须以11、10或01开头'
  } else {
    if (username.length !== 9) return '普通用户账号必须是9位'
    if (!username.startsWith('00')) return '普通用户账号必须以00开头'
  }
  return ''
})

const hasLowercase = computed(() => /[a-z]/.test(form.value.password))
const hasUppercase = computed(() => /[A-Z]/.test(form.value.password))
const hasNumber = computed(() => /\d/.test(form.value.password))
const hasSpecialChar = computed(() => /[@$!%*?&]/.test(form.value.password))

const passwordStrength = computed(() => {
  let strength = 0
  if (form.value.password.length >= 8) strength++
  if (hasLowercase.value) strength++
  if (hasUppercase.value) strength++
  if (hasNumber.value) strength++
  if (hasSpecialChar.value) strength++
  return Math.min(strength, 4)
})

const passwordStrengthClass = computed(() => {
  switch (passwordStrength.value) {
    case 0: return 'weak'
    case 1: return 'weak'
    case 2: return 'medium'
    case 3: return 'strong'
    case 4: return 'veryStrong'
    default: return ''
  }
})

const passwordStrengthText = computed(() => {
  switch (passwordStrength.value) {
    case 0: return '请输入密码'
    case 1: return '弱'
    case 2: return '中等'
    case 3: return '强'
    case 4: return '非常强'
    default: return ''
  }
})

const passwordError = computed(() => {
  const pwd = form.value.password || ''
  if (!pwd) return ''
  if (pwd.length < 8) return '密码至少需要8位'
  if (!/[a-z]/.test(pwd)) return '密码必须包含小写字母'
  if (!/[A-Z]/.test(pwd)) return '密码必须包含大写字母'
  if (!/\d/.test(pwd)) return '密码必须包含数字'
  if (!/[@$!%*?&]/.test(pwd)) return '密码必须包含特殊字符(@$!%*?&)'
  return ''
})

const getPhoneLengthHint = () => {
  const hints = {
    '+86': '11位，1开头',
    '+852': '8位，5/6/9开头',
    '+853': '8位，6开头',
    '+886': '10位，09开头',
    '+81': '10-11位',
    '+82': '10-11位',
    '+65': '8位',
    '+66': '10位',
    '+60': '10位',
    '+84': '10位',
    '+91': '10位',
    '+971': '9位',
    '+966': '9位',
    '+62': '10-12位',
    '+63': '10位',
    '+1': '10位',
    '+7': '10位',
    '+44': '11位',
    '+49': '10-11位',
    '+33': '9位',
    '+39': '10位',
    '+34': '9位',
    '+41': '9位',
    '+46': '9位',
    '+47': '8位',
    '+61': '9位',
    '+64': '8-9位',
    '+55': '11位',
    '+54': '10位'
  }
  return hints[form.value.areaCode] || ''
}

const phoneError = computed(() => {
  const phone = form.value.phoneNum || ''
  const code = form.value.areaCode
  
  if (!phone) return ''
  
  const rules = {
    '+86': { length: 11, pattern: /^1/ },
    '+852': { length: 8, pattern: /^[569]/ },
    '+853': { length: 8, pattern: /^6/ },
    '+886': { length: 10, pattern: /^09/ },
    '+81': { minLength: 10, maxLength: 11 },
    '+82': { minLength: 10, maxLength: 11 },
    '+65': { length: 8 },
    '+66': { length: 10 },
    '+60': { length: 10 },
    '+84': { length: 10 },
    '+91': { length: 10 },
    '+971': { length: 9 },
    '+966': { length: 9 },
    '+62': { minLength: 10, maxLength: 12 },
    '+63': { length: 10 },
    '+1': { length: 10 },
    '+7': { length: 10 },
    '+44': { length: 11 },
    '+49': { minLength: 10, maxLength: 11 },
    '+33': { length: 9 },
    '+39': { length: 10 },
    '+34': { length: 9 },
    '+41': { length: 9 },
    '+46': { length: 9 },
    '+47': { length: 8 },
    '+61': { length: 9 },
    '+64': { minLength: 8, maxLength: 9 },
    '+55': { length: 11 },
    '+54': { length: 10 }
  }
  
  const rule = rules[code]
  if (!rule) return ''
  
  if (rule.length && phone.length !== rule.length) {
    return `手机号必须是${rule.length}位`
  }
  if (rule.minLength && phone.length < rule.minLength) {
    return `手机号至少${rule.minLength}位`
  }
  if (rule.maxLength && phone.length > rule.maxLength) {
    return `手机号最多${rule.maxLength}位`
  }
  if (rule.pattern && !rule.pattern.test(phone)) {
    return getPhoneLengthHint()
  }
  return ''
})

const truncateUsername = () => {
  const maxLength = form.value.role === 1 ? 6 : 9
  if (form.value.username && form.value.username.length > maxLength) {
    form.value.username = form.value.username.substring(0, maxLength)
  }
}

onMounted(() => {
  role.value = localStorage.getItem('role') || ''
  adminLevel.value = parseInt(localStorage.getItem('adminLevel') || '0')
  loadData()
})

const loadData = async () => {
  const res = await getUserList()
  if (res.code === 200) list.value = res.data
}

const openAdd = () => { 
  form.value = { role: 2, areaCode: '+86' }
  isEdit.value = false
  show.value = true 
}

const openEdit = (row) => { 
  const phone = row.phone || ''
  const phoneParts = phone.split('|')
  form.value = { 
    ...row, 
    areaCode: phoneParts[0] || '+86',
    phoneNum: phoneParts[1] || ''
  }
  isEdit.value = true 
  show.value = true 
}

const submit = async () => {
  if (usernameError.value) {
    alert(usernameError.value)
    return
  }
  if (passwordError.value && !isEdit.value) {
    alert(passwordError.value)
    return
  }
  if (phoneError.value) {
    alert(phoneError.value)
    return
  }
  
  const submitData = {
    ...form.value,
    phone: form.value.areaCode + '|' + form.value.phoneNum
  }
  
  if (isEdit.value) {
    const res = await updateUser(submitData)
    alert(res.msg)
  } else {
    const res = await addUser(submitData)
    alert(res.msg)
  }
  show.value = false
  loadData()
}

const handleDelete = async (id) => {
  if (confirm('确定删除？')) {
    const res = await deleteUser(id)
    alert(res.msg)
    loadData()
  }
}

const handleImport = () => {
  fileInput.value.click()
}

const onFileSelect = async (event) => {
  const file = event.target.files[0]
  if (file) {
    const res = await importUser(file)
    alert(res.msg)
    loadData()
    event.target.value = ''
  }
}

const handleExport = async () => {
  const res = await exportUser()
  const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '员工数据.xlsx'
  document.body.appendChild(a)
  a.click()
  window.URL.revokeObjectURL(url)
  document.body.removeChild(a)
}
</script>

<style scoped>
.error-text {
  color: #ff4757 !important;
  font-size: 12px;
  margin-top: 5px;
}

.password-strength {
  margin-top: 5px;
}

.strength-label {
  font-size: 12px;
  color: #666 !important;
  margin-bottom: 4px;
}

.strength-bars {
  display: flex;
  gap: 3px;
  margin-bottom: 4px;
}

.bar {
  flex: 1;
  height: 5px;
  background: #ddd;
  border-radius: 3px;
  transition: all 0.3s ease;
}

.bar.weak { background: #ff4757; }
.bar.medium { background: #ffa502; }
.bar.strong { background: #2ed573; }
.bar.veryStrong { background: #1dd1a1; }

.strength-text {
  font-size: 12px;
  font-weight: bold;
  margin-bottom: 4px;
}

.strength-text.weak { color: #ff4757 !important; }
.strength-text.medium { color: #ffa502 !important; }
.strength-text.strong { color: #2ed573 !important; }
.strength-text.veryStrong { color: #1dd1a1 !important; }

.password-hints {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 10px;
}

.password-hints div {
  color: #999 !important;
}

.password-hints div.valid {
  color: #2ed573 !important;
}

.phone-input {
  display: flex;
}
</style>
