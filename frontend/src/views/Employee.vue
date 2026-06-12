<template>
  <div class="p-4">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
      <h3>员工管理</h3>
      <div v-if="isAdmin && adminType" style="color:#409EFF; font-size:14px; font-weight:bold;">
        当前身份：{{ adminType }}
      </div>
    </div>
    <div class="toolbar">
      <el-input
        v-model="searchText"
        placeholder="搜索姓名 / 员工编号 / 电话..."
        clearable
        size="small"
        style="width: 240px; margin-right: 8px"
        @input="currentPage = 1"
      />
      <el-button type="primary" @click="openAdd" v-if="hasManagePermission" size="small">新增</el-button>
      <el-button @click="handleImport" v-if="hasManagePermission" size="small">导入Excel</el-button>
      <el-button @click="handleExport" size="small">导出Excel</el-button>
      <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="onFileSelect" />
    </div>
    <div class="table-wrap">
    <el-table :data="pagedList" border size="small">
      <el-table-column label="编号" type="index" width="60"/>
      <el-table-column label="员工编号" prop="username"/>
      <el-table-column label="姓名">
        <template #default="scope">
          <el-button link type="primary" @click="viewDetail(scope.row)" style="text-decoration: underline">{{ scope.row.realName || '-' }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="电话" prop="phone"/>
      <el-table-column label="工资" prop="salary"/>
      <el-table-column label="员工级别">
        <template #default="scope">{{ scope.row.role === 1 ? '管理员' : '普通用户' }}</template>
      </el-table-column>
      <el-table-column label="备注" prop="remark"/>
      <el-table-column label="创建时间">
        <template #default="scope">
          <span>{{ formatDate(scope.row.createTime) }}</span><br/>
          <span style="color:#909399; font-size:12px">{{ formatTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" v-if="hasManagePermission">
        <template #default="scope">
          <el-button @click="openEdit(scope.row)" v-if="canEditRow(scope.row)">编辑</el-button>
          <el-button type="danger" @click="handleDelete(scope.row.id)" v-if="canEditRow(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="list.length > pageSize"
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="filteredList.length"
      layout="prev, pager, next"
      style="margin-top:15px; justify-content:center"
    />
    </div>

    <el-dialog v-model="show" title="员工" @close="show=false">
      <el-form :model="form">
        <el-form-item label="员工编号">
          <el-input v-model="form.username" :placeholder="isEdit ? '' : '9位员工编号(00开头)'" @input="truncateUsername" :disabled="isEdit" />
          <div v-if="usernameError" class="error-text">{{ usernameError }}</div>
        </el-form-item>

        <!-- 编辑模式才显示以下字段 -->
        <template v-if="isEdit">
          <el-form-item label="姓名">
            <el-input v-model="form.realName" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="电话">
            <div class="phone-input">
              <el-select v-model="form.areaCode" placeholder="区号" style="width: 100px; margin-right: 10px; color: #409EFF; font-weight: bold">
                <el-option v-for="area in areaCodes" :key="area.code" :label="area.code" :value="area.code" style="color: #409EFF" />
              </el-select>
              <el-input v-model="form.phoneNum" :placeholder="'手机号 (' + getPhoneLengthHint() + ')'" />
            </div>
            <div v-if="phoneError" class="error-text">{{ phoneError }}</div>
          </el-form-item>
        </template>

        <el-form-item label="工资">
          <el-input-number v-model="form.salary" :min="100" :step="100" placeholder="请输入工资" style="width:100%" />
        </el-form-item>
        <el-form-item label="会员等级">
          <el-select v-model="form.memberLevel" placeholder="请选择会员等级" clearable style="width:100%">
            <el-option label="无（不分配会员）" value="" />
            <el-option label="普通会员" value="普通会员" />
            <el-option label="VIP" value="VIP" />
            <el-option label="SVIP" value="SVIP" />
          </el-select>
        </el-form-item>
        <el-form-item label="员工级别">
          <span style="color:#333">普通用户</span>
          <span style="color:#909399; font-size:12px; margin-left:8px">管理员需自行注册，此处仅可添加普通员工</span>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark"/></el-form-item>

        <div v-if="!isEdit" style="color:#E6A23C; font-size:12px; padding:8px; background:#fdf6ec; border-radius:4px; margin-bottom:10px">
          ⚠ 新增员工只需填写员工编号，姓名、密码、电话留空，等员工自行注册时填写
        </div>
      </el-form>
      <template #footer>
        <el-button @click="show=false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 员工详情弹窗 -->
    <el-dialog v-model="detailShow" title="员工详细信息" width="520px">
      <template v-if="detailUser">
        <div style="text-align:center; margin-bottom:20px">
          <el-avatar :size="72" :src="detailUser.avatar">
            {{ detailUser.realName ? detailUser.realName.charAt(0) : '?' }}
          </el-avatar>
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="员工编号">{{ detailUser.username }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ detailUser.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ detailUser.gender || '-' }}</el-descriptions-item>
          <el-descriptions-item label="年龄">{{ detailUser.age || '-' }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ detailUser.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="工资">{{ detailUser.salary != null ? detailUser.salary : '-' }}</el-descriptions-item>
          <el-descriptions-item label="级别">
            <el-tag :type="detailUser.role === 1 ? 'danger' : 'info'" size="small">
              {{ detailUser.role === 1 ? '管理员' : '普通用户' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="会员等级">
            <el-tag v-if="detailUser.memberLevel" :type="detailUser.memberLevel === 'SVIP' ? 'danger' : detailUser.memberLevel === 'VIP' ? 'warning' : 'info'" size="small">
              {{ detailUser.memberLevel }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="住址" :span="2">{{ detailUser.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailUser.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ detailUser.createTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="detailShow = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getUserList, addUser, updateUser, deleteUser, importUser, exportUser } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const searchText = ref('')
const filteredList = computed(() => {
  const kw = searchText.value.trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter(row =>
    (row.realName && row.realName.toLowerCase().includes(kw)) ||
    (row.username && row.username.toLowerCase().includes(kw)) ||
    (row.phone && row.phone.includes(kw))
  )
})
const pagedList = computed(() => filteredList.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value))
const show = ref(false)
const detailShow = ref(false)
const detailUser = ref(null)
const form = ref({ role: 0, areaCode: '+86', password: '' })
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

const currentUserId = ref(null)

// 11管理员可以编辑所有员工，01管理员只能编辑普通用户
const canEditRow = (row) => {
  if (adminLevel.value === 1) return true  // 一号管理员可编辑所有人
  if (adminLevel.value === 3) {
    // 三号管理员只能编辑普通用户(role=0)
    return row.role === 0
  }
  return false
}

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
  currentUserId.value = parseInt(localStorage.getItem('userId') || '0')
  loadData()
})

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `${h}:${min}:${s}`
}

const loadData = async () => {
  const res = await getUserList()
  if (res.code === 200) list.value = res.data
}

const openAdd = () => {
  form.value = { role: 0, salary: null, remark: '', username: '', memberLevel: '', areaCode: '+86', phoneNum: '' }
  isEdit.value = false
  show.value = true
}

const viewDetail = (row) => {
  detailUser.value = row
  detailShow.value = true
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
    ElMessage.warning(usernameError.value)
    return
  }

  if (isEdit.value) {
    if (phoneError.value) {
      ElMessage.warning(phoneError.value)
      return
    }
  }

  // 只有填写了电话号码才拼接
  const phoneStr = (form.value.phoneNum && form.value.phoneNum.trim())
    ? (form.value.areaCode || '+86') + '|' + form.value.phoneNum.trim()
    : null

  const submitData = {
    ...form.value,
    phone: phoneStr,
    memberLevel: form.value.memberLevel || null  // 空字符串转为 null
  }

  // 新增员工：密码和电话都留空，由员工注册时自行设置
  if (!isEdit.value) {
    submitData.password = null
    submitData.phone = null
  }

  if (isEdit.value) {
    const res = await updateUser(submitData)
    ElMessage.info(res.msg)
  } else {
    const res = await addUser(submitData)
    ElMessage.info(res.msg)
  }
  show.value = false
  loadData()
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除？', '确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const res = await deleteUser(id)
    ElMessage.info(res.msg)
    loadData()
  } catch { /* 取消 */ }
}

const handleImport = () => {
  fileInput.value.click()
}

const onFileSelect = async (event) => {
  const file = event.target.files[0]
  if (file) {
    const res = await importUser(file)
    ElMessage.info(res.msg)
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

.phone-input {
  display: flex;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

/* 手机端适配 */
@media (max-width: 767px) {
  .toolbar .el-button {
    font-size: 12px;
    padding: 6px 10px;
  }

  h3 {
    font-size: 16px;
  }
}
</style>
