<template>
  <div class="p-4">
    <h3>商品管理</h3>
    <div style="display: flex; gap: 10px; margin-bottom: 15px;">
      <el-button type="primary" @click="openAdd" v-if="hasManagePermission">新增</el-button>
      <el-button @click="handleImport" v-if="hasManagePermission">导入Excel</el-button>
      <el-button @click="handleExport">导出Excel</el-button>
      <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="onFileSelect" />
    </div>
    <div class="table-wrap">
    <el-table :data="pagedList" border size="small">
      <el-table-column label="编号" type="index" width="60"/>
      <el-table-column label="商品编号" prop="goodsCode"/>
      <el-table-column label="名称" prop="goodsName"/>
      <el-table-column label="单价" prop="price"/>
      <el-table-column label="供应商编号" prop="supplierCode"/>
      <el-table-column label="简介" prop="intro"/>
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
      :total="list.length"
      layout="prev, pager, next"
      style="margin-top:15px; justify-content:center"
    />
    </div>

    <el-dialog v-model="show" title="商品" @close="show=false">
      <el-form :model="form">
        <el-form-item label="商品编号"><el-input v-model="form.goodsCode"/></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.goodsName"/></el-form-item>
        <el-form-item label="单价">
          <el-input-number v-model="form.price" :min="0" :step="0.01" :precision="2" style="width:100%"/>
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="form.supplierId" placeholder="请选择供应商">
            <el-option v-for="supplier in supplierList" :key="supplier.id" :label="supplier.supplierCode + ' - ' + supplier.supplierName" :value="supplier.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="简介"><el-input v-model="form.intro"/></el-form-item>
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
import { getGoodsList, addGoods, updateGoods, deleteGoods, importGoods, exportGoods } from '@/api/goods'
import { getSupplierList } from '@/api/supplier'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const pagedList = computed(() => list.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value))
const supplierList = ref([])
const show = ref(false)
const form = ref({})
const isEdit = ref(false)
const fileInput = ref(null)
const role = ref('')
const adminLevel = ref(0)

const hasManagePermission = computed(() => {
  return role.value === '1' && (adminLevel.value === 1 || adminLevel.value === 2)
})

onMounted(async () => {
  role.value = localStorage.getItem('role') || ''
  adminLevel.value = parseInt(localStorage.getItem('adminLevel') || '0')
  // 先加载供应商数据，再加载商品（避免重复请求）
  await loadSupplierData()
})

const loadData = async () => {
  const res = await getGoodsList()
  if (res.code === 200) {
    list.value = res.data.map(item => {
      const supplier = supplierList.value.find(s => s.id === item.supplierId)
      if (supplier) {
        item.supplierCode = supplier.supplierCode
      }
      return item
    })
  }
}

const loadSupplierData = async () => {
  const res = await getSupplierList()
  if (res.code === 200) {
    supplierList.value = res.data
  }
  // 供应商数据就绪后再加载商品
  await loadData()
}

const openAdd = () => { form.value = {}; isEdit.value = false; show.value = true }
const openEdit = (row) => { form.value = { ...row }; isEdit.value = true; show.value = true }

const submit = async () => {
  if (isEdit.value) {
    const res = await updateGoods(form.value)
    ElMessage.info(res.msg)
  } else {
    const res = await addGoods(form.value)
    ElMessage.info(res.msg)
  }
  show.value = false
  loadData()
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除？', '确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const res = await deleteGoods(id)
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
    const res = await importGoods(file)
    ElMessage.info(res.msg)
    loadData()
    event.target.value = ''
  }
}

const handleExport = async () => {
  const res = await exportGoods()
  const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '商品数据.xlsx'
  document.body.appendChild(a)
  a.click()
  window.URL.revokeObjectURL(url)
  document.body.removeChild(a)
}
</script>

<style scoped>
.table-wrap {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
</style>