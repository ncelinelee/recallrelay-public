<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { session, roleLabels, switchRole, logout } from '../stores/session'
import IconGlyph from './IconGlyph.vue'
import LanguageToggle from './LanguageToggle.vue'

const route = useRoute()
const router = useRouter()
const mobileNav = ref(false)
const roleMenu = ref(false)
const searchText = ref('')
const useMock = import.meta.env.VITE_USE_MOCK === 'true'

const allNav = [
  { label: 'Dashboard', to: '/dashboard', icon: 'dashboard', roles: 'all' },
  { label: 'Search', to: '/search', icon: 'search', roles: 'all' },
  { label: 'Appointments', to: '/appointments', icon: 'calendar', roles: ['APPOINTMENT_COORDINATOR', 'SERVICE_ADVISOR', 'SERVICE_MANAGER'] },
  { label: 'Parts', to: '/parts', icon: 'parts', roles: ['PARTS_SPECIALIST', 'APPOINTMENT_COORDINATOR', 'SERVICE_ADVISOR', 'SERVICE_MANAGER'] },
  { label: 'Campaigns', to: '/campaigns', icon: 'campaign', roles: ['HEAD_OFFICE_ADMIN', 'HEAD_OFFICE_MANAGER', 'HEAD_OFFICE_STAFF', 'SERVICE_MANAGER', 'APPOINTMENT_COORDINATOR', 'SERVICE_ADVISOR', 'CALL_CENTER_AGENT'] },
  { label: 'Employees', to: '/employees', icon: 'employees', roles: ['HEAD_OFFICE_ADMIN', 'HEAD_OFFICE_MANAGER', 'SERVICE_MANAGER'] },
]

const nav = computed(() => allNav.filter((item) => item.roles === 'all' || item.roles.includes(session.role)))
const roleLabel = computed(() => roleLabels[session.role])
const avatarInitials = computed(() => String(session.employeeName || 'User').split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase())

function goSearch() {
  if (!searchText.value.trim()) return
  router.push({ path: '/search', query: { q: searchText.value.trim() } })
  searchText.value = ''
}
function onSwitchRole(role) {
  switchRole(role)
  roleMenu.value = false
  router.push('/dashboard')
}
function signOut() {
  logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ 'sidebar--open': mobileNav }">
      <div class="brand">
        <div class="brand__mark">RR</div>
        <div>
          <strong>RecallRelay</strong>
          <span>{{ $tr('Service Operations') }}</span>
        </div>
      </div>

      <div class="sidebar-context">
        <span class="context-label">{{ $tr('DEALERSHIP') }}</span>
        <strong>{{ session.dealershipName }}</strong>
        <small>{{ session.organizationCode }}</small>
      </div>

      <nav class="side-nav">
        <button
          v-for="item in nav"
          :key="item.to"
          class="side-nav__item"
          :class="{ 'side-nav__item--active': route.path.startsWith(item.to) }"
          @click="router.push(item.to); mobileNav = false"
        >
          <IconGlyph :name="item.icon" />
          <span>{{ $tr(item.label) }}</span>
        </button>
      </nav>

      <div class="sidebar-footer">
        <div class="system-line"><span></span> {{ $tr('System Operational') }}</div>
        <small>RecallRelay v1.6</small>
      </div>
    </aside>

    <div class="shell-main">
      <header class="topbar">
        <button class="menu-button" @click="mobileNav = !mobileNav">
          <span></span><span></span><span></span>
        </button>

        <form class="global-search" @submit.prevent="goSearch">
          <IconGlyph name="search" :size="17" />
          <input v-model="searchText" :placeholder="$tr('Search customer, VIN, recall no., part no.')" />
          <span class="search-key">↵</span>
        </form>

        <LanguageToggle class="topbar-language" />

        <div class="topbar-context">
          <span class="topbar-context__label">{{ $tr('Organization') }}</span>
          <strong>{{ session.organizationName }}</strong>
        </div>

        <div class="user-menu">
          <button class="user-menu__trigger" @click="roleMenu = !roleMenu">
            <span class="avatar">{{ avatarInitials }}</span>
            <span class="user-copy">
              <strong>{{ session.employeeName }}</strong>
              <small>{{ $tr(roleLabel) }}</small>
            </span>
            <span class="chevron">⌄</span>
          </button>
          <div v-if="roleMenu" class="user-popover">
            <div v-if="useMock" class="user-popover__section">
              <span class="popover-label">{{ $tr('Prototype role switch') }}</span>
              <button v-for="(label, role) in roleLabels" :key="role" @click="onSwitchRole(role)">
                <span>{{ $tr(label) }}</span>
                <small v-if="session.role === role">{{ $tr('Current') }}</small>
              </button>
            </div>
            <button class="signout-button" @click="signOut">
              <IconGlyph name="logout" :size="16" /> {{ $tr('Sign out') }}
            </button>
          </div>
        </div>
      </header>

      <main class="page-content">
        <slot />
      </main>
    </div>
  </div>
</template>
