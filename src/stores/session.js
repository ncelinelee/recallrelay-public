import { reactive } from 'vue'

const savedRole = localStorage.getItem('recallrelay-role') || 'SERVICE_MANAGER'
const savedAuth = localStorage.getItem('recallrelay-auth') === 'true'

export const roleLabels = {
  HEAD_OFFICE_ADMIN: 'Head Office Admin',
  HEAD_OFFICE_MANAGER: 'Head Office Manager',
  HEAD_OFFICE_STAFF: 'Head Office Staff',
  CALL_CENTER_AGENT: 'Call Center Agent',
  SERVICE_MANAGER: 'Service Manager',
  APPOINTMENT_COORDINATOR: 'Appointment Coordinator',
  PARTS_SPECIALIST: 'Parts Specialist',
  SERVICE_ADVISOR: 'Service Advisor',
  TECHNICIAN: 'Technician',
}

export const session = reactive({
  isAuthenticated: savedAuth,
  role: savedRole,
  employeeDbId: Number(localStorage.getItem('recallrelay-employee-db-id') || 0),
  employeeName: localStorage.getItem('recallrelay-employee-name') || 'Celine Lee',
  employeeId: localStorage.getItem('recallrelay-employee-id') || 'jlee',
  organizationCode: localStorage.getItem('recallrelay-org-code') || 'OHC-001-001',
  organizationName: localStorage.getItem('recallrelay-org-name') || 'Northstar Automotive Group',
  dealershipId: Number(localStorage.getItem('recallrelay-dealership-id') || 12),
  dealershipName: localStorage.getItem('recallrelay-dealership-name') || 'Downtown Subaru',
})

export function loginAs(role, context = {}) {
  session.isAuthenticated = true
  session.role = role
  if (context.employeeId) session.employeeId = context.employeeId
  if (context.organizationCode) session.organizationCode = context.organizationCode

  const demoUsers = {
    jlee: { id: 105, name: 'Celine Lee' },
    hadmin: { id: 90, name: 'Helen Admin' },
    homgr: { id: 91, name: 'Morgan Hill' },
    hostaff: { id: 92, name: 'Jamie Park' },
    call01: { id: 93, name: 'Taylor Kim' },
    mpark: { id: 101, name: 'Mina Park' },
    jporter: { id: 102, name: 'Jordan Porter' },
    slee: { id: 103, name: 'Sarah Lee' },
    kadams: { id: 104, name: 'Kevin Adams' },
  }
  const demoUser = demoUsers[session.employeeId]
  session.employeeName = demoUser?.name || session.employeeName || 'Current User'
  session.employeeDbId = demoUser?.id || session.employeeDbId

  localStorage.setItem('recallrelay-auth', 'true')
  localStorage.setItem('recallrelay-role', role)
  localStorage.setItem('recallrelay-employee-db-id', String(session.employeeDbId || ''))
  localStorage.setItem('recallrelay-employee-name', session.employeeName)
  localStorage.setItem('recallrelay-employee-id', session.employeeId)
  localStorage.setItem('recallrelay-org-code', session.organizationCode)
}

export function loginFromApi(loginResponse, context = {}) {
  const employee = loginResponse?.employee || {}
  session.isAuthenticated = true
  session.role = employee.role || 'SERVICE_MANAGER'
  session.employeeDbId = Number(employee.id || 0)
  session.employeeName = employee.employeeName || session.employeeName
  session.employeeId = employee.employeeLoginId || session.employeeId
  session.organizationCode = context.organizationCode || session.organizationCode
  session.dealershipId = employee.dealershipId ?? 0
  session.dealershipName = session.dealershipId ? (employee.dealershipName || session.dealershipName || 'Assigned Dealership') : 'Head Office'

  localStorage.setItem('recallrelay-auth', 'true')
  localStorage.setItem('recallrelay-role', session.role)
  localStorage.setItem('recallrelay-employee-db-id', String(session.employeeDbId || ''))
  localStorage.setItem('recallrelay-employee-name', session.employeeName)
  localStorage.setItem('recallrelay-employee-id', session.employeeId)
  localStorage.setItem('recallrelay-org-code', session.organizationCode)
  localStorage.setItem('recallrelay-dealership-id', String(session.dealershipId || ''))
  if (loginResponse?.accessToken) localStorage.setItem('recallrelay-token', loginResponse.accessToken)
}

export function switchRole(role) {
  session.role = role
  if (['HEAD_OFFICE_ADMIN', 'HEAD_OFFICE_MANAGER', 'HEAD_OFFICE_STAFF', 'CALL_CENTER_AGENT'].includes(role)) {
    session.dealershipId = 0
    session.dealershipName = 'Head Office'
  } else if (!session.dealershipId) {
    session.dealershipId = 12
    session.dealershipName = 'Downtown Subaru'
  }
  localStorage.setItem('recallrelay-role', role)
}

export function logout() {
  session.isAuthenticated = false
  localStorage.removeItem('recallrelay-auth')
  localStorage.removeItem('recallrelay-token')
}
