<script setup>
import { computed, onMounted, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import PageHeader from '../components/PageHeader.vue'
import PanelCard from '../components/PanelCard.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { employeeRows } from '../data/mock'
import { recallRelayApi } from '../services/api'
import { roleLabels, session } from '../stores/session'
import { registrationState, approvePendingRegistration, rejectPendingRegistration } from '../stores/registration'
import { tr } from '../i18n'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'
const q = ref('')
const showAdd = ref(false)
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const error = ref('')
const rows = ref([])
const selected = ref(null)
const realPending = ref([])
const pendingLoading = ref(false)

const dealershipOptions = [
  { id: 12, name: 'Downtown Subaru' },
  { id: 13, name: 'Northgate Subaru' },
  { id: 14, name: 'Riverside Subaru' },
]

const scopeLabel = computed(() => {
  if (session.role === 'HEAD_OFFICE_ADMIN') return 'Organization scope'
  if (session.role === 'HEAD_OFFICE_MANAGER') return 'Head Office scope'
  return 'Assigned dealership only'
})
const pendingRegistrations = computed(() => {
  const source = useMock ? registrationState.pending : realPending.value
  if (session.role === 'SERVICE_MANAGER') return source.filter((row) => Number(row.dealershipId) === Number(session.dealershipId))
  if (session.role === 'HEAD_OFFICE_MANAGER') return source.filter((row) => row.department === 'HEAD_OFFICE')
  return session.role === 'HEAD_OFFICE_ADMIN' ? source : []
})
const filtered = computed(() => {
  const needle = q.value.trim().toLowerCase()
  if (!needle) return rows.value
  return rows.value.filter((e) => `${e.login} ${e.name} ${e.email} ${e.role}`.toLowerCase().includes(needle))
})
const canDeactivateSelected = computed(() => selected.value && Number(selected.value.id) !== Number(session.employeeDbId) && selected.value.active)
const canReactivateSelected = computed(() => selected.value && !selected.value.active)

function isRoleAssignable(role) {
  if (session.role === 'SERVICE_MANAGER') return !['HEAD_OFFICE_ADMIN', 'HEAD_OFFICE_MANAGER', 'HEAD_OFFICE_STAFF', 'CALL_CENTER_AGENT', 'SERVICE_MANAGER'].includes(role)
  if (session.role === 'HEAD_OFFICE_MANAGER') return ['HEAD_OFFICE_STAFF', 'CALL_CENTER_AGENT'].includes(role)
  return true
}

const newEmployee = ref({
  login: '',
  name: '',
  email: '',
  dealershipId: session.dealershipId || 12,
  role: 'APPOINTMENT_COORDINATOR',
  password: '',
})

function normalizeApiRow(row) {
  return {
    id: row.id,
    login: row.employeeLoginId,
    name: row.employeeName,
    email: row.companyEmail,
    dealershipId: row.dealershipId,
    dealership: row.dealershipName || 'Head Office',
    role: row.role,
    active: row.active,
  }
}

function normalizeMockRow(row) {
  const names = {
    mpark: 'Mina Park',
    jporter: 'Jordan Porter',
    slee: 'Sarah Lee',
    kadams: 'Kevin Adams',
    jlee: 'Celine Lee',
  }
  return { ...row, name: row.name || names[row.login] || row.login, dealershipId: row.dealershipId ?? null, department: row.department || (row.dealershipId == null ? 'HEAD_OFFICE' : 'SERVICE') }
}

function selectRow(row) {
  selected.value = { ...row }
  message.value = ''
  error.value = ''
}

async function loadEmployees(keepId = null) {
  loading.value = true
  error.value = ''
  try {
    if (useMock) {
      const approved = registrationState.approved.map((row) => ({
        id: row.id,
        login: row.employeeId,
        name: row.name,
        email: row.email,
        dealershipId: row.dealershipId,
        dealership: row.dealershipName || (row.department === 'HEAD_OFFICE' ? 'Head Office' : 'Downtown Subaru'),
        role: row.role,
        active: true,
      }))
      const allMockRows = [...employeeRows.map(normalizeMockRow), ...approved]
      if (session.role === 'SERVICE_MANAGER') rows.value = allMockRows.filter((row) => Number(row.dealershipId) === Number(session.dealershipId))
      else if (session.role === 'HEAD_OFFICE_MANAGER') rows.value = allMockRows.filter((row) => row.department === 'HEAD_OFFICE' && row.role !== 'HEAD_OFFICE_ADMIN')
      else rows.value = allMockRows
    } else {
      const { data } = await recallRelayApi.getEmployees()
      rows.value = data.map(normalizeApiRow)
    }
    const targetId = keepId ?? selected.value?.id ?? rows.value[0]?.id
    const target = rows.value.find((row) => Number(row.id) === Number(targetId)) || rows.value[0] || null
    selected.value = target ? { ...target } : null
  } catch (e) {
    error.value = e?.response?.data?.message || tr('Could not load the employee list.')
  } finally {
    loading.value = false
  }
}

async function loadPendingRegistrations() {
  if (useMock || !['HEAD_OFFICE_ADMIN', 'HEAD_OFFICE_MANAGER', 'SERVICE_MANAGER'].includes(session.role)) return
  pendingLoading.value = true
  try {
    const { data } = await recallRelayApi.getPendingRegistrations()
    realPending.value = Array.isArray(data) ? data : []
  } catch (e) {
    error.value = e?.response?.data?.message || tr('Could not load pending sign-up requests.')
  } finally {
    pendingLoading.value = false
  }
}

async function approveRegistration(row) {
  const ok = window.confirm(`${row.name || row.employeeName} (${row.employeeId || row.employeeLoginId}) ${tr('Approve this sign-up request?')}`)
  if (!ok) return
  error.value = ''
  message.value = ''
  try {
    if (useMock) approvePendingRegistration(row.id, session.employeeId)
    else await recallRelayApi.approveRegistration(row.id)
    message.value = tr('Sign-up request approved.')
    await loadEmployees()
    await loadPendingRegistrations()
  } catch (e) {
    error.value = e?.response?.data?.message || e.message || tr('Could not approve the sign-up request.')
  }
}

async function rejectRegistration(row) {
  const ok = window.confirm(`${row.name || row.employeeName} (${row.employeeId || row.employeeLoginId}) ${tr('Reject this sign-up request?')}`)
  if (!ok) return
  error.value = ''
  message.value = ''
  try {
    if (useMock) rejectPendingRegistration(row.id, session.employeeId)
    else await recallRelayApi.rejectRegistration(row.id)
    message.value = tr('Sign-up request rejected.')
    await loadPendingRegistrations()
  } catch (e) {
    error.value = e?.response?.data?.message || e.message || tr('Could not reject the sign-up request.')
  }
}

async function saveEmployee() {
  if (!selected.value) return
  if (!selected.value.name?.trim() || !selected.value.email?.trim()) {
    error.value = tr('Employee Name and Company Email are required.')
    return
  }

  saving.value = true
  error.value = ''
  message.value = ''
  try {
    if (useMock) {
      const index = rows.value.findIndex((row) => row.id === selected.value.id)
      if (index >= 0) rows.value[index] = { ...selected.value }
    } else {
      const payload = {
        employeeName: selected.value.name.trim(),
        companyEmail: selected.value.email.trim(),
        dealershipId: selected.value.dealershipId,
        role: selected.value.role,
        active: Boolean(selected.value.active),
      }
      const { data } = await recallRelayApi.updateEmployee(selected.value.id, payload)
      const updated = normalizeApiRow(data)
      await loadEmployees(updated.id)
    }
    message.value = tr('Employee information saved.')
  } catch (e) {
    error.value = e?.response?.data?.message || tr('Could not save employee information.')
  } finally {
    saving.value = false
  }
}

async function deactivateEmployee() {
  if (!selected.value || !canDeactivateSelected.value) return
  const ok = window.confirm(`${selected.value.name} (${selected.value.login}) ${tr('Deactivate this account?')}\n\n${tr('Sign-in will be blocked, but historical Case / Interaction links will remain.')}`)
  if (!ok) return
  selected.value.active = false
  await saveEmployee()
  if (!error.value) message.value = tr('Account deactivated. Historical work links remain intact.')
}

async function reactivateEmployee() {
  if (!selected.value || !canReactivateSelected.value) return
  selected.value.active = true
  await saveEmployee()
  if (!error.value) message.value = tr('Account reactivated.')
}

async function createEmployee() {
  const form = newEmployee.value
  if (!form.login.trim() || !form.name.trim() || !form.email.trim() || !form.password) {
    error.value = tr('Employee ID, Name, Email and Temporary Password are required.')
    return
  }

  saving.value = true
  error.value = ''
  message.value = ''
  try {
    if (useMock) {
      const created = {
        id: Date.now(),
        login: form.login.trim(),
        name: form.name.trim(),
        email: form.email.trim(),
        dealershipId: form.dealershipId,
        dealership: dealershipOptions.find((d) => d.id === Number(form.dealershipId))?.name || 'Downtown Subaru',
        role: form.role,
        active: true,
      }
      rows.value.push(created)
      selected.value = { ...created }
    } else {
      const payload = {
        employeeLoginId: form.login.trim(),
        employeeName: form.name.trim(),
        companyEmail: form.email.trim(),
        dealershipId: Number(form.dealershipId),
        role: form.role,
        temporaryPassword: form.password,
      }
      const { data } = await recallRelayApi.createEmployee(payload)
      await loadEmployees(data.id)
    }
    showAdd.value = false
    newEmployee.value = {
      login: '',
      name: '',
      email: '',
      dealershipId: session.dealershipId || 12,
      role: 'APPOINTMENT_COORDINATOR',
      password: '',
    }
    message.value = tr('Employee account created.')
  } catch (e) {
    error.value = e?.response?.data?.message || tr('Could not create the employee account.')
  } finally {
    saving.value = false
  }
}

onMounted(async () => { await loadEmployees(); await loadPendingRegistrations() })
</script>

<template>
  <AppShell>
    <PageHeader
      eyebrow="Account & organization"
      title="Employee Management"
      description="Issue employee accounts, update profile / role and deactivate access while preserving historical work records."
    >
      <span class="access-pill">{{ $tr(scopeLabel) }}</span>
      <button class="button button--primary" @click="showAdd=true">{{ $tr('+ Add Employee') }}</button>
    </PageHeader>

    <div v-if="message" class="employee-feedback employee-feedback--success">{{ message }}</div>
    <div v-if="error" class="employee-feedback employee-feedback--error">{{ error }}</div>

    <PanelCard
      v-if="['HEAD_OFFICE_ADMIN', 'HEAD_OFFICE_MANAGER', 'SERVICE_MANAGER'].includes(session.role)"
      class="pending-registration-panel"
      title="Pending Sign-ups"
      :subtitle="pendingLoading ? 'Loading registration requests…' : `${pendingRegistrations.length} request(s) waiting for manager approval.`"
    >
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>{{ $tr('Employee ID') }}</th><th>{{ $tr('Name') }}</th><th>{{ $tr('Company Email') }}</th><th>{{ $tr('Department / Position') }}</th><th>{{ $tr('Dealership') }}</th><th>{{ $tr('Requested') }}</th><th>{{ $tr('Action') }}</th></tr></thead>
          <tbody>
            <tr v-for="row in pendingRegistrations" :key="row.id">
              <td class="mono">{{ row.employeeId || row.employeeLoginId }}</td>
              <td>{{ row.name || row.employeeName }}</td>
              <td>{{ row.email || row.companyEmail }}</td>
              <td><div class="pending-registration-meta"><strong>{{ $tr(row.department) }}</strong><small>{{ $tr(roleLabels[row.role || row.requestedRole] || row.role || row.requestedRole) }}</small></div></td>
              <td>{{ row.dealershipName || (row.dealershipId ? `#${row.dealershipId}` : $tr('Head Office')) }}</td>
              <td>{{ row.requestedAt ? new Date(row.requestedAt).toLocaleDateString() : $tr('Pending') }}</td>
              <td><div class="pending-registration-actions"><button class="table-action" @click="approveRegistration(row)">{{ $tr('Approve') }}</button><button class="table-action table-action--danger" @click="rejectRegistration(row)">{{ $tr('Reject') }}</button></div></td>
            </tr>
            <tr v-if="!pendingLoading && !pendingRegistrations.length"><td colspan="7" class="empty-row">{{ $tr('No pending sign-up requests in your approval scope.') }}</td></tr>
          </tbody>
        </table>
      </div>
    </PanelCard>

    <div class="management-grid">
      <PanelCard title="Employee List" :subtitle="loading ? 'Loading employees…' : `${filtered.length} employee(s) in current scope.`">
        <template #actions><input v-model="q" class="mini-search" :placeholder="$tr('Employee ID / name / email')" /></template>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>{{ $tr('Employee ID') }}</th>
                <th>{{ $tr('Name') }}</th>
                <th>{{ $tr('Company Email') }}</th>
                <th>{{ $tr('Dealership') }}</th>
                <th>{{ $tr('Role') }}</th>
                <th>{{ $tr('Status') }}</th>
                <th>{{ $tr('Action') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in filtered" :key="row.id" class="data-table__clickable" @click="selectRow(row)">
                <td class="mono">{{ row.login }}</td>
                <td>{{ row.name }}</td>
                <td>{{ row.email }}</td>
                <td>{{ row.dealership }}</td>
                <td>{{ $tr(roleLabels[row.role]) }}</td>
                <td><StatusBadge :value="row.active ? 'ACTIVE' : 'INACTIVE'" /></td>
                <td><button class="table-action" @click.stop="selectRow(row)">{{ $tr('Edit') }}</button></td>
              </tr>
              <tr v-if="!loading && !filtered.length"><td colspan="7" class="empty-row">{{ $tr('No employees in current scope.') }}</td></tr>
            </tbody>
          </table>
        </div>
      </PanelCard>

      <PanelCard title="Edit Employee" padded>
        <div class="side-form" v-if="selected">
          <label class="field"><span>{{ $tr('Employee ID') }}</span><input v-model="selected.login" disabled /></label>
          <label class="field"><span>{{ $tr('Employee Name') }}</span><input v-model="selected.name" maxlength="120" /></label>
          <label class="field"><span>{{ $tr('Company Email') }}</span><input v-model="selected.email" type="email" maxlength="150" /></label>
          <label class="field">
            <span>{{ $tr('Role') }}</span>
            <select v-model="selected.role">
              <option
                v-for="(label, role) in roleLabels"
                :key="role"
                :value="role"
                :disabled="!isRoleAssignable(role)"
              >{{ $tr(label) }}</option>
            </select>
          </label>
          <label class="field">
            <span>{{ $tr('Dealership') }}</span>
            <select v-model.number="selected.dealershipId" :disabled="session.role === 'SERVICE_MANAGER'">
              <option v-for="dealer in dealershipOptions" :key="dealer.id" :value="dealer.id">{{ dealer.name }}</option>
            </select>
          </label>
          <div class="account-status-card">
            <div>
              <span>{{ $tr('Account Status') }}</span>
              <StatusBadge :value="selected.active ? 'ACTIVE' : 'INACTIVE'" />
            </div>
            <small>{{ $tr('Status changes use the explicit Deactivate / Reactivate action. Historical Case / Interaction links are never deleted.') }}</small>
          </div>
          <button class="button button--primary button--full" :disabled="saving" @click="saveEmployee">
            {{ $tr(saving ? 'Saving…' : 'Save Employee') }}
          </button>

          <div class="danger-zone">
            <div>
              <strong>{{ $tr(selected.active ? 'Deactivate Account' : 'Reactivate Account') }}</strong>
              <p v-if="selected.active">{{ $tr('Blocks sign-in without deleting the employee record. Historical Case / Interaction references remain traceable.') }}</p>
              <p v-else>{{ $tr('Restores sign-in access to the existing employee record.') }}</p>
            </div>
            <button v-if="selected.active" class="button button--danger button--full" :disabled="saving || !canDeactivateSelected" @click="deactivateEmployee">
              {{ $tr('Deactivate Account') }}
            </button>
            <button v-else class="button button--secondary button--full" :disabled="saving || !canReactivateSelected" @click="reactivateEmployee">
              {{ $tr('Reactivate Account') }}
            </button>
            <small v-if="selected && Number(selected.id) === Number(session.employeeDbId) && selected.active">{{ $tr('현재 로그인한 본인 계정은 비활성화할 수 없습니다.') }}</small>
          </div>
        </div>
        <div v-else class="empty-edit-state">{{ $tr('Select an employee to edit.') }}</div>
      </PanelCard>
    </div>

    <div v-if="showAdd" class="modal-backdrop" @click.self="showAdd=false">
      <div class="modal-card">
        <div class="modal-card__header"><div><span>{{ $tr('ACCOUNT ISSUANCE') }}</span><h2>{{ $tr('Add Employee') }}</h2></div><button @click="showAdd=false">×</button></div>
        <div class="side-form">
          <label class="field"><span>{{ $tr('Employee ID (Login ID)') }}</span><input v-model="newEmployee.login" /></label>
          <label class="field"><span>{{ $tr('Employee Name') }}</span><input v-model="newEmployee.name" /></label>
          <label class="field"><span>{{ $tr('Company Email') }}</span><input v-model="newEmployee.email" type="email" /></label>
          <label class="field">
            <span>{{ $tr('Dealership') }}</span>
            <select v-model.number="newEmployee.dealershipId" :disabled="session.role === 'SERVICE_MANAGER'">
              <option v-for="dealer in dealershipOptions" :key="dealer.id" :value="dealer.id">{{ dealer.name }}</option>
            </select>
          </label>
          <label class="field">
            <span>{{ $tr('Role') }}</span>
            <select v-model="newEmployee.role">
              <option
                v-for="(label, role) in roleLabels"
                :key="role"
                :value="role"
                :disabled="!isRoleAssignable(role)"
              >{{ $tr(label) }}</option>
            </select>
          </label>
          <label class="field"><span>{{ $tr('Temporary Password') }}</span><input v-model="newEmployee.password" type="password" /></label>
        </div>
        <div class="modal-card__footer">
          <button class="button button--ghost" @click="showAdd=false">{{ $tr('Cancel') }}</button>
          <button class="button button--primary" :disabled="saving" @click="createEmployee">{{ $tr('Create Employee') }}</button>
        </div>
      </div>
    </div>
  </AppShell>
</template>
