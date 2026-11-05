import { createRouter, createWebHistory } from 'vue-router'
import { session } from '../stores/session'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
  { path: '/password', component: () => import('../views/PasswordView.vue'), meta: { public: true } },
  { path: '/signup', component: () => import('../views/SignUpView.vue'), meta: { public: true } },
  { path: '/dashboard', component: () => import('../views/DashboardView.vue') },
  { path: '/search', component: () => import('../views/SearchView.vue') },
  { path: '/cases/:id', component: () => import('../views/CaseDetailView.vue') },
  { path: '/customers/new', component: () => import('../views/NewCustomerView.vue') },
  { path: '/parts', component: () => import('../views/PartsView.vue') },
  { path: '/appointments', component: () => import('../views/AppointmentsView.vue') },
  { path: '/campaigns', component: () => import('../views/CampaignsView.vue') },
  { path: '/employees', component: () => import('../views/EmployeesView.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (!to.meta.public && !session.isAuthenticated) return '/login'
})

export default router
