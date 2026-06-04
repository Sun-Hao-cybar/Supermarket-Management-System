import request from '@/utils/request'

export const getGoodsList = () => request.get('/goods/list')
export const addGoods = (data) => request.post('/goods/add', data)
export const updateGoods = (data) => request.post('/goods/update', data)
export const deleteGoods = (id) => request.get('/goods/delete', { params: { id } })
export const importGoods = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/goods/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const exportGoods = () => request.get('/goods/export', { responseType: 'blob' })