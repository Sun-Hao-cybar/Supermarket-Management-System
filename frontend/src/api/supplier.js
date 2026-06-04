import request from '@/utils/request'

export const getSupplierList = () => request.get('/supplier/list')
export const addSupplier = (data) => request.post('/supplier/add', data)
export const updateSupplier = (data) => request.post('/supplier/update', data)
export const deleteSupplier = (id) => request.get('/supplier/delete', { params: { id } })
export const importSupplier = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/supplier/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const exportSupplier = () => request.get('/supplier/export', { responseType: 'blob' })