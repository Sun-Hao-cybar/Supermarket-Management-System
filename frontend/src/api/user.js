import request from '@/utils/request'

export const getUserList = () => request.get('/user/list')
export const addUser = (data) => request.post('/user/add', data)
export const updateUser = (data) => request.post('/user/update', data)
export const deleteUser = (id) => request.get('/user/delete', { params: { id } })
export const login = (username, password) => request.post('/user/login', null, { params: { username, password } })
export const register = (data) => request.post('/user/register', data)
export const getUserById = (id) => request.get('/user/getById', { params: { id } })
export const importUser = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const exportUser = () => request.get('/user/export', { responseType: 'blob' })
export const checkHasEmployees = () => request.get('/user/checkHasEmployees')