<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { recallRelayApi } from '../services/api'
import { addApprovedManager, addPendingRegistration } from '../stores/registration'
import LanguageToggle from '../components/LanguageToggle.vue'
import { tr } from '../i18n'

const router = useRouter()
const useMock = import.meta.env.VITE_USE_MOCK === 'true'
const accountType = ref('EMPLOYEE')
const step = ref('FORM')
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const verificationCode = ref('')
const demoVerificationCode = ref('')
// Preview-only values are generated locally and never authorize backend access.
const demoManagerCode = crypto.randomUUID()

const form = ref({
  organizationCode: 'OHC-001-001',
  employeeId: '',
  name: '',
  email: '',
  phone: '',
  dealershipNo: '12',
  department: 'SERVICE',
  role: 'SERVICE_ADVISOR',
  password: '',
  confirmPassword: '',
  managerType: 'SERVICE_MANAGER',
  oneTimeCode: '',
})

const roleOptions = computed(() => {
  if (form.value.department === 'SERVICE') {
    return [
      ['SERVICE_ADVISOR', 'Service Advisor'],
      ['APPOINTMENT_COORDINATOR', 'Appointment Coordinator'],
      ['TECHNICIAN', 'Technician'],
    ]
  }
  if (form.value.department === 'PARTS') return [['PARTS_SPECIALIST', 'Parts Specialist']]
  return [
    ['HEAD_OFFICE_STAFF', 'Head Office Staff'],
    ['CALL_CENTER_AGENT', 'Call Center Agent'],
  ]
})

const managerNeedsDealership = computed(() => form.value.managerType === 'SERVICE_MANAGER')

watch(() => form.value.department, () => {
  form.value.role = roleOptions.value[0]?.[0] || ''
})
watch(() => form.value.managerType, (value) => {
  form.value.department = value === 'SERVICE_MANAGER' ? 'SERVICE' : 'HEAD_OFFICE'
})

function validateCommon() {
  errorMessage.value = ''
  if (!form.value.organizationCode.trim() || !form.value.employeeId.trim() || !form.value.name.trim() || !form.value.email.trim() || !form.value.phone.trim()) {
    errorMessage.value = tr('Please enter Organization Code, Employee ID, Name, Company Email and Phone Number.')
    return false
  }
  if (useMock && !['@northstar.example', '@northstar.com'].some((domain) => form.value.email.toLowerCase().endsWith(domain))) {
    errorMessage.value = tr('In prototype mode, use a @northstar.com or @northstar.example company email.')
    return false
  }
  if (!form.value.password || form.value.password.length < 8) {
    errorMessage.value = tr('Password must be at least 8 characters.')
    return false
  }
  if (form.value.password !== form.value.confirmPassword) {
    errorMessage.value = tr('Password and Confirm Password do not match.')
    return false
  }
  return true
}

async function requestEmployeeVerification() {
  if (!validateCommon()) return
  if (form.value.department !== 'HEAD_OFFICE' && !form.value.dealershipNo.trim()) {
    errorMessage.value = tr('Service / Parts employees must enter a Dealership Number.')
    return
  }

  loading.value = true
  try {
    if (useMock) {
      demoVerificationCode.value = String(100000 + crypto.getRandomValues(new Uint32Array(1))[0] % 900000)
    } else {
      const { data } = await recallRelayApi.startEmployeeRegistration({
        organizationCode: form.value.organizationCode.trim(),
        employeeId: form.value.employeeId.trim(),
        employeeName: form.value.name.trim(),
        companyEmail: form.value.email.trim(),
        phone: form.value.phone.trim(),
        dealershipId: form.value.department === 'HEAD_OFFICE' ? null : Number(form.value.dealershipNo),
        department: form.value.department,
        requestedRole: form.value.role,
        password: form.value.password,
      })
      demoVerificationCode.value = data?.devVerificationCode || ''
    }
    step.value = 'VERIFY_EMAIL'
    successMessage.value = tr('A 6-digit verification code was sent to your company email.')
  } catch (e) {
    errorMessage.value = e?.response?.data?.message || e.message || tr('Could not send the verification code.')
  } finally {
    loading.value = false
  }
}

async function verifyEmployeeEmail() {
  errorMessage.value = ''
  if (!verificationCode.value.trim()) {
    errorMessage.value = tr('Enter the verification code.')
    return
  }
  loading.value = true
  try {
    if (useMock) {
      if (verificationCode.value.trim() !== demoVerificationCode.value) throw new Error(tr('The verification code does not match.'))
      addPendingRegistration({
        organizationCode: form.value.organizationCode.trim(),
        employeeId: form.value.employeeId.trim(),
        name: form.value.name.trim(),
        email: form.value.email.trim(),
        phone: form.value.phone.trim(),
        dealershipId: form.value.department === 'HEAD_OFFICE' ? null : Number(form.value.dealershipNo),
        dealershipNo: form.value.department === 'HEAD_OFFICE' ? null : form.value.dealershipNo.trim(),
        dealershipName: form.value.department === 'HEAD_OFFICE' ? 'Head Office' : (Number(form.value.dealershipNo) === 12 ? 'Downtown Subaru' : `Dealership ${form.value.dealershipNo}`),
        department: form.value.department,
        role: form.value.role,
      })
    } else {
      await recallRelayApi.verifyEmployeeRegistration({
        organizationCode: form.value.organizationCode.trim(),
        employeeId: form.value.employeeId.trim(),
        verificationCode: verificationCode.value.trim(),
      })
    }
    step.value = 'PENDING_APPROVAL'
    successMessage.value = tr('Company Email verification is complete.')
  } catch (e) {
    errorMessage.value = e?.response?.data?.message || e.message || tr('Company email verification failed.')
  } finally {
    loading.value = false
  }
}

async function registerManager() {
  if (!validateCommon()) return
  if (!form.value.oneTimeCode.trim()) {
    errorMessage.value = tr('Enter the company-issued One-time Manager Code.')
    return
  }
  if (managerNeedsDealership.value && !form.value.dealershipNo.trim()) {
    errorMessage.value = tr('A Service Manager must enter a Dealership Number.')
    return
  }

  loading.value = true
  try {
    if (useMock) {
      const validService = form.value.managerType === 'SERVICE_MANAGER' && form.value.employeeId.trim() === 'smgr-demo-01' && form.value.oneTimeCode.trim() === demoManagerCode
      const validHeadOffice = form.value.managerType === 'HEAD_OFFICE_MANAGER' && form.value.employeeId.trim() === 'homgr-demo-01' && form.value.oneTimeCode.trim() === demoManagerCode
      if (!validService && !validHeadOffice) throw new Error(tr('Employee ID and One-time Manager Code do not match.'))
      addApprovedManager({
        organizationCode: form.value.organizationCode.trim(),
        employeeId: form.value.employeeId.trim(),
        name: form.value.name.trim(),
        email: form.value.email.trim(),
        phone: form.value.phone.trim(),
        dealershipId: managerNeedsDealership.value ? Number(form.value.dealershipNo) : null,
        dealershipNo: managerNeedsDealership.value ? form.value.dealershipNo.trim() : null,
        dealershipName: managerNeedsDealership.value ? 'Downtown Subaru' : 'Head Office',
        department: managerNeedsDealership.value ? 'SERVICE' : 'HEAD_OFFICE',
        role: form.value.managerType,
      })
    } else {
      await recallRelayApi.registerManager({
        organizationCode: form.value.organizationCode.trim(),
        employeeId: form.value.employeeId.trim(),
        employeeName: form.value.name.trim(),
        companyEmail: form.value.email.trim(),
        phone: form.value.phone.trim(),
        managerRole: form.value.managerType,
        dealershipId: managerNeedsDealership.value ? Number(form.value.dealershipNo) : null,
        oneTimeCode: form.value.oneTimeCode.trim(),
        password: form.value.password,
      })
    }
    step.value = 'MANAGER_APPROVED'
    successMessage.value = tr('The company-issued code was verified and the manager account was approved automatically.')
  } catch (e) {
    errorMessage.value = e?.response?.data?.message || e.message || tr('Manager account registration failed.')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="simple-auth signup-auth">
    <div class="simple-auth__card signup-card">
      <div class="simple-auth__toolbar"><LanguageToggle /></div>
      <div class="brand auth-simple-brand">
        <div class="brand__mark">RR</div>
        <div><strong>RecallRelay</strong><span>{{ $tr('Employee Registration') }}</span></div>
      </div>

      <template v-if="step === 'FORM'">
        <div class="signup-type-switch">
          <button :class="{ active: accountType === 'EMPLOYEE' }" @click="accountType='EMPLOYEE'">{{ $tr('Employee') }}</button>
          <button :class="{ active: accountType === 'MANAGER' }" @click="accountType='MANAGER'">{{ $tr('Manager') }}</button>
        </div>

        <h1>{{ $tr('Create employee account') }}</h1>
        <p v-if="accountType === 'EMPLOYEE'">{{ $tr('Company email verification is required. Your account becomes active after your direct manager approves the request.') }}</p>
        <p v-else>{{ $tr('Manager accounts require a company-issued one-time code and matching Employee ID. Valid manager requests are approved automatically.') }}</p>

        <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>

        <div class="signup-grid">
          <label class="field"><span>{{ $tr('Organization Code') }}</span><input v-model="form.organizationCode" /></label>
          <label class="field"><span>{{ $tr('Employee ID') }}</span><input v-model="form.employeeId" :placeholder="$tr('Company employee number')" /></label>
          <label class="field"><span>{{ $tr('Name') }}</span><input v-model="form.name" /></label>
          <label class="field"><span>{{ $tr('Phone Number') }}</span><input v-model="form.phone" /></label>
          <label class="field signup-span-2"><span>{{ $tr('Company Email') }}</span><input v-model="form.email" type="email" :placeholder="$tr('name@company.com')" /></label>

          <template v-if="accountType === 'EMPLOYEE'">
            <label v-if="form.department !== 'HEAD_OFFICE'" class="field"><span>{{ $tr('Dealership Number') }}</span><input v-model="form.dealershipNo" inputmode="numeric" /></label>
            <label class="field" :class="{ 'signup-span-2': form.department === 'HEAD_OFFICE' }"><span>{{ $tr('Department') }}</span>
              <select v-model="form.department">
                <option value="SERVICE">{{ $tr('Service') }}</option>
                <option value="PARTS">{{ $tr('Parts Department') }}</option>
                <option value="HEAD_OFFICE">{{ $tr('Head Office') }}</option>
              </select>
            </label>
            <label class="field signup-span-2"><span>{{ $tr('Position') }}</span>
              <select v-model="form.role">
                <option v-for="item in roleOptions" :key="item[0]" :value="item[0]">{{ $tr(item[1]) }}</option>
              </select>
            </label>
          </template>

          <template v-else>
            <label class="field"><span>{{ $tr('Manager Type') }}</span>
              <select v-model="form.managerType">
                <option value="SERVICE_MANAGER">{{ $tr('Service Manager') }}</option>
                <option value="HEAD_OFFICE_MANAGER">{{ $tr('Head Office Manager') }}</option>
              </select>
            </label>
            <label v-if="managerNeedsDealership" class="field"><span>{{ $tr('Dealership Number') }}</span><input v-model="form.dealershipNo" inputmode="numeric" /></label>
            <label class="field" :class="{ 'signup-span-2': !managerNeedsDealership }"><span>{{ $tr('One-time Manager Code') }}</span><input v-model="form.oneTimeCode" type="password" autocomplete="one-time-code" /></label>
          </template>

          <label class="field"><span>{{ $tr('Password') }}</span><input v-model="form.password" type="password" autocomplete="new-password" /></label>
          <label class="field"><span>{{ $tr('Confirm Password') }}</span><input v-model="form.confirmPassword" type="password" autocomplete="new-password" /></label>
        </div>

        <div v-if="useMock && accountType === 'MANAGER'" class="demo-hint compact-demo">
          {{ $tr('Prototype manager test:') }} smgr-demo-01 / homgr-demo-01: <strong>{{ demoManagerCode }}</strong>
        </div>

        <button v-if="accountType === 'EMPLOYEE'" class="button button--primary button--full" :disabled="loading" @click="requestEmployeeVerification">
          {{ $tr(loading ? 'Sending…' : 'Send Company Email Verification Code') }}
        </button>
        <button v-else class="button button--primary button--full" :disabled="loading" @click="registerManager">
          {{ $tr(loading ? 'Verifying…' : 'Verify Code & Create Manager Account') }}
        </button>
      </template>

      <template v-else-if="step === 'VERIFY_EMAIL'">
        <h1>{{ $tr('Verify company email') }}</h1>
        <p>{{ $tr('Enter the 6-digit code sent to') }} <strong>{{ form.email }}</strong>.</p>
        <p v-if="successMessage" class="notice notice--success">{{ successMessage }}</p>
        <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>
        <label class="field"><span>{{ $tr('Verification Code') }}</span><input v-model="verificationCode" inputmode="numeric" maxlength="6" :placeholder="$tr('6-digit code')" /></label>
        <div v-if="useMock && demoVerificationCode" class="demo-hint compact-demo">{{ $tr('Prototype verification code:') }} <strong>{{ demoVerificationCode }}</strong></div>
        <button class="button button--primary button--full" :disabled="loading" @click="verifyEmployeeEmail">{{ $tr('Verify & Submit Registration') }}</button>
        <button class="text-button" @click="step='FORM'">{{ $tr('Back to registration') }}</button>
      </template>

      <template v-else-if="step === 'PENDING_APPROVAL'">
        <div class="registration-state registration-state--pending">
          <span>✓</span>
          <h1>{{ $tr('Manager approval pending') }}</h1>
          <p>{{ $tr('Your company email has been verified. Your direct manager can now review the registration request in Employee Management.') }}</p>
          <strong>{{ form.employeeId }} · {{ form.email }}</strong>
        </div>
        <button class="button button--primary button--full" @click="router.push('/login')">{{ $tr('Back to Sign In') }}</button>
      </template>

      <template v-else>
        <div class="registration-state registration-state--approved">
          <span>✓</span>
          <h1>{{ $tr('Manager account approved') }}</h1>
          <p>{{ $tr('The company-issued Employee ID and one-time manager code were verified. No additional manager approval is required.') }}</p>
        </div>
        <button class="button button--primary button--full" @click="router.push('/login')">{{ $tr('Continue to Sign In') }}</button>
      </template>

      <button v-if="step === 'FORM'" class="text-button" type="button" @click="router.push('/login')">{{ $tr('Already have an account? Sign In') }}</button>
    </div>
  </div>
</template>
