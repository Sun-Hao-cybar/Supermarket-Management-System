<template>
  <div class="p-4">
    <h3>采购管理</h3>

    <!-- 主表工具栏 -->
    <div style="display: flex; gap: 10px; margin-bottom: 15px;">
      <el-button type="primary" @click="openMainAdd" v-if="hasManagePermission">新增采购单</el-button>
      <el-button @click="handleMainImport" v-if="hasManagePermission">导入主表Excel</el-button>
      <el-button @click="handleMainExport">导出主表Excel</el-button>
      <input ref="mainFileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="onMainFileSelect" />
    </div>

    <!-- 主表 -->
    <div class="table-wrap">
    <el-table :data="pagedMainList" border highlight-current-row @row-click="onMainRowClick" :row-class-name="mainRowClass" size="small">
      <el-table-column label="采购清单号">
        <template #default="scope">
          <el-button link type="primary" @click.stop="openPurchaseInfo(scope.row)">{{ scope.row.purchaseNo }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="员工">
        <template #default="scope">{{ getUserLabel(scope.row.userId) }}</template>
      </el-table-column>
      <el-table-column label="采购数量">
        <template #default="scope">{{ scope.row.totalNum || 0 }} 件</template>
      </el-table-column>
      <el-table-column label="采购总价">
        <template #default="scope">{{ scope.row.totalPrice || 0 }} 元</template>
      </el-table-column>
      <el-table-column label="采购时间">
        <template #default="scope">{{ scope.row.purchaseTime }}</template>
      </el-table-column>
      <el-table-column label="备注" prop="remark"/>
      <el-table-column label="操作" v-if="hasManagePermission">
        <template #default="scope">
          <el-button @click.stop="openMainEdit(scope.row)" v-if="canManageRow(scope.row)">编辑</el-button>
          <el-button type="danger" @click.stop="handleMainDelete(scope.row)" v-if="canManageRow(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="mainList.length > pageSize"
      v-model:current-page="mainPage"
      :page-size="pageSize"
      :total="mainList.length"
      layout="prev, pager, next"
      style="margin-top:15px; justify-content:center"
    />
    </div>

    <!-- 明细工具栏 -->
    <h4 style="margin-top:20px">
      采购明细
      <span v-if="selectedMainNo" style="color:#409EFF;font-size:14px">
        — 当前筛选：{{ selectedMainNo }}
        <el-button type="warning" size="small" link @click="clearFilter">清除筛选</el-button>
      </span>
    </h4>
    <div style="display: flex; gap: 10px; margin-bottom: 15px;">
      <el-button type="primary" @click="openDetailAdd" v-if="hasManagePermission">新增明细</el-button>
      <el-button @click="handleDetailImport" v-if="hasManagePermission">导入明细Excel</el-button>
      <el-button @click="handleDetailExport">导出明细Excel</el-button>
      <input ref="detailFileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="onDetailFileSelect" />
    </div>

    <!-- 明细表 -->
    <div class="table-wrap">
    <el-table :data="pagedDetailList" border size="small">
      <el-table-column label="明细号" prop="detailNo"/>
      <el-table-column label="采购清单号">
        <template #default="scope">
          <el-button link type="primary" @click.stop="openPurchaseInfoByNo(scope.row.purchaseNo)">{{ scope.row.purchaseNo }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="商品">
        <template #default="scope">{{ getGoodsLabel(scope.row.goodsId) }}</template>
      </el-table-column>
      <el-table-column label="采购数量">
        <template #default="scope">{{ scope.row.goodsNum }} 件</template>
      </el-table-column>
      <el-table-column label="商品单价">
        <template #default="scope">{{ scope.row.goodsPrice }} 元</template>
      </el-table-column>
      <el-table-column label="商品总价">
        <template #default="scope">{{ scope.row.totalPrice }} 元</template>
      </el-table-column>
      <el-table-column label="备注" prop="remark"/>
      <el-table-column label="操作" v-if="hasManagePermission">
        <template #default="scope">
          <el-button @click="openDetailEdit(scope.row)">编辑</el-button>
          <el-button type="danger" @click="handleDetailDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="filteredDetailList.length > pageSize"
      v-model:current-page="detailPage"
      :page-size="pageSize"
      :total="filteredDetailList.length"
      layout="prev, pager, next"
      style="margin-top:15px; justify-content:center"
    />
    </div>

    <!-- 主表对话框 -->
    <el-dialog v-model="mainShow" title="采购主表" @close="mainShow=false">
      <el-form :model="mainForm">
        <el-form-item label="采购清单号"><el-input v-model="mainForm.purchaseNo"/></el-form-item>
        <el-form-item label="员工">
          <el-select v-model="mainForm.userId" placeholder="请选择员工" style="width:100%">
            <el-option v-for="user in availableEmployees" :key="user.id" :label="user.username + ' - ' + user.realName" :value="user.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="采购时间">
          <el-date-picker v-model="mainForm.purchaseTime" type="date" placeholder="请选择日期" value-format="YYYY-MM-DD" style="width:100%"/>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="mainForm.remark"/></el-form-item>
        <el-form-item>
          <span style="color:#909399;font-size:12px">采购数量和总价由明细自动汇总计算</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mainShow=false">取消</el-button>
        <el-button type="primary" @click="submitMain">确定</el-button>
      </template>
    </el-dialog>

    <!-- 明细对话框 -->
    <el-dialog v-model="detailShow" title="采购明细" @close="detailShow=false">
      <el-form :model="detailForm">
        <el-form-item label="明细号"><el-input v-model="detailForm.detailNo"/></el-form-item>
        <el-form-item label="采购清单号">
          <el-input v-model="detailForm.purchaseNo" :disabled="!!selectedMainNo && !isDetailEdit" :placeholder="selectedMainNo || '请输入采购清单号'"/>
        </el-form-item>
        <el-form-item label="商品">
          <el-select v-model="detailForm.goodsId" placeholder="请选择商品" @change="onGoodsChange" style="width:100%">
            <el-option v-for="g in goodsList" :key="g.id" :label="g.goodsCode + ' - ' + g.goodsName + ' (' + g.price + '元)'" :value="g.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="采购数量">
          <el-input-number v-model="detailForm.goodsNum" :min="1" @change="calcTotalPrice" style="width:100%" />
        </el-form-item>
        <el-form-item label="商品单价">
          <el-input-number v-model="detailForm.goodsPrice" :min="0" :step="0.01" style="width:100%" />
        </el-form-item>
        <el-form-item label="商品总价">
          <span style="font-weight:bold;font-size:16px">{{ calcDisplayTotal }} 元</span>
          <span style="color:#909399;font-size:12px;margin-left:8px">= 数量 × 单价</span>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="detailForm.remark"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="detailShow=false">取消</el-button>
        <el-button type="primary" @click="submitDetail">确定</el-button>
      </template>
    </el-dialog>

    <!-- 采购信息弹窗 -->
    <el-dialog v-model="infoShow" title="采购信息" width="700px" @close="infoShow=false">
      <template v-if="infoMain">
        <el-descriptions :column="2" border size="small" style="margin-bottom:20px">
          <el-descriptions-item label="采购清单号" :span="2">{{ infoMain.purchaseNo }}</el-descriptions-item>
          <el-descriptions-item label="员工">{{ getUserLabel(infoMain.userId) }}</el-descriptions-item>
          <el-descriptions-item label="采购时间">{{ infoMain.purchaseTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="采购数量">{{ infoMain.totalNum || 0 }} 件</el-descriptions-item>
          <el-descriptions-item label="采购总价">{{ infoMain.totalPrice || 0 }} 元</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ infoMain.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin-bottom:10px">关联明细</h4>
        <el-table :data="infoDetails" border size="small">
          <el-table-column label="明细号" prop="detailNo"/>
          <el-table-column label="商品">
            <template #default="scope">{{ getGoodsLabel(scope.row.goodsId) }}</template>
          </el-table-column>
          <el-table-column label="数量" prop="goodsNum"/>
          <el-table-column label="单价" prop="goodsPrice"/>
          <el-table-column label="总价" prop="totalPrice"/>
          <el-table-column label="备注" prop="remark"/>
        </el-table>
        <el-empty v-if="infoDetails.length === 0" description="暂无关联明细" />
      </template>
      <template #footer>
        <el-button @click="infoShow=false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getPurchaseMainList, addPurchaseMain, updatePurchaseMain, deletePurchaseMain, importPurchaseMain, exportPurchaseMain } from '@/api/purchaseMain'
import { getPurchaseDetailList, addPurchaseDetail, updatePurchaseDetail, deletePurchaseDetail, importPurchaseDetail, exportPurchaseDetail } from '@/api/purchaseDetail'
import { getUserList } from '@/api/user'
import { getGoodsList } from '@/api/goods'
import { ElMessage, ElMessageBox } from 'element-plus'

const mainList = ref([])
const detailList = ref([])
const goodsList = ref([])
const userList = ref([])
const mainPage = ref(1)
const detailPage = ref(1)
const pageSize = ref(10)
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
const currentUserId = ref(null)
const selectedMainNo = ref('')

// 采购信息弹窗
const infoShow = ref(false)
const infoMain = ref(null)
const infoDetails = ref([])

const hasManagePermission = computed(() => {
  return role.value === '1' && (adminLevel.value === 1 || adminLevel.value === 3)
})

// 可选员工列表：只显示已注册的普通用户（password 非空表示已完成注册）
const availableEmployees = computed(() => {
  let list = userList.value.filter(u => u.role === 0 && u.password)
  if (adminLevel.value === 3) {
    const self = userList.value.find(u => u.id === currentUserId.value)
    if (self) list.unshift(self)
  }
  return list
})

const pagedMainList = computed(() => mainList.value.slice((mainPage.value - 1) * pageSize.value, mainPage.value * pageSize.value))

// 明细按选中的采购清单号过滤
const filteredDetailList = computed(() => {
  if (!selectedMainNo.value) return detailList.value
  return detailList.value.filter(d => d.purchaseNo === selectedMainNo.value)
})
const pagedDetailList = computed(() => filteredDetailList.value.slice((detailPage.value - 1) * pageSize.value, detailPage.value * pageSize.value))

// 计算明细总价显示值
const calcDisplayTotal = computed(() => {
  const num = parseFloat(detailForm.value.goodsNum) || 0
  const price = parseFloat(detailForm.value.goodsPrice) || 0
  return (num * price).toFixed(2)
})

// 用户显示标签
const getUserLabel = (userId) => {
  const user = userList.value.find(u => u.id === userId)
  return user ? `${user.username} - ${user.realName || ''}` : `ID:${userId}`
}

// 商品显示标签
const getGoodsLabel = (goodsId) => {
  const goods = goodsList.value.find(g => g.id === goodsId)
  return goods ? `${goods.goodsCode} - ${goods.goodsName}` : `ID:${goodsId}`
}

// 11管理员可管理所有采购，01管理员只能管理自己和普通用户的采购
const canManageRow = (row) => {
  if (adminLevel.value === 1) return true
  if (adminLevel.value === 3) {
    const purchaseUser = userList.value.find(u => u.id === row.userId)
    if (!purchaseUser) return false
    if (purchaseUser.role === 0) return true
    if (purchaseUser.id === currentUserId.value) return true
    return false
  }
  return false
}

// 主表行点击
const onMainRowClick = (row) => {
  selectedMainNo.value = row.purchaseNo
  detailPage.value = 1
}

// 主表行高亮
const mainRowClass = ({ row }) => {
  return row.purchaseNo === selectedMainNo.value ? 'current-row' : ''
}

// 清除筛选
const clearFilter = () => {
  selectedMainNo.value = ''
  detailPage.value = 1
}

// 打开采购信息弹窗（通过主表行）
const openPurchaseInfo = async (row) => {
  await loadDetailData()
  infoMain.value = mainList.value.find(m => m.purchaseNo === row.purchaseNo) || row
  infoDetails.value = detailList.value.filter(d => d.purchaseNo === row.purchaseNo)
  infoShow.value = true
}

// 打开采购信息弹窗（通过明细表采购清单号）
const openPurchaseInfoByNo = async (purchaseNo) => {
  await loadDetailData()
  const main = mainList.value.find(m => m.purchaseNo === purchaseNo)
  if (main) {
    infoMain.value = main
    infoDetails.value = detailList.value.filter(d => d.purchaseNo === purchaseNo)
    infoShow.value = true
  }
}

const loadMainData = async () => {
  const res = await getPurchaseMainList()
  if (res.code === 200) mainList.value = res.data
}

const loadDetailData = async () => {
  const res = await getPurchaseDetailList()
  if (res.code === 200) detailList.value = res.data
}

// 重新计算主表汇总
const recalcMainTotals = async (purchaseNo) => {
  const details = detailList.value.filter(d => d.purchaseNo === purchaseNo)
  const totalNum = details.reduce((sum, d) => sum + (d.goodsNum || 0), 0)
  const totalPrice = details.reduce((sum, d) => sum + (parseFloat(d.totalPrice) || 0), 0)
  const main = mainList.value.find(m => m.purchaseNo === purchaseNo)
  if (main) {
    await updatePurchaseMain({ ...main, totalNum, totalPrice: totalPrice.toFixed(2) }, currentUserId.value, adminLevel.value)
    loadMainData()
  }
}

const openMainAdd = () => { mainForm.value = {}; isMainEdit.value = false; mainShow.value = true }
const openMainEdit = (row) => { mainForm.value = { ...row }; isMainEdit.value = true; mainShow.value = true }

const openDetailAdd = () => {
  detailForm.value = { goodsNum: 1, goodsPrice: 0, purchaseNo: selectedMainNo.value || '' }
  isDetailEdit.value = false
  detailShow.value = true
}
const openDetailEdit = (row) => { detailForm.value = { ...row }; isDetailEdit.value = true; detailShow.value = true }

const submitMain = async () => {
  if (!mainForm.value.purchaseNo) {
    ElMessage.warning('请输入采购清单号')
    return
  }
  if (isMainEdit.value) {
    const res = await updatePurchaseMain(mainForm.value, currentUserId.value, adminLevel.value)
    ElMessage.info(res.msg)
  } else {
    mainForm.value.totalNum = 0
    mainForm.value.totalPrice = 0
    const res = await addPurchaseMain(mainForm.value)
    ElMessage.info(res.msg)
  }
  mainShow.value = false
  loadMainData()
}

const submitDetail = async () => {
  if (!detailForm.value.purchaseNo) {
    ElMessage.warning('请输入采购清单号')
    return
  }
  // 使用 calcDisplayTotal 作为实际总价
  detailForm.value.totalPrice = parseFloat(calcDisplayTotal.value)
  if (isDetailEdit.value) {
    const res = await updatePurchaseDetail(detailForm.value, currentUserId.value, adminLevel.value)
    ElMessage.info(res.msg)
  } else {
    const res = await addPurchaseDetail(detailForm.value)
    ElMessage.info(res.msg)
  }
  detailShow.value = false
  await loadDetailData()
  // 自动更新主表汇总
  await recalcMainTotals(detailForm.value.purchaseNo)
}

const handleMainDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除采购单 "${row.purchaseNo}" 吗？关联的所有明细也会被删除！`,
      '确认删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await deletePurchaseMain(row.id, currentUserId.value, adminLevel.value)
    ElMessage.info(res.msg)
    if (selectedMainNo.value === row.purchaseNo) clearFilter()
    loadMainData()
    loadDetailData()
  } catch { /* 取消 */ }
}

const handleDetailDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该明细吗？', '确认删除', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const res = await deletePurchaseDetail(row.id, currentUserId.value, adminLevel.value)
    ElMessage.info(res.msg)
    await loadDetailData()
    await recalcMainTotals(row.purchaseNo)
  } catch { /* 取消 */ }
}

const handleMainImport = () => mainFileInput.value.click()
const onMainFileSelect = async (event) => {
  const file = event.target.files[0]
  if (file) {
    const res = await importPurchaseMain(file)
    ElMessage.info(res.msg)
    loadMainData()
    event.target.value = ''
  }
}

const handleMainExport = async () => {
  const res = await exportPurchaseMain()
  const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = '采购主表数据.xlsx'
  document.body.appendChild(a); a.click()
  window.URL.revokeObjectURL(url); document.body.removeChild(a)
}

const handleDetailImport = () => detailFileInput.value.click()
const onDetailFileSelect = async (event) => {
  const file = event.target.files[0]
  if (file) {
    const res = await importPurchaseDetail(file)
    ElMessage.info(res.msg)
    loadDetailData()
    event.target.value = ''
  }
}

const handleDetailExport = async () => {
  const res = await exportPurchaseDetail()
  const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = '采购明细数据.xlsx'
  document.body.appendChild(a); a.click()
  window.URL.revokeObjectURL(url); document.body.removeChild(a)
}

const loadUserData = async () => {
  const res = await getUserList()
  if (res.code === 200) userList.value = res.data
}

const loadGoodsData = async () => {
  const res = await getGoodsList()
  if (res.code === 200) goodsList.value = res.data
}

const onGoodsChange = (goodsId) => {
  const goods = goodsList.value.find(g => g.id === goodsId)
  if (goods) {
    detailForm.value.goodsPrice = goods.price
  }
}

const calcTotalPrice = () => { /* 由 calcDisplayTotal 自动计算 */ }

onMounted(() => {
  role.value = localStorage.getItem('role') || ''
  adminLevel.value = parseInt(localStorage.getItem('adminLevel') || '0')
  currentUserId.value = parseInt(localStorage.getItem('userId') || '0')
  loadMainData()
  loadDetailData()
  loadUserData()
  loadGoodsData()
})
</script>

<style scoped>
.el-table .current-row > td {
  background-color: #ecf5ff !important;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
</style>
