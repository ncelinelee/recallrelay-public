<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { loginAs, loginFromApi, roleLabels } from '../stores/session'
import { recallRelayApi } from '../services/api'
import LanguageToggle from '../components/LanguageToggle.vue'
import { tr } from '../i18n'

const route = useRoute()
const router = useRouter()
const organizationCode = ref('OHC-001-001')
const employeeId = ref('jlee')
const password = ref('')
const previewRole = ref('SERVICE_MANAGER')
const showPassword = ref(false)
const loading = ref(false)
const errorMessage = ref('')

const useMock = import.meta.env.VITE_USE_MOCK === 'true'

async function signIn() {
  errorMessage.value = ''

  if (!organizationCode.value.trim() || !employeeId.value.trim() || (!useMock && !password.value)) {
    errorMessage.value = tr('Please enter Organization Code, Employee ID and Password.')
    return
  }

  loading.value = true
  try {
    if (useMock) {
      // Prototype mode: credentials are intentionally not validated.
      // This keeps the Vue prototype usable before the Spring Boot backend is connected.
      loginAs(previewRole.value, {
        organizationCode: organizationCode.value.trim(),
        employeeId: employeeId.value.trim(),
      })
      await router.replace('/dashboard')
      return
    }

    const { data } = await recallRelayApi.login({
      organizationCode: organizationCode.value.trim(),
      employeeId: employeeId.value.trim(),
      password: password.value,
    })

    loginFromApi(data, { organizationCode: organizationCode.value.trim() })
    await router.replace(data.mustChangePassword ? '/password?mode=setup' : '/dashboard')
  } catch (error) {
    if (useMock) {
      errorMessage.value = tr('A prototype login error occurred. Refresh and try again.')
    } else if (error?.response?.status === 401) {
      errorMessage.value = tr('Check your Organization Code, Employee ID or Password.')
    } else if (error?.response?.status === 403) {
      errorMessage.value = tr('This account is inactive or cannot access the current organization.')
    } else {
      errorMessage.value = tr('Cannot connect to the Backend API. Check VITE_API_BASE_URL and server status.')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-layout">
    <section class="auth-brand">
      <div class="auth-brand__inner">
        <div class="auth-kicker">{{ $tr('AUTOMOTIVE RECALL WORKFLOW PLATFORM') }}</div>
        <div class="auth-logo"><span>{{ $tr('RR') }}</span> RecallRelay</div>
        <h1>{{ $tr('One recall case. One connected service workflow.') }}</h1>
        <p>{{ $tr('Customer, VIN, campaign, parts, contact, appointment and service progress in one dealership workspace.') }}</p>

        <div class="auth-metrics">
          <div><strong>842</strong><span>{{ $tr('Open cases') }}</span></div>
          <div><strong>18</strong><span>{{ $tr('Dealerships') }}</span></div>
          <div><strong>12</strong><span>{{ $tr('Active campaigns') }}</span></div>
        </div>

        <div class="garage-grid">
          <div class="garage-panel">
            <span>{{ $tr('CASE WORKFLOW') }}</span>
            <strong>{{ $tr('Structured & Proactive') }}</strong>
            <small>{{ $tr('Search → Context → Next Action') }}</small>
          </div>
          <div class="garage-panel">
            <span>{{ $tr('SYSTEM STATUS') }}</span>
            <strong>{{ $tr('Operational') }}</strong>
            <small>{{ $tr('Role + organization scope enabled') }}</small>
          </div>
        </div>
      </div>
    </section>

    <section class="auth-form-wrap">
      <form class="auth-form" @submit.prevent="signIn">
        <div class="auth-form__toolbar"><LanguageToggle /></div>
        <div class="auth-form__header">
          <div class="brand__mark brand__mark--large">RR</div>
          <div>
            <h2>{{ $tr('Employee Sign In') }}</h2>
            <p>{{ $tr('Organization-issued credentials') }}</p>
          </div>
        </div>

        <p v-if="useMock" class="demo-hint">
          <strong>{{ $tr('Prototype mode') }}</strong> — {{ $tr('Use the demo credentials below and choose a Preview Role. Backend authentication is bypassed only in this local test mode.') }}
        </p>
        <p v-if="route.query.passwordChanged === '1'" class="notice notice--success">{{ $tr('Password updated. Sign in with your new password.') }}</p>
        <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>

        <label>
          <span>{{ $tr('Organization Code') }}</span>
          <input v-model="organizationCode" autocomplete="organization" required />
        </label>

        <label>
          <span>{{ $tr('Employee ID') }}</span>
          <input v-model="employeeId" autocomplete="username" required />
        </label>

        <label>
          <span>{{ $tr('Password') }}</span>
          <div class="password-field">
            <input v-model="password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" :required="!useMock" />
            <button type="button" @click="showPassword = !showPassword">{{ $tr(showPassword ? 'Hide' : 'Show') }}</button>
          </div>
        </label>

        <label v-if="useMock" class="demo-role">
          <span>{{ $tr('Preview Role') }} <small>{{ $tr('prototype only') }}</small></span>
          <select v-model="previewRole">
            <option v-for="(label, role) in roleLabels" :key="role" :value="role">{{ $tr(label) }}</option>
          </select>
        </label>

        <button class="button button--primary button--full" type="submit" :disabled="loading">
          {{ $tr(loading ? 'Signing in…' : 'Sign In') }}
        </button>
        <button class="text-button login-forgot" type="button" @click="router.push('/password?mode=reset')">{{ $tr('Forgot password?') }}</button>
        <div class="auth-divider"><span>{{ $tr('or') }}</span></div>
        <button class="button button--secondary button--full create-account-button" type="button" @click="router.push('/signup')">{{ $tr('Create an account') }}</button>

        <div class="auth-policy">
          <span class="policy-dot"></span>
          <p>{{ $tr('Inactive accounts are blocked. Temporary-password users are redirected to Password Setup.') }}</p>
        </div>
      </form>
    </section>
  </div>
</template>
