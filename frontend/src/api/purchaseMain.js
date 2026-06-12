import request from '@/utils/request'

export const getPurchaseMainList = () => request.get('/purchaseMain/list')
export const addPurchaseMain = (data) => request.post('/purchaseMain/add', data)
export const updatePurchaseMain = (data, userId, adminLevel) => request.post('/purchaseMain/update', data, { params: { operatorUserId: userId, adminLevel } })
export const deletePurchaseMain = (id, userId, adminLevel) => request.get('/purchaseMain/delete', { params: { id, operatorUserId: userId, adminLevel } })
export const importPurchaseMain = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/purchaseMain/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const exportPurchaseMain = () => request.get('/purchaseMain/export', { responseType: 'blob' })