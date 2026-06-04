<template>
  <div class="p-4">
    <h3>供应商管理</h3>
    <div style="display: flex; gap: 10px; margin-bottom: 15px;">
      <el-button type="primary" @click="openAdd" v-if="hasManagePermission">新增</el-button>
      <el-button @click="handleImport" v-if="hasManagePermission">导入Excel</el-button>
      <el-button @click="handleExport">导出Excel</el-button>
      <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="onFileSelect" />
    </div>
    <el-table :data="list" border style="margin-top:15px">
      <el-table-column label="编号" prop="id"/>
      <el-table-column label="供应商编号" prop="supplierCode"/>
      <el-table-column label="名称" prop="supplierName"/>
      <el-table-column label="简称" prop="shortName"/>
      <el-table-column label="地址" prop="address"/>
      <el-table-column label="公司电话" prop="phone"/>
      <el-table-column label="邮件" prop="email"/>
      <el-table-column label="联系人" prop="contactPerson"/>
      <el-table-column label="联系人电话" prop="contactPhone"/>
      <el-table-column label="备注" prop="remark"/>
      <el-table-column label="操作" v-if="hasManagePermission">
        <template #default="scope">
          <el-button @click="openEdit(scope.row)">编辑</el-button>
          <el-button type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="show" title="供应商" @close="show=false">
      <el-form :model="form">
        <el-form-item label="供应商编号"><el-input v-model="form.supplierCode"/></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.supplierName"/></el-form-item>
        <el-form-item label="简称"><el-input v-model="form.shortName"/></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address"/></el-form-item>
        <el-form-item label="公司电话"><el-input v-model="form.phone"/></el-form-item>
        <el-form-item label="邮件"><el-input v-model="form.email"/></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactPerson"/></el-form-item>
        <el-form-item label="联系人电话"><el-input v-model="form.contactPhone"/></el-form-item>
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

const list = ref([])
const show = ref(false)
const form = ref({})
const isEdit = ref(false)
const fileInput = ref(null)
const role = ref('')
const adminLevel = ref(0)

const hasManagePermission = computed(() => {
  return role.value === '1' && (adminLevel.value === 1 || adminLevel.value === 2)
})

onMounted(() => {
  role.value = localStorage.getItem('role') || ''
  adminLevel.value = parseInt(localStorage.getItem('adminLevel') || '0')
  loadData()
})

const loadData = async () => {
  const res = await getSupplierList()
  if (res.code === 200) list.value = res.data
}

const openAdd = () => { form.value = {}; isEdit.value = false; show.value = true }
const openEdit = (row) => { form.value = { ...row }; isEdit.value = true; show.value = true }

const submit = async () => {
  if (isEdit.value) {
    const res = await updateSupplier(form.value)
    alert(res.msg)
  } else {
    const res = await addSupplier(form.value)
    alert(res.msg)
  }
  show.value = false
  loadData()
}

const handleDelete = async (id) => {
  if (confirm('确定删除？')) {
    const res = await deleteSupplier(id)
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
    const res = await importSupplier(file)
    alert(res.msg)
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