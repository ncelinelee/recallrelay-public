import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('recallrelay-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export const recallRelayApi = {
  login: (payload) => api.post('/auth/login', payload),
  changePassword: (payload) => api.patch('/auth/password', payload),
  requestPasswordReset: (payload) => api.post('/auth/password-reset/request', payload),
  confirmPasswordReset: (payload) => api.post('/auth/password-reset/confirm', payload),
  startEmployeeRegistration: (payload) => api.post('/auth/registrations/employee/start', payload),
  verifyEmployeeRegistration: (payload) => api.post('/auth/registrations/employee/verify', payload),
  registerManager: (payload) => api.post('/auth/registrations/manager', payload),
  getPendingRegistrations: () => api.get('/registrations/pending'),
  approveRegistration: (registrationId) => api.post(`/registrations/${registrationId}/approve`),
  rejectRegistration: (registrationId) => api.post(`/registrations/${registrationId}/reject`),

  getDashboard: (params) => api.get('/dashboard', { params }),
  search: (q) => api.get('/search', { params: { q } }),
  getDealerships: (params) => api.get('/dealerships', { params }),

  getEmployees: (params) => api.get('/employees', { params }),
  createEmployee: (payload) => api.post('/employees', payload),
  updateEmployee: (employeeId, payload) => api.patch(`/employees/${employeeId}`, payload),

  createCustomer: (payload) => api.post('/customers', payload),

  getCampaigns: (params) => api.get('/campaigns', { params }),
  createCampaign: (payload) => api.post('/campaigns', payload),
  getCampaign: (campaignId) => api.get(`/campaigns/${campaignId}`),
  updateCampaign: (campaignId, payload) => api.patch(`/campaigns/${campaignId}`, payload),
  updateCampaignPriority: (campaignId, payload) => api.patch(`/campaigns/${campaignId}/priority`, payload),
  getCampaignTargets: (campaignId) => api.get(`/campaigns/${campaignId}/targets`),
  addCampaignTarget: (campaignId, payload) => api.post(`/campaigns/${campaignId}/targets`, payload),
  deleteCampaignTarget: (campaignId, targetId) => api.delete(`/campaigns/${campaignId}/targets/${targetId}`),

  getCases: (params) => api.get('/cases', { params }),
  getCase: (caseId) => api.get(`/cases/${caseId}`),
  updateCase: (caseId, payload) => api.patch(`/cases/${caseId}`, payload),
  addCasePart: (caseId, payload) => api.post(`/cases/${caseId}/parts`, payload),

  getParts: (params) => api.get('/parts', { params }),
  updatePart: (partId, payload) => api.patch(`/parts/${partId}`, payload),

  getAppointments: (params) => api.get('/appointments', { params }),
  getAvailability: (params) => api.get('/appointments/availability', { params }),
  createAppointment: (caseId, payload) => api.post(`/cases/${caseId}/appointments`, payload),
  getAppointment: (appointmentId) => api.get(`/appointments/${appointmentId}`),
  updateAppointment: (appointmentId, payload) => api.patch(`/appointments/${appointmentId}`, payload),

  getActivities: (caseId) => api.get(`/cases/${caseId}/activities`),
  createActivity: (caseId, payload) => api.post(`/cases/${caseId}/activities`, payload),

  // AI — Promise & Supply Intelligence
  promiseIntegrity: (caseId) =>
    api.post(`/ai/cases/${caseId}/promise-integrity`, null, { timeout: 60000 }),

  overpromiseCheck: (caseId, payload) =>
    api.post(`/ai/cases/${caseId}/overpromise`, payload, { timeout: 60000 }),

  changeImpact: (caseId, payload) =>
    api.post(`/ai/cases/${caseId}/change-impact`, payload, { timeout: 60000 }),

  nextBestQuestion: (caseId) =>
    api.post(`/ai/cases/${caseId}/next-best-question`, null, { timeout: 60000 }),

  checkContradictions: (caseId) =>
    api.post(`/ai/cases/${caseId}/contradictions`, null, { timeout: 60000 }),
}

export default api
