<template>
  <div class="p-4">
    <h3>会员管理</h3>
    <div style="display: flex; gap: 10px; margin-bottom: 15px;">
      <el-input
        v-model="searchText"
        placeholder="搜索姓名 / 电话 / 会员编号..."
        clearable
        size="small"
        style="width: 240px; margin-right: 8px"
        @input="currentPage = 1"
      />
      <el-button type="primary" @click="openAdd">新增</el-button>
      <el-button @click="handleImport">导入Excel</el-button>
      <el-button @click="handleExport">导出Excel</el-button>
      <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="onFileSelect" />
    </div>
    <div class="table-wrap">
    <el-table :data="pagedList" border size="small">
      <el-table-column label="编号" type="index" width="60"/>
      <el-table-column label="会员编号" prop="memberNo"/>
      <el-table-column label="姓名" prop="name"/>
      <el-table-column label="电话">
        <template #default="scope">
          {{ scope.row.phoneCode || '+86' }}|{{ scope.row.phoneNum }}
        </template>
      </el-table-column>
      <el-table-column label="会员等级">
        <template #default="scope">
          <el-tag :type="scope.row.level === 'SVIP' ? 'danger' : scope.row.level === 'VIP' ? 'warning' : 'info'" size="small">
            {{ isEmployeeMember(scope.row) && scope.row.level ? '员工' + scope.row.level : (scope.row.level || '普通会员') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" prop="registerTimeFormatted"/>
      <el-table-column label="备注" prop="remark"/>
      <el-table-column label="操作" width="150">
        <template #default="scope">
          <el-button @click="openEdit(scope.row)">编辑</el-button>
          <el-button type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
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

    <el-dialog v-model="show" title="会员" @close="show=false">
      <el-form :model="form">
        <el-form-item label="会员编号">
          <el-input v-model="form.memberNo" :disabled="isEdit"/>
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.name"/></el-form-item>
        <el-form-item label="电话">
          <div style="display: flex; gap: 10px;">
            <el-select v-model="form.phoneCode" placeholder="请选择区号" style="width: 120px; color: #409EFF; font-weight: bold">
              <el-option v-for="item in phoneCodeOptions" :key="item.code" :label="item.label" :value="item.code" style="color: #409EFF"/>
            </el-select>
            <el-input v-model="form.phoneNum" :placeholder="phonePlaceholder"/>
          </div>
        </el-form-item>
        <el-form-item label="会员等级">
          <el-select v-model="form.level" :disabled="!canChangeLevel">
            <el-option label="普通会员" value="普通会员"/>
            <el-option label="VIP" value="VIP"/>
            <el-option label="SVIP" value="SVIP"/>
          </el-select>
          <span v-if="!canChangeLevel && isEdit" style="color:#909399; font-size:12px; margin-left:8px">您无权修改此会员等级</span>
        </el-form-item>
        <el-form-item label="注册时间">
          <el-date-picker v-model="form.registerTime" type="datetime" placeholder="请选择日期时间" value-format="YYYY-MM-DD HH:mm"/>
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
import { getMemberList, addMember, updateMember, deleteMember, importMember, exportMember } from '@/api/member'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const searchText = ref('')
const filteredList = computed(() => {
  const kw = searchText.value.trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter(row =>
    (row.name && row.name.toLowerCase().includes(kw)) ||
    (row.phone && row.phone.includes(kw)) ||
    (row.memberNo && row.memberNo.toLowerCase().includes(kw))
  )
})
const pagedList = computed(() => filteredList.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value))
const show = ref(false)
const form = ref({})
const isEdit = ref(false)
const role = ref('')
const adminLevel = ref(0)

// 根据管理员层级判断是否可修改会员等级
const isEmployeeMember = (member) => {
  if (!member || !member.memberNo) return false
  return /^M00/.test(member.memberNo)
}

const canChangeLevel = computed(() => {
  if (!isEdit.value) return true  // 新增时可以选等级
  if (role.value !== '1') return false
  const memberNo = form.value.memberNo || ''
  if (adminLevel.value === 1) {
    // 11 管理员：不可修改自己（M11xxx）
    return !/^M11/.test(memberNo)
  }
  if (adminLevel.value === 2) {
    // 10 管理员：不可修改 11、自己（M10xxx）、01
    return !/^M(11|10|01)/.test(memberNo)
  }
  if (adminLevel.value === 3) {
    // 01 管理员：不可修改 11、10、自己（M01xxx）
    return !/^M(11|10|01)/.test(memberNo)
  }
  return false
})

const phoneCodeOptions = [
  { code: '+86', label: '+86 中国大陆', length: 11, pattern: /^1/ },
  { code: '+852', label: '+852 中国香港', length: 8, pattern: /^(5|6|9)/ },
  { code: '+853', label: '+853 中国澳门', length: 8, pattern: /^6/ },
  { code: '+886', label: '+886 中国台湾', length: 10, pattern: /^09/ },
  { code: '+81', label: '+81 日本', length: 10, pattern: /^[0-9]/ },
  { code: '+82', label: '+82 韩国', length: 11, pattern: /^[0-9]/ },
  { code: '+65', label: '+65 新加坡', length: 8, pattern: /^[0-9]/ },
  { code: '+66', label: '+66 泰国', length: 10, pattern: /^[0-9]/ },
  { code: '+60', label: '+60 马来西亚', length: 10, pattern: /^[0-9]/ },
  { code: '+84', label: '+84 越南', length: 10, pattern: /^[0-9]/ },
  { code: '+91', label: '+91 印度', length: 10, pattern: /^[0-9]/ },
  { code: '+971', label: '+971 阿联酋', length: 9, pattern: /^[0-9]/ },
  { code: '+966', label: '+966 沙特', length: 9, pattern: /^[0-9]/ },
  { code: '+62', label: '+62 印尼', length: 12, pattern: /^[0-9]/ },
  { code: '+63', label: '+63 菲律宾', length: 10, pattern: /^[0-9]/ },
  { code: '+1', label: '+1 美国/加拿大', length: 10, pattern: /^[0-9]/ },
  { code: '+7', label: '+7 俄罗斯', length: 10, pattern: /^[0-9]/ },
  { code: '+44', label: '+44 英国', length: 11, pattern: /^[0-9]/ },
  { code: '+49', label: '+49 德国', length: 11, pattern: /^[0-9]/ },
  { code: '+33', label: '+33 法国', length: 9, pattern: /^[0-9]/ },
  { code: '+39', label: '+39 意大利', length: 10, pattern: /^[0-9]/ },
  { code: '+34', label: '+34 西班牙', length: 9, pattern: /^[0-9]/ },
  { code: '+41', label: '+41 瑞士', length: 9, pattern: /^[0-9]/ },
  { code: '+46', label: '+46 瑞典', length: 9, pattern: /^[0-9]/ },
  { code: '+47', label: '+47 挪威', length: 8, pattern: /^[0-9]/ },
  { code: '+61', label: '+61 澳大利亚', length: 9, pattern: /^[0-9]/ },
  { code: '+64', label: '+64 新西兰', length: 9, pattern: /^[0-9]/ },
  { code: '+55', label: '+55 巴西', length: 11, pattern: /^[0-9]/ },
  { code: '+54', label: '+54 阿根廷', length: 10, pattern: /^[0-9]/ }
]

const phonePlaceholder = computed(() => {
  const selected = phoneCodeOptions.find(item => item.code === form.value.phoneCode)
  if (selected) {
    return `请输入${selected.length}位电话号码${selected.pattern.source !== '/^[0-9]/' ? '，' + selected.label.split(' ')[1] + '手机号以' + selected.pattern.source.replace(/[\^\/]/g, '') + '开头' : ''}`
  }
  return '请输入电话号码'
})

const validatePhone = () => {
  const selected = phoneCodeOptions.find(item => item.code === form.value.phoneCode)
  const phoneNum = form.value.phoneNum
  
  if (!selected || !phoneNum) return true
  
  const phoneRegex = /^[0-9]+$/
  if (!phoneRegex.test(phoneNum)) {
    ElMessage.warning('电话号码只能包含数字')
    return false
  }

  if (phoneNum.length !== selected.length) {
    ElMessage.warning(`${selected.label.split(' ')[1]}的电话号码必须是${selected.length}位`)
    return false
  }

  if (!selected.pattern.test(phoneNum)) {
    ElMessage.warning(`${selected.label.split(' ')[1]}的电话号码必须以${selected.pattern.source.replace(/[\^\/]/g, '')}开头`)
    return false
  }
  
  return true
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  } catch (e) {
    return dateStr
  }
}

const loadData = async () => {
  const res = await getMemberList()
  if (res.code === 200) {
    list.value = res.data.map(item => {
      let phoneCode = '+86'
      let phoneNum = ''
      if (item.phone) {
        if (item.phone.includes('|')) {
          const parts = item.phone.split('|')
          phoneCode = parts[0] || '+86'
          phoneNum = parts[1] || ''
        } else {
          const phoneCodeRegex = /^(\+86|\+852|\+853|\+886|\+81|\+82|\+65|\+66|\+60|\+84|\+91|\+971|\+966|\+62|\+63|\+1|\+7|\+44|\+49|\+33|\+39|\+34|\+41|\+46|\+47|\+61|\+64|\+55|\+54)/
          const match = item.phone.match(phoneCodeRegex)
          if (match) {
            phoneCode = match[1]
            phoneNum = item.phone.substring(match[1].length)
          } else {
            phoneCode = '+86'
            phoneNum = item.phone
          }
        }
      }
      return { ...item, registerTimeFormatted: formatDateTime(item.registerTime), phoneCode, phoneNum }
    })
  }
}

const openAdd = () => {
  form.value = { phoneCode: '+86', phoneNum: '', level: '普通会员' }
  isEdit.value = false
  show.value = true
}

const openEdit = (row) => { 
  form.value = { ...row }
  if (!form.value.phoneCode) {
    form.value.phoneCode = '+86'
  }
  if (!form.value.phoneNum && form.value.phone) {
    const parts = form.value.phone.split('|')
    form.value.phoneCode = parts[0] || '+86'
    form.value.phoneNum = parts[1] || ''
  }
  isEdit.value = true
  show.value = true 
}

const submit = async () => {
  if (!validatePhone()) return

  if (form.value.phoneCode && form.value.phoneNum) {
    form.value.phone = form.value.phoneCode + '|' + form.value.phoneNum
  }

  if (isEdit.value) {
    const res = await updateMember(form.value)
    ElMessage.info(res.msg)
  } else {
    const res = await addMember(form.value)
    ElMessage.info(res.msg)
  }
  show.value = false
  loadData()
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除？', '确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const res = await deleteMember(id)
    ElMessage.info(res.msg)
    loadData()
  } catch { /* 取消 */ }
}

const fileInput = ref(null)

const handleImport = () => {
  fileInput.value.click()
}

const onFileSelect = async (event) => {
  const file = event.target.files[0]
  if (file) {
    const res = await importMember(file)
    ElMessage.info(res.msg)
    loadData()
    event.target.value = ''
  }
}

const handleExport = async () => {
  const res = await exportMember()
  const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '会员数据.xlsx'
  document.body.appendChild(a)
  a.click()
  window.URL.revokeObjectURL(url)
  document.body.removeChild(a)
}

role.value = localStorage.getItem('role') || ''
adminLevel.value = parseInt(localStorage.getItem('adminLevel') || '0')
loadData()
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

@media (max-width: 767px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar .el-input,
  .toolbar .el-button {
    width: 100%;
    margin-right: 0 !important;
  }
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
</style>