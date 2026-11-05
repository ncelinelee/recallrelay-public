import { tr } from '../i18n'
import { reactive } from 'vue'

const STORAGE_KEY = 'recallrelay-pending-registrations'
const APPROVED_KEY = 'recallrelay-approved-self-registrations'

const demoSeed = [
  {
    id: 9001,
    organizationCode: 'OHC-001-001',
    employeeId: 'rchoi',
    name: 'Rachel Choi',
    email: 'rachel.choi@northstar.example',
    phone: '(555) 414-2208',
    dealershipId: 12,
    dealershipNo: '12',
    dealershipName: 'Downtown Subaru',
    department: 'SERVICE',
    role: 'SERVICE_ADVISOR',
    status: 'PENDING_MANAGER_APPROVAL',
    requestedAt: '2026-08-22T11:20:00Z',
  },
  {
    id: 9002,
    organizationCode: 'OHC-001-001',
    employeeId: 'dnguyen',
    name: 'Daniel Nguyen',
    email: 'daniel.nguyen@northstar.example',
    phone: '(555) 414-2211',
    dealershipId: 12,
    dealershipNo: '12',
    dealershipName: 'Downtown Subaru',
    department: 'PARTS',
    role: 'PARTS_SPECIALIST',
    status: 'PENDING_MANAGER_APPROVAL',
    requestedAt: '2026-08-22T12:10:00Z',
  },
]

function load(key, fallback) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}

export const registrationState = reactive({
  pending: load(STORAGE_KEY, demoSeed),
  approved: load(APPROVED_KEY, []),
})

function persist() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(registrationState.pending))
  localStorage.setItem(APPROVED_KEY, JSON.stringify(registrationState.approved))
}

export function addPendingRegistration(payload) {
  const exists = registrationState.pending.some((row) =>
    row.organizationCode === payload.organizationCode &&
    (row.employeeId.toLowerCase() === payload.employeeId.toLowerCase() || row.email.toLowerCase() === payload.email.toLowerCase())
  )
  if (exists) throw new Error(tr('An Employee ID or Company Email is already pending approval.'))

  const row = {
    ...payload,
    id: Date.now(),
    status: 'PENDING_MANAGER_APPROVAL',
    requestedAt: new Date().toISOString(),
  }
  registrationState.pending.push(row)
  persist()
  return row
}

export function approvePendingRegistration(id, approvedBy = 'manager') {
  const index = registrationState.pending.findIndex((row) => Number(row.id) === Number(id))
  if (index < 0) return null
  const [row] = registrationState.pending.splice(index, 1)
  const approved = { ...row, status: 'APPROVED', approvedAt: new Date().toISOString(), approvedBy }
  registrationState.approved.push(approved)
  persist()
  return approved
}

export function rejectPendingRegistration(id, rejectedBy = 'manager') {
  const index = registrationState.pending.findIndex((row) => Number(row.id) === Number(id))
  if (index < 0) return null
  const [row] = registrationState.pending.splice(index, 1)
  persist()
  return { ...row, status: 'REJECTED', rejectedAt: new Date().toISOString(), rejectedBy }
}

export function addApprovedManager(payload) {
  const row = {
    ...payload,
    id: Date.now(),
    status: 'APPROVED',
    approvedAt: new Date().toISOString(),
    approvedBy: 'COMPANY_ONE_TIME_CODE',
  }
  registrationState.approved.push(row)
  persist()
  return row
}
