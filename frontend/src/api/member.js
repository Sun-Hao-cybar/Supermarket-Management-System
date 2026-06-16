import request from '@/utils/request'

export const getMemberList = () => request.get('/member/list')
export const addMember = (data) => request.post('/member/add', data)
export const updateMember = (data) => request.post('/member/update', data)
export const deleteMember = (id) => request.get('/member/delete', { params: { id } })
export const importMember = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/member/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const exportMember = () => request.get('/member/export', { responseType: 'blob' })