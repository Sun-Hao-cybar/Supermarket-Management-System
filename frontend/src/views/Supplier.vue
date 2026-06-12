<template>
  <div class="p-4">
    <h3>供应商管理</h3>
    <div style="display: flex; gap: 10px; margin-bottom: 15px;">
      <el-input
        v-model="searchText"
        placeholder="搜索供应商名称 / 编码 / 联系人..."
        clearable
        size="small"
        style="width: 240px; margin-right: 8px"
        @input="currentPage = 1"
      />
      <el-button type="primary" @click="openAdd" v-if="hasManagePermission">新增</el-button>
      <el-button @click="handleImport" v-if="hasManagePermission">导入Excel</el-button>
      <el-button @click="handleExport">导出Excel</el-button>
      <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="onFileSelect" />
    </div>
    <div class="table-wrap">
    <el-table :data="pagedList" border size="small">
      <el-table-column label="编号" type="index" width="60"/>
      <el-table-column label="供应商编号" prop="supplierCode"/>
      <el-table-column label="名称" prop="supplierName"/>
      <el-table-column label="简称" prop="shortName"/>
      <el-table-column label="地址" prop="address"/>
      <el-table-column label="公司电话" prop="phone"/>
      <el-table-column label="邮件" prop="email"/>
      <el-table-column label="联系人" prop="contactPerson"/>
      <el-table-column label="联系人电话">
        <template #default="scope">
          {{ scope.row.contactPhoneCode || '+86' }}|{{ scope.row.contactPhoneNum }}
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="remark"/>
      <el-table-column label="操作" v-if="hasManagePermission">
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

    <el-dialog v-model="show" title="供应商" @close="show=false">
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item label="供应商编号"><el-input v-model="form.supplierCode"/></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.supplierName"/></el-form-item>
        <el-form-item label="简称"><el-input v-model="form.shortName"/></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address"/></el-form-item>
        <el-form-item label="公司电话"><el-input v-model="form.phone"/></el-form-item>
        <el-form-item label="邮件" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱地址，如: example@email.com"/>
        </el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactPerson"/></el-form-item>
        <el-form-item label="联系人电话">
          <div style="display: flex; gap: 10px;">
            <el-select v-model="form.contactPhoneCode" placeholder="请选择区号" style="width: 120px; color: #409EFF; font-weight: bold">
              <el-option v-for="item in phoneCodeOptions" :key="item.code" :label="item.label" :value="item.code" style="color: #409EFF"/>
            </el-select>
            <el-input v-model="form.contactPhoneNum" :placeholder="phonePlaceholder"/>
          </div>
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
import { getSupplierList, addSupplier, updateSupplier, deleteSupplier, importSupplier, exportSupplier } from '@/api/supplier'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const searchText = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const filteredList = computed(() => {
  const kw = searchText.value.trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter(row =>
    (row.supplierName && row.supplierName.toLowerCase().includes(kw)) ||
    (row.supplierCode && row.supplierCode.toLowerCase().includes(kw)) ||
    (row.contactPerson && row.contactPerson.toLowerCase().includes(kw))
  )
})
const pagedList = computed(() => filteredList.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value))
const show = ref(false)
const form = ref({})
const isEdit = ref(false)
const fileInput = ref(null)
const role = ref('')
const adminLevel = ref(0)
const formRef = ref(null)

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

const rules = {
  email: [
    { 
      validator: (rule, value, callback) => {
        if (!value) {
          callback()
          return
        }
        const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
        if (!emailRegex.test(value)) {
          callback(new Error('请输入有效的邮箱地址，格式如: example@email.com'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur'
    }
  ]
}

const phonePlaceholder = computed(() => {
  const selected = phoneCodeOptions.find(item => item.code === form.value.contactPhoneCode)
  if (selected) {
    return `请输入${selected.length}位电话号码${selected.pattern.source !== '/^[0-9]/' ? '，' + selected.label.split(' ')[1] + '手机号以' + selected.pattern.source.replace(/[\^\/]/g, '') + '开头' : ''}`
  }
  return '请输入电话号码'
})

const hasManagePermission = computed(() => {
  return role.value === '1' && (adminLevel.value === 1 || adminLevel.value === 2)
})

onMounted(() => {
  role.value = localStorage.getItem('role') || ''
  adminLevel.value = parseInt(localStorage.getItem('adminLevel') || '0')
  loadData()
})

const validatePhone = () => {
  const selected = phoneCodeOptions.find(item => item.code === form.value.contactPhoneCode)
  const phoneNum = form.value.contactPhoneNum

  if (!selected || !phoneNum) return true

  const phoneRegex = /^[0-9]+$/
  if (!phoneRegex.test(phoneNum)) {
    ElMessage.warning('电话号码只能包含数字')
    return false
  }

  // 支持范围长度的国家（如日本10-11位）
  const phoneLengths = selected.length.toString().includes('-')
    ? selected.length.split('-').map(Number)
    : [selected.length]
  const minLen = phoneLengths[0]
  const maxLen = phoneLengths[phoneLengths.length - 1] || minLen
  if (phoneNum.length < minLen || phoneNum.length > maxLen) {
    ElMessage.warning(`${selected.label.split(' ')[1]}的电话号码必须是${selected.length}位`)
    return false
  }

  if (!selected.pattern.test(phoneNum)) {
    ElMessage.warning(`${selected.label.split(' ')[1]}的电话号码开头不符合规则`)
    return false
  }

  return true
}

const loadData = async () => {
  const res = await getSupplierList()
  if (res.code === 200) {
    list.value = res.data.map(item => {
      if (item.contactPhone) {
        if (item.contactPhone.includes('|')) {
          const parts = item.contactPhone.split('|')
          item.contactPhoneCode = parts[0] || '+86'
          item.contactPhoneNum = parts[1] || ''
        } else {
          const phoneCodeRegex = /^(\+86|\+852|\+853|\+886|\+81|\+82|\+65|\+66|\+60|\+84|\+91|\+971|\+966|\+62|\+63|\+1|\+7|\+44|\+49|\+33|\+39|\+34|\+41|\+46|\+47|\+61|\+64|\+55|\+54)/
          const match = item.contactPhone.match(phoneCodeRegex)
          if (match) {
            item.contactPhoneCode = match[1]
            item.contactPhoneNum = item.contactPhone.substring(match[1].length)
          } else {
            item.contactPhoneCode = '+86'
            item.contactPhoneNum = item.contactPhone
          }
        }
      } else {
        item.contactPhoneCode = '+86'
        item.contactPhoneNum = ''
      }
      return item
    })
  }
}

const openAdd = () => {
  form.value = { contactPhoneCode: '+86', contactPhoneNum: '', supplierCode: '', supplierName: '', shortName: '', address: '', phone: '', email: '', contactPerson: '', remark: '' }
  isEdit.value = false
  show.value = true
}

const openEdit = (row) => { 
  form.value = { ...row }
  if (!form.value.contactPhoneCode) {
    form.value.contactPhoneCode = '+86'
  }
  if (!form.value.contactPhoneNum && form.value.contactPhone) {
    const code = form.value.contactPhone.match(/^\+\d+/)?.[0] || '+86'
    form.value.contactPhoneCode = code
    form.value.contactPhoneNum = form.value.contactPhone.substring(code.length)
  }
  isEdit.value = true
  show.value = true 
}

const submit = async () => {
  if (!validatePhone()) return

  // 执行表单校验（如邮箱格式）
  try {
    await formRef.value.validate()
  } catch {
    return // 校验不通过
  }

  if (form.value.contactPhoneCode && form.value.contactPhoneNum) {
    form.value.contactPhone = form.value.contactPhoneCode + '|' + form.value.contactPhoneNum
  }

  if (isEdit.value) {
    const res = await updateSupplier(form.value)
    ElMessage.info(res.msg)
  } else {
    const res = await addSupplier(form.value)
    ElMessage.info(res.msg)
  }
  show.value = false
  loadData()
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除？', '确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const res = await deleteSupplier(id)
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
    const res = await importSupplier(file)
    ElMessage.info(res.msg)
    loadData()
    event.target.value = ''
  }
}

const handleExport = async () => {
  const res = await exportSupplier()
  const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '供应商数据.xlsx'
  document.body.appendChild(a)
  a.click()
  window.URL.revokeObjectURL(url)
  document.body.removeChild(a)
}
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
</style>