<template>
  <div class="p-4">
    <h3>会员管理</h3>
    <div style="display: flex; gap: 10px; margin-bottom: 15px;">
      <el-button type="primary" @click="openAdd">新增</el-button>
      <el-button @click="handleExport">导出Excel</el-button>
    </div>
    <el-table :data="list" border style="margin-top:15px">
      <el-table-column label="编号" prop="id"/>
      <el-table-column label="会员编号" prop="memberNo"/>
      <el-table-column label="姓名" prop="name"/>
      <el-table-column label="电话" prop="phone"/>
      <el-table-column label="注册时间" prop="registerTime"/>
      <el-table-column label="备注" prop="remark"/>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button @click="openEdit(scope.row)">编辑</el-button>
          <el-button type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="show" title="会员" @close="show=false">
      <el-form :model="form">
        <el-form-item label="会员编号">
          <el-input v-model="form.memberNo" :disabled="isEdit"/>
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.name"/></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone"/></el-form-item>
        <el-form-item label="注册时间">
          <el-date-picker v-model="form.registerTime" type="date" placeholder="请选择日期" value-format="YYYY-MM-DD"/>
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
import { ref, onMounted } from 'vue'
import { getMemberList, addMember, updateMember, deleteMember, exportMember } from '@/api/member'
const list = ref([])
const show = ref(false)
const form = ref({})
const isEdit = ref(false)

const loadData = async () => {
  const res = await getMemberList()
  if (res.code === 200) list.value = res.data
}

const openAdd = () => { form.value = {}; isEdit.value = false; show.value = true }
const openEdit = (row) => { form.value = { ...row }; isEdit.value = true; show.value = true }

const submit = async () => {
  if (isEdit.value) {
    const res = await updateMember(form.value)
    alert(res.msg)
  } else {
    const res = await addMember(form.value)
    alert(res.msg)
  }
  show.value = false
  loadData()
}

const handleDelete = async (id) => {
  if (confirm('确定删除？')) {
    const res = await deleteMember(id)
    alert(res.msg)
    loadData()
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

loadData()
</script>