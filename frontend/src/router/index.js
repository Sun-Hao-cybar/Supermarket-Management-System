import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Login', component: () => import('@/views/Login.vue') },
  {
    path: '/layout',
    component: () => import('@/views/Layout.vue'),
    children: [
      { path: 'user-info', component: () => import('@/views/UserInfo.vue') },
      { path: 'goods', component: () => import('@/views/Goods.vue') },
      { path: 'purchase', component: () => import('@/views/Purchase.vue') },
      { path: 'supplier', component: () => import('@/views/Supplier.vue') },
      { path: 'employee', component: () => import('@/views/Employee.vue') },
      { path: 'member', component: () => import('@/views/Member.vue') },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router