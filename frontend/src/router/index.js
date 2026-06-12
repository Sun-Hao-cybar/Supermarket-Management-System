import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Login', component: () => import('@/views/Login.vue') },
  {
    path: '/layout',
    component: () => import('@/views/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: 'user-info', component: () => import('@/views/UserInfo.vue') },
      { path: 'goods', component: () => import('@/views/Goods.vue') },
      { path: 'purchase', component: () => import('@/views/Purchase.vue') },
      { path: 'supplier', component: () => import('@/views/Supplier.vue') },
      { path: 'employee', component: () => import('@/views/Employee.vue') },
      { path: 'member', component: () => import('@/views/Member.vue') },
    ]
  },
  // 404 兜底
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录重定向到登录页
router.beforeEach((to, from, next) => {
  if (to.matched.some(record => record.meta.requiresAuth)) {
    const userId = localStorage.getItem('userId')
    if (!userId) {
      next('/')
      return
    }
  }
  next()
})

export default router
