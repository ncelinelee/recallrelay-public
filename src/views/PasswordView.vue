<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { recallRelayApi } from '../services/api'
import { logout, session } from '../stores/session'
import { employeeRows } from '../data/mock'
import LanguageToggle from '../components/LanguageToggle.vue'
import { tr } from '../i18n'

const route = useRoute()
const router = useRouter()
const mode = ref(route.query.mode === 'setup' ? 'setup' : 'reset')
const organizationCode = ref('OHC-001-001')
const email = ref('')
const resetToken = ref(String(route.query.token || ''))
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const sent = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const demoResetUrl = ref('')

const useMock = import.meta.env.VITE_USE_MOCK === 'true'
const isSetup = computed(() => mode.value === 'setup')
const isResetConfirm = computed(() => mode.value === 'reset' && Boolean(resetToken.value))

const mockEmails = new Set([
  ...employeeRows.map((row) => String(row.email || '').toLowerCase()),
  'jlee@northstar.example',
  'maria@northstar.example',
  'parts01@northstar.example',
  'advisor01@northstar.example',
  'tech01@northstar.example',
  'admin@northstar.example',
])

function validateNewPassword() {
  if (!newPassword.value || !confirmPassword.value) {
    errorMessage.value = tr('Enter the new password and Confirm Password.')
    return false
  }
  if (newPassword.value.length < 8) {
    errorMessage.value = tr('The new password must be at least 8 characters.')
    return false
  }
  if (newPassword.value !== confirmPassword.value) {
    errorMessage.value = tr('The new password and Confirm Password do not match.')
    return false
  }
  return true
}

async function requestReset() {
  errorMessage.value = ''
  successMessage.value = ''
  demoResetUrl.value = ''
  if (!organizationCode.value.trim() || !email.value.trim()) {
    errorMessage.value = tr('Enter the Organization Code and registered Company Email.')
    return
  }

  loading.value = true
  try {
    if (useMock) {
      if (mockEmails.has(email.value.trim().toLowerCase())) {
        demoResetUrl.value = `/password?mode=reset&token=demo-reset-${encodeURIComponent(email.value.trim().toLowerCase())}`
      }
    } else {
      const { data } = await recallRelayApi.requestPasswordReset({
        organizationCode: organizationCode.value.trim(),
        companyEmail: email.value.trim(),
      })
      demoResetUrl.value = data?.devResetUrl || ''
    }
    sent.value = true
    // Always use a generic response so an attacker cannot discover registered employee emails.
    successMessage.value = tr('If the information matches a registered account, a password reset link will be sent to the Company Email.')
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || tr('Could not process the password reset request.')
  } finally {
    loading.value = false
  }
}

async function confirmReset() {
  errorMessage.value = ''
  successMessage.value = ''
  if (!validateNewPassword()) return
  loading.value = true
  try {
    if (!useMock) {
      await recallRelayApi.confirmPasswordReset({
        token: resetToken.value,
        newPassword: newPassword.value,
      })
    }
    successMessage.value = tr('Password reset complete. Sign in with your new password.')
    setTimeout(() => router.replace({ path: '/login', query: { passwordChanged: '1' } }), 700)
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || tr('The reset link has expired or has already been used.')
  } finally {
    loading.value = false
  }
}

async function savePassword() {
  errorMessage.value = ''
  successMessage.value = ''
  if (!currentPassword.value) {
    errorMessage.value = tr('Enter the Temporary / Current Password.')
    return
  }
  if (!validateNewPassword()) return
  if (newPassword.value === currentPassword.value) {
    errorMessage.value = tr('The new password must be different from the temporary/current password.')
    return
  }

  loading.value = true
  try {
    await recallRelayApi.changePassword({
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
    })
    successMessage.value = tr('Password changed. Sign in again with the new password.')
    logout()
    setTimeout(() => router.replace({ path: '/login', query: { passwordChanged: '1' } }), 700)
  } catch (error) {
    if (error?.response?.status === 400) errorMessage.value = error?.response?.data?.message || tr('Check the current password or new password.')
    else if (error?.response?.status === 401) errorMessage.value = tr('Authentication expired. Sign in again and retry.')
    else if (error?.response?.status === 403) errorMessage.value = tr('This account cannot change its password.')
    else errorMessage.value = tr('Cannot connect to the password change API. Check Backend status.')
  } finally {
    loading.value = false
  }
}

function goReset() {
  mode.value = 'reset'
  resetToken.value = ''
  sent.value = false
  errorMessage.value = ''
  successMessage.value = ''
}
function goSetup() {
  mode.value = 'setup'
  errorMessage.value = ''
  successMessage.value = ''
}
function openDemoReset() {
  if (!demoResetUrl.value) return
  const token = demoResetUrl.value.split('token=')[1] || ''
  resetToken.value = decodeURIComponent(token)
  router.replace({ path: '/password', query: { mode: 'reset', token: resetToken.value } })
}
</script>

<template>
  <div class="simple-auth">
    <div class="simple-auth__card">
      <div class="simple-auth__toolbar"><LanguageToggle /></div>
      <div class="brand auth-simple-brand">
        <div class="brand__mark">RR</div>
        <div><strong>RecallRelay</strong><span>{{ $tr('Password Security') }}</span></div>
      </div>

      <div class="simple-auth__tabs">
        <button :class="{ active: mode === 'reset' }" @click="goReset">{{ $tr('Reset') }}</button>
        <button :class="{ active: mode === 'setup' }" @click="goSetup">{{ $tr('First-time Setup') }}</button>
      </div>

      <template v-if="mode === 'reset' && !isResetConfirm">
        <h1>{{ $tr('Reset password') }}</h1>
        <p>{{ $tr('Enter your Organization Code and the Company Email registered in Employee Management.') }}</p>
        <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>
        <p v-if="successMessage" class="notice notice--success">{{ successMessage }}</p>

        <label class="field"><span>{{ $tr('Organization Code') }}</span><input v-model="organizationCode" /></label>
        <label class="field"><span>{{ $tr('Registered Company Email') }}</span><input v-model="email" type="email" :placeholder="$tr('name@company.com')" /></label>
        <button class="button button--primary button--full" :disabled="loading" @click="requestReset">{{ $tr(loading ? 'Sending…' : 'Send Reset Link') }}</button>

        <div v-if="useMock && sent" class="demo-hint compact-demo">
          {{ $tr('Prototype note: only a known Employee Management email produces a demo reset link.') }}
          <button v-if="demoResetUrl" class="text-button" @click="openDemoReset">{{ $tr('Open Demo Reset Link') }}</button>
        </div>
      </template>

      <template v-else-if="mode === 'reset' && isResetConfirm">
        <h1>{{ $tr('Create a new password') }}</h1>
        <p>{{ $tr('This reset link can be used once before it expires.') }}</p>
        <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>
        <p v-if="successMessage" class="notice notice--success">{{ successMessage }}</p>
        <label class="field"><span>{{ $tr('New Password') }}</span><input v-model="newPassword" type="password" autocomplete="new-password" /></label>
        <label class="field"><span>{{ $tr('Confirm Password') }}</span><input v-model="confirmPassword" type="password" autocomplete="new-password" /></label>
        <button class="button button--primary button--full" :disabled="loading" @click="confirmReset">{{ $tr('Reset Password') }}</button>
      </template>

      <template v-else>
        <h1>{{ $tr('Set a new password') }}</h1>
        <p>{{ $tr('Temporary-password users must change their password before entering business screens.') }}</p>
        <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>
        <p v-if="successMessage" class="notice notice--success">{{ successMessage }}</p>
        <div class="notice" v-if="session.employeeId">{{ $tr('Signed in as') }} <strong>{{ session.employeeId }}</strong>. {{ $tr('Enter the temporary/current password once more.') }}</div>
        <label class="field"><span>{{ $tr('Temporary / Current Password') }}</span><input v-model="currentPassword" type="password" autocomplete="current-password" /></label>
        <label class="field"><span>{{ $tr('New Password') }}</span><input v-model="newPassword" type="password" autocomplete="new-password" /></label>
        <label class="field"><span>{{ $tr('Confirm Password') }}</span><input v-model="confirmPassword" type="password" autocomplete="new-password" /></label>
        <button class="button button--primary button--full" :disabled="loading" @click="savePassword">{{ $tr(loading ? 'Saving…' : 'Save New Password') }}</button>
      </template>

      <button class="text-button" @click="router.push('/login')">{{ $tr('Back to Sign In') }}</button>
    </div>
  </div>
</template>
