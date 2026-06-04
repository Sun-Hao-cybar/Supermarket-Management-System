<template>
  <div class="p-4">
    <h3>采购管理</h3>
    <div style="display: flex; gap: 10px; margin-bottom: 15px;">
      <el-button type="primary" @click="openMainAdd" v-if="hasManagePermission">新增采购单</el-button>
      <el-button @click="handleMainImport" v-if="hasManagePermission">导入主表Excel</el-button>
      <el-button @click="handleMainExport">导出主表Excel</el-button>
      <input ref="mainFileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="onMainFileSelect" />
    </div>
    <el-table :data="mainList" border style="margin-top:15px">
      <el-table-column label="采购清单号" prop="purchaseNo"/>
      <el-table-column label="员工编号" prop="userId"/>
      <el-table-column label="采购数量" prop="totalNum"/>
      <el-table-column label="采购总价" prop="totalPrice"/>
      <el-table-column label="采购时间" prop="purchaseTime"/>
      <el-table-column label="备注" prop="remark"/>
      <el-table-column label="操作" v-if="hasManagePermission">
        <template #default="scope">
          <el-button @click="openMainEdit(scope.row)">编辑</el-button>
          <el-button type="danger" @click="handleMainDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <h4 style="margin-top:20px">采购明细</h4>
    <div style="display: flex; gap: 10px; margin-bottom: 15px;">
      <el-button type="primary" @click="openDetailAdd" v-if="hasManagePermission">新增明细</el-button>
      <el-button @click="handleDetailImport" v-if="hasManagePermission">导入明细Excel</el-button>
      <el-button @click="handleDetailExport">导出明细Excel</el-button>
      <input ref="detailFileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="onDetailFileSelect" />
    </div>
    <el-table :data="detailList" border style="margin-top:15px">
      <el-table-column label="明细号" prop="detailNo"/>
      <el-table-column label="采购清单号" prop="purchaseNo"/>
      <el-table-column label="商品编号" prop="goodsId"/>
      <el-table-column label="采购数量" prop="goodsNum"/>
      <el-table-column label="商品单价" prop="goodsPrice"/>
      <el-table-column label="商品总价" prop="totalPrice"/>
      <el-table-column label="备注" prop="remark"/>
      <el-table-column label="操作" v-if="hasManagePermission">
        <template #default="scope">
          <el-button @click="openDetailEdit(scope.row)">编辑</el-button>
          <el-button type="danger" @click="handleDetailDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="mainShow" title="采购主表" @close="mainShow=false">
      <el-form :model="mainForm">
        <el-form-item label="采购清单号"><el-input v-model="mainForm.purchaseNo"/></el-form-item>
        <el-form-item label="员工"><el-select v-model="mainForm.userId" placeholder="请选择员工">
          <el-option v-for="user in userList" :key="user.id" :label="user.username + ' - ' + user.realName" :value="user.id"/>
        </el-select></el-form-item>
        <el-form-item label="采购数量"><el-input v-model="mainForm.totalNum"/></el-form-item>
        <el-form-item label="采购总价"><el-input v-model="mainForm.totalPrice"/></el-form-item>
        <el-form-item label="采购时间">
          <el-date-picker v-model="mainForm.purchaseTime" type="date" placeholder="请选择日期" value-format="YYYY-MM-DD"/>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="mainForm.remark"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mainShow=false">取消</el-button>
        <el-button type="primary" @click="submitMain">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailShow" title="采购明细" @close="detailShow=false">
      <el-form :model="detailForm">
        <el-form-item label="明细号"><el-input v-model="detailForm.detailNo"/></el-form-item>
        <el-form-item label="采购清单号"><el-input v-model="detailForm.purchaseNo"/></el-form-item>
        <el-form-item label="商品编号"><el-input v-model="detailForm.goodsId"/></el-form-item>
        <el-form-item label="采购数量"><el-input v-model="detailForm.goodsNum"/></el-form-item>
        <el-form-item label="商品单价"><el-input v-model="detailForm.goodsPrice"/></el-form-item>
        <el-form-item label="商品总价"><el-input v-model="detailForm.totalPrice"/></el-form-item>
        <el-form-item label="备注"><el-input v-model="detailForm.remark"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="detailShow=false">取消</el-button>
        <el-button type="primary" @click="submitDetail">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getPurchaseMainList, addPurchaseMain, updatePurchaseMain, deletePurchaseMain, importPurchaseMain, exportPurchaseMain } from '@/api/purchaseMain'
import { getPurchaseDetailList, addPurchaseDetail, updatePurchaseDetail, deletePurchaseDetail, importPurchaseDetail, exportPurchaseDetail } from '@/api/purchaseDetail'
import { getUserList } from '@/api/user'

const mainList = ref([])
const detailList = ref([])
const userList = ref([])
const mainShow = ref(false)
const detailShow = ref(false)
const mainForm = ref({})
const detailForm = ref({})
const isMainEdit = ref(false)
const isDetailEdit = ref(false)
const mainFileInput = ref(null)
const detailFileInput = ref(null)
const role = ref('')
const adminLevel = ref(0)

const hasManagePermission = computed(() => {
  return role.value === '1' && (adminLevel.value === 1 || adminLevel.value === 3)
})

const loadMainData = async () => {
  const res = await getPurchaseMainList()
  if (res.code === 200) mainList.value = res.data
}

const loadDetailData = async () => {
  const res = await getPurchaseDetailList()
  if (res.code === 200) detailList.value = res.data
}

const openMainAdd = () => { mainForm.value = {}; isMainEdit.value = false; mainShow.value = true }
const openMainEdit = (row) => { mainForm.value = { ...row }; isMainEdit.value = true; mainShow.value = true }

const openDetailAdd = () => { detailForm.value = {}; isDetailEdit.value = false; detailShow.value = true }
const openDetailEdit = (row) => { detailForm.value = { ...row }; isDetailEdit.value = true; detailShow.value = true }

const submitMain = async () => {
  if (isMainEdit.value) {
    const res = await updatePurchaseMain(mainForm.value)
    alert(res.msg)
  } else {
    const res = await addPurchaseMain(mainForm.value)
    alert(res.msg)
  }
  mainShow.value = false
  loadMainData()
}

const submitDetail = async () => {
  if (isDetailEdit.value) {
    const res = await updatePurchaseDetail(detailForm.value)
    alert(res.msg)
  } else {
    const res = await addPurchaseDetail(detailForm.value)
    alert(res.msg)
  }
  detailShow.value = false
  loadDetailData()
}

const handleMainDelete = async (id) => {
  if (confirm('确定删除？')) {
    const res = await deletePurchaseMain(id)
    alert(res.msg)
    loadMainData()
  }
}

const handleDetailDelete = async (id) => {
  if (confirm('确定删除？')) {
    const res = await deletePurchaseDetail(id)
    alert(res.msg)
    loadDetailData()
  }
}

const handleMainImport = () => {
  mainFileInput.value.click()
}

const onMainFileSelect = async (event) => {
  const file = event.target.files[0]
  if (file) {
    const res = await importPurchaseMain(file)
    alert(res.msg)
    loadMainData()
    event.target.value = ''
  }
}

const handleMainExport = async () => {
  const res = await exportPurchaseMain()
  const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '采购主表数据.xlsx'
  document.body.appendChild(a)
  a.click()
  window.URL.revokeObjectURL(url)
  document.body.removeChild(a)
}

const handleDetailImport = () => {
  detailFileInput.value.click()
}

const onDetailFileSelect = async (event) => {
  const file = event.target.files[0]
  if (file) {
    const res = await importPurchaseDetail(file)
    alert(res.msg)
    loadDetailData()
    event.target.value = ''
  }
}

const handleDetailExport = async () => {
  const res = await exportPurchaseDetail()
  const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '采购明细数据.xlsx'
  document.body.appendChild(a)
  a.click()
  window.URL.revokeObjectURL(url)
  document.body.removeChild(a)
}

const loadUserData = async () => {
  const res = await getUserList()
  if (res.code === 200) userList.value = res.data
}

onMounted(() => {
  role.value = localStorage.getItem('role') || ''
  adminLevel.value = parseInt(localStorage.getItem('adminLevel') || '0')
  loadMainData()
  loadDetailData()
  loadUserData()
})
</script>