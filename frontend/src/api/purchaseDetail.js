import request from '@/utils/request'

export const getPurchaseDetailList = () => request.get('/purchaseDetail/list')
export const addPurchaseDetail = (data) => request.post('/purchaseDetail/add', data)
export const updatePurchaseDetail = (data) => request.post('/purchaseDetail/update', data)
export const deletePurchaseDetail = (id) => request.get('/purchaseDetail/delete', { params: { id } })
export const importPurchaseDetail = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/purchaseDetail/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const exportPurchaseDetail = () => request.get('/purchaseDetail/export', { responseType: 'blob' })