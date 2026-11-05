<script setup>
import { computed, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import PageHeader from '../components/PageHeader.vue'
import PanelCard from '../components/PanelCard.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { appointmentRows } from '../data/mock'
import { session, roleLabels } from '../stores/session'
import { tr } from '../i18n'

const selectedCase = ref('RC-20881 · Amy Henderson · WXR-2026-14')
const readiness = ref('NOT_READY')
const date = ref('2026-09-03')
const time = ref('10:00')
const technician = ref('Kevin Adams')
const appointmentStatus = ref('SCHEDULED')
const additional = ref(false)
const serviceNote = ref('')
const advisor = ref('Sarah Lee')
const handoff = ref('REQUIRED')
const hasCurrentAppointment = ref(true)
const message = ref('')

const canEdit = computed(() => ['APPOINTMENT_COORDINATOR','SERVICE_ADVISOR','SERVICE_MANAGER'].includes(session.role))
const accessLabel = computed(() => session.role === 'SERVICE_MANAGER'
  ? tr('Service Manager · Full Dealership Access')
  : `${tr(roleLabels[session.role] || 'Role')} · ${tr(canEdit.value ? 'Edit' : 'View Only')}`)

const selectedCaseSummary = computed(() => {
  if (selectedCase.value.startsWith('RC-20879')) {
    return { need: 'ASAP', eta: 'N/A', partStatus: 'BACKORDERED', risk: 'AT_RISK', reason: 'ASAP request has no confirmed part ETA.' }
  }
  return { need: '2026-09-03', eta: '2026-09-02', partStatus: 'IN_TRANSIT', risk: 'WATCH', reason: 'ETA is before customer need, but physical receipt is not confirmed.' }
})

function saveAppointment() {
  if (!canEdit.value) return
  if (!date.value || !time.value || !technician.value) {
    message.value = tr('Date, time and technician are required.')
    return
  }
  hasCurrentAppointment.value = true
  appointmentStatus.value = 'SCHEDULED'
  message.value = readiness.value === 'NOT_READY'
    ? tr('Appointment saved with NOT_READY warning. Part status must be rechecked before service.')
    : tr('Appointment saved.')
}

function cancelAppointment() {
  if (!canEdit.value || !hasCurrentAppointment.value) return
  appointmentStatus.value = 'CANCELLED'
  message.value = tr('Current appointment cancelled. The same appointment record is retained for history.')
}
</script>

<template>
  <AppShell>
    <PageHeader eyebrow="Recall appointment coordination" title="Appointment Management" description="Manage the single current appointment for a recall case and verify customer need, supply readiness and technician availability.">
      <span class="access-pill">{{ accessLabel }}</span>
    </PageHeader>

    <div v-if="message" class="employee-feedback employee-feedback--success appointment-feedback">{{ message }}</div>

    <div class="decision-strip">
      <div><span>{{ $tr('Customer Need') }}</span><strong>{{ selectedCaseSummary.need }}</strong></div>
      <div><span>{{ $tr('Part Status') }}</span><StatusBadge :value="selectedCaseSummary.partStatus" /></div>
      <div><span>{{ $tr('Current ETA') }}</span><strong>{{ selectedCaseSummary.eta }}</strong></div>
      <div><span>{{ $tr('Supply Alignment') }}</span><StatusBadge :value="selectedCaseSummary.risk" /><small>{{ $tr(selectedCaseSummary.reason) }}</small></div>
    </div>

    <div class="appointment-layout">
      <PanelCard :title="hasCurrentAppointment ? 'Current Appointment' : 'No Current Appointment'" padded>
        <div class="form-grid">
          <label class="field field--wide"><span>{{ $tr('Customer / Recall Case') }}</span><select v-model="selectedCase" :disabled="!canEdit"><option value="RC-20881 · Amy Henderson · WXR-2026-14">{{ $tr('RC-20881 · Amy Henderson · WXR-2026-14') }}</option><option value="RC-20879 · Michael Chen · WRL-2026-03">{{ $tr('RC-20879 · Michael Chen · WRL-2026-03') }}</option></select></label>

          <div class="readiness-box field--wide">
            <div><span>{{ $tr('Part Readiness') }}</span><StatusBadge :value="readiness" /></div>
            <p>{{ $tr('READY = linked Part ≥ 1 and every status is ARRIVED. Otherwise NOT_READY.') }}</p>
            <div v-if="readiness === 'NOT_READY'" class="notice notice--warning">{{ $tr('Part Not Ready — booking is allowed, but the warning remains visible and supply must be rechecked before service commitment.') }}</div>
          </div>

          <label class="field"><span>{{ $tr('Appointment Date') }}</span><input v-model="date" type="date" :disabled="!canEdit" /></label>
          <label class="field"><span>{{ $tr('Appointment Time') }}</span><input v-model="time" type="time" :disabled="!canEdit" /></label>
          <label class="field"><span>{{ $tr('Technician') }}</span><select v-model="technician" :disabled="!canEdit"><option value="Kevin Adams">{{ $tr('Kevin Adams') }}</option><option value="Ravi Patel">{{ $tr('Ravi Patel') }}</option><option value="James Brown">{{ $tr('James Brown') }}</option></select></label>
          <div class="availability-block"><span>{{ $tr('Availability') }}</span><strong>{{ $tr('AVAILABLE') }}</strong><small>{{ $tr('Selected timeslot') }}</small></div>
          <label class="field"><span>{{ $tr('Appointment Status') }}</span><input :value="appointmentStatus" disabled /></label>
        </div>
      </PanelCard>

      <PanelCard title="Additional Service / Handoff" padded>
        <div class="side-form">
          <label class="check-field"><input v-model="additional" type="checkbox" :disabled="!canEdit" /><span><strong>{{ $tr('Additional Service Requested') }}</strong><small>{{ $tr('RecallRelay stores request / note / advisor handoff only.') }}</small></span></label>
          <label class="field"><span>{{ $tr('Service Note') }}</span><textarea v-model="serviceNote" :disabled="!canEdit" rows="5" :placeholder="$tr('Internal note for the appointment')"></textarea></label>
          <label class="field"><span>{{ $tr('Service Advisor') }}</span><select v-model="advisor" :disabled="!canEdit"><option value="Sarah Lee">{{ $tr('Sarah Lee') }}</option><option value="David Kim">{{ $tr('David Kim') }}</option></select></label>
          <label class="field"><span>{{ $tr('Handoff Status') }}</span><select v-model="handoff" :disabled="!canEdit"><option value="REQUIRED">{{ $tr('REQUIRED') }}</option><option value="COMPLETE">{{ $tr('COMPLETE') }}</option><option value="NOT_REQUIRED">{{ $tr('NOT_REQUIRED') }}</option></select></label>
        </div>
      </PanelCard>
    </div>

    <div class="action-bar">
      <span>{{ $tr('One current appointment per Case: first create = POST; change, cancel or reschedule = PATCH the existing record.') }}</span>
      <div>
        <button v-if="hasCurrentAppointment" class="button button--ghost" :disabled="!canEdit || appointmentStatus === 'CANCELLED'" @click="cancelAppointment">{{ $tr('Cancel Appointment') }}</button>
        <button class="button button--primary" :disabled="!canEdit" @click="saveAppointment">{{ $tr(hasCurrentAppointment ? 'Save Changes' : 'Schedule Appointment') }}</button>
      </div>
    </div>

    <PanelCard title="Today's Appointments">
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>{{ $tr('Time') }}</th><th>{{ $tr('Customer') }}</th><th>{{ $tr('Recall') }}</th><th>{{ $tr('Part Readiness') }}</th><th>{{ $tr('Technician') }}</th><th>{{ $tr('Status') }}</th></tr></thead>
          <tbody><tr v-for="row in appointmentRows" :key="row.id"><td>{{ row.time }}</td><td>{{ row.customer }}</td><td class="mono">{{ row.recall }}</td><td><StatusBadge :value="row.readiness" /></td><td>{{ row.technician }}</td><td><StatusBadge :value="row.status" /></td></tr></tbody>
        </table>
      </div>
    </PanelCard>
  </AppShell>
</template>
