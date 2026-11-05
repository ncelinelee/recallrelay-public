<script setup>
import { computed, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import PageHeader from '../components/PageHeader.vue'
import PanelCard from '../components/PanelCard.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { campaignRows } from '../data/mock'
import { session } from '../stores/session'
import { tr } from '../i18n'

const rows = ref(campaignRows.map((row) => ({ ...row })))
const selected = ref({ ...rows.value[0] })
const search = ref('')
const status = ref('')
const dealershipFilter = ref('ALL')
const targetVin = ref('JF2BTABC1M000001')
const feedback = ref('')
const error = ref('')
const showCreate = ref(false)
const newCampaign = ref({ recallNo: '', type: 'Safety Recall', status: 'ACTIVE', severity: 'MEDIUM', safetyImpact: '' })
const targets = ref([
  { id: 1, vin: 'JF2BTABC1M000001', match: 'MATCHED', caseLink: 'RC-20881' },
  { id: 2, vin: '4S4BTABC0N0004270', match: 'MATCHED', caseLink: 'RC-20866' },
  { id: 3, vin: 'JF2SKABC8M0003408', match: 'MATCHED', caseLink: 'RC-20852' },
])

const headOffice = computed(() => session.role === 'HEAD_OFFICE_ADMIN')
const serviceManager = computed(() => session.role === 'SERVICE_MANAGER')
const canEditExisting = computed(() => headOffice.value || serviceManager.value)
const canCreateCampaign = computed(() => headOffice.value)
const filtered = computed(() => rows.value.filter((c) => (!search.value || c.recallNo.toLowerCase().includes(search.value.toLowerCase())) && (!status.value || c.status === status.value)))
const openCasesLabel = computed(() => tr(serviceManager.value ? 'Open Cases (This Dealership)' : 'Open Cases'))
const scopeLabel = computed(() => serviceManager.value ? `${tr('Assigned dealership')}: ${session.dealershipName}` : tr('Organization-wide campaign scope'))


function createCampaign() {
  if (!canCreateCampaign.value) return
  const recallNo = newCampaign.value.recallNo.trim().toUpperCase()
  if (!recallNo) {
    error.value = tr('Recall No. is required.')
    return
  }
  if (rows.value.some((row) => row.recallNo === recallNo)) {
    error.value = tr('Recall No. already exists.')
    return
  }
  const created = {
    id: Date.now(),
    recallNo,
    status: newCampaign.value.status,
    severity: newCampaign.value.severity,
    type: newCampaign.value.type,
    date: new Date().toISOString().slice(0, 10),
    open: 0,
    dealershipOpen: 0,
    priority: 'P3',
    safetyImpact: newCampaign.value.safetyImpact.trim(),
  }
  rows.value.unshift(created)
  selected.value = { ...created }
  showCreate.value = false
  newCampaign.value = { recallNo: '', type: 'Safety Recall', status: 'ACTIVE', severity: 'MEDIUM', safetyImpact: '' }
  error.value = ''
  feedback.value = tr('New recall campaign created by Head Office Admin.')
}

function selectCampaign(row) {
  selected.value = { ...row }
  feedback.value = ''
  error.value = ''
}

function saveCampaign() {
  if (!canEditExisting.value) return
  const index = rows.value.findIndex((row) => row.id === selected.value.id)
  if (index >= 0) rows.value[index] = { ...selected.value }
  feedback.value = serviceManager.value
    ? tr('Existing campaign settings updated for the current management scope.')
    : tr('Campaign updated.')
}

function addTarget() {
  if (!canEditExisting.value) return
  const vin = targetVin.value.trim().toUpperCase()
  error.value = ''
  feedback.value = ''
  if (vin.length !== 17) {
    error.value = tr('Target VIN must be exactly 17 characters.')
    return
  }
  if (targets.value.some((item) => item.vin === vin)) {
    error.value = tr('This VIN is already registered in the campaign.')
    return
  }
  targets.value.push({ id: Date.now(), vin, match: 'PENDING', caseLink: 'Not linked' })
  targetVin.value = ''
  feedback.value = tr('Target VIN added to the existing campaign.')
}

function removeTarget(target) {
  if (!canEditExisting.value) return
  targets.value = targets.value.filter((item) => item.id !== target.id)
  feedback.value = tr('Target VIN removed from the existing campaign.')
}
</script>

<template>
  <AppShell>
    <PageHeader eyebrow="Organization recall campaigns" title="Recall Campaign Management" description="Head Office creates campaigns. Service Managers have full management access to existing campaigns within their assigned dealership scope.">
      <span class="access-pill">{{ serviceManager ? $tr('Service Manager · Full Existing-Campaign Access') : scopeLabel }}</span>
      <button v-if="canCreateCampaign" class="button button--primary" @click="showCreate=true">{{ $tr('+ Add Campaign') }}</button>
    </PageHeader>

    <div v-if="feedback" class="employee-feedback employee-feedback--success">{{ feedback }}</div>
    <div v-if="error" class="employee-feedback employee-feedback--error">{{ error }}</div>

    <PanelCard title="Campaign Search">
      <div class="filter-row">
        <input v-model="search" class="control" :placeholder="$tr('Recall No.')" />
        <select v-model="status" class="control"><option value="">{{ $tr('All statuses') }}</option><option value="ACTIVE">{{ $tr('ACTIVE') }}</option><option value="CLOSED">{{ $tr('CLOSED') }}</option></select>
        <select v-if="headOffice" v-model="dealershipFilter" class="control"><option value="ALL">{{ $tr('All Dealerships') }}</option><option value="12">{{ $tr('Downtown Subaru') }}</option></select>
        <div v-else class="scope-control"><span>{{ $tr('Scope') }}</span><strong>{{ session.dealershipName }}</strong></div>
        <button class="button button--ghost" @click="search='';status='';dealershipFilter='ALL'">{{ $tr('Clear') }}</button>
      </div>
    </PanelCard>

    <div class="campaign-layout">
      <PanelCard title="Campaign List" :subtitle="`${filtered.length} campaign(s) · ${scopeLabel}`">
        <div class="table-wrap">
          <table class="data-table">
            <thead><tr><th>{{ $tr('Recall No.') }}</th><th>{{ $tr('Status') }}</th><th>{{ $tr('Severity') }}</th><th>{{ $tr('Type') }}</th><th>{{ $tr('Campaign Date') }}</th><th>{{ openCasesLabel }}</th></tr></thead>
            <tbody>
              <tr v-for="row in filtered" :key="row.id" class="data-table__clickable" @click="selectCampaign(row)">
                <td class="mono"><strong>{{ row.recallNo }}</strong></td><td><StatusBadge :value="row.status" /></td><td><StatusBadge :value="row.severity" /></td><td>{{ $tr(row.type) }}</td><td>{{ row.date }}</td><td>{{ serviceManager ? row.dealershipOpen : row.open }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </PanelCard>

      <PanelCard title="Existing Campaign Detail" padded>
        <div class="side-form">
          <label class="field"><span>{{ $tr('Recall No.') }} <small>{{ $tr('immutable after creation') }}</small></span><input :value="selected.recallNo" disabled class="mono" /></label>
          <label class="field"><span>{{ $tr('Campaign Type') }}</span><input v-model="selected.type" :disabled="!canEditExisting" /></label>
          <label class="field"><span>{{ $tr('Status') }}</span><select v-model="selected.status" :disabled="!canEditExisting"><option value="ACTIVE">{{ $tr('ACTIVE') }}</option><option value="CLOSED">{{ $tr('CLOSED') }}</option></select></label>
          <label class="field"><span>{{ $tr('Severity') }}</span><select v-model="selected.severity" :disabled="!canEditExisting"><option value="CRITICAL">{{ $tr('CRITICAL') }}</option><option value="HIGH">{{ $tr('HIGH') }}</option><option value="MEDIUM">{{ $tr('MEDIUM') }}</option><option value="LOW">{{ $tr('LOW') }}</option></select></label>
          <label class="field"><span>{{ $tr('Safety Impact') }}</span><textarea v-model="selected.safetyImpact" :disabled="!canEditExisting" rows="3"></textarea></label>

          <div class="divider"></div>

          <label class="field"><span>{{ $tr('Assigned Dealership Priority') }}</span><select v-model="selected.priority" :disabled="!canEditExisting"><option value="P1">{{ $tr('P1') }}</option><option value="P2">{{ $tr('P2') }}</option><option value="P3">{{ $tr('P3') }}</option></select></label>
          <small class="field-help">{{ $tr('Severity describes recall impact. P1 / P2 / P3 is the operational priority used by the dealership.') }}</small>
          <button class="button button--primary button--full" :disabled="!canEditExisting" @click="saveCampaign">{{ $tr('Save Existing Campaign') }}</button>
          <small v-if="serviceManager" class="field-help">{{ $tr('Creating a new recall campaign remains Head Office Admin only.') }}</small>
        </div>
      </PanelCard>
    </div>

    <PanelCard title="Recall Target VIN" subtitle="Existing campaign targets can be managed by Head Office Admin or Service Manager; new campaign creation remains Head Office only.">
      <template #actions>
        <div v-if="canEditExisting" class="target-action">
          <input v-model="targetVin" class="control mono" maxlength="17" :placeholder="$tr('17-character VIN')" />
          <button class="button button--secondary button--small" @click="addTarget">{{ $tr('Add Target') }}</button>
        </div>
      </template>
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>{{ $tr('VIN') }}</th><th>{{ $tr('Vehicle Match') }}</th><th>{{ $tr('Case Link Status') }}</th><th v-if="canEditExisting">{{ $tr('Action') }}</th></tr></thead>
          <tbody>
            <tr v-for="target in targets" :key="target.id">
              <td class="mono">{{ target.vin }}</td><td><StatusBadge :value="target.match" /></td><td>{{ target.caseLink }}</td><td v-if="canEditExisting"><button class="table-action" @click="removeTarget(target)">{{ $tr('Remove') }}</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </PanelCard>


    <div v-if="showCreate" class="modal-backdrop" @click.self="showCreate=false">
      <div class="modal-card">
        <div class="modal-card__header">
          <div><span>{{ $tr('HEAD OFFICE ONLY') }}</span><h2>{{ $tr('Create Recall Campaign') }}</h2></div>
          <button @click="showCreate=false">×</button>
        </div>
        <div class="side-form">
          <label class="field"><span>{{ $tr('Recall No.') }}</span><input v-model="newCampaign.recallNo" class="mono" :placeholder="$tr('WXR-2026-15')" /></label>
          <label class="field"><span>{{ $tr('Campaign Type') }}</span><input v-model="newCampaign.type" /></label>
          <label class="field"><span>{{ $tr('Status') }}</span><select v-model="newCampaign.status"><option value="ACTIVE">{{ $tr('ACTIVE') }}</option><option value="CLOSED">{{ $tr('CLOSED') }}</option></select></label>
          <label class="field"><span>{{ $tr('Severity') }}</span><select v-model="newCampaign.severity"><option value="CRITICAL">{{ $tr('CRITICAL') }}</option><option value="HIGH">{{ $tr('HIGH') }}</option><option value="MEDIUM">{{ $tr('MEDIUM') }}</option><option value="LOW">{{ $tr('LOW') }}</option></select></label>
          <label class="field"><span>{{ $tr('Safety Impact') }}</span><textarea v-model="newCampaign.safetyImpact" rows="3"></textarea></label>
        </div>
        <div class="modal-card__footer">
          <button class="button button--ghost" @click="showCreate=false">{{ $tr('Cancel') }}</button>
          <button class="button button--primary" @click="createCampaign">{{ $tr('Create Campaign') }}</button>
        </div>
      </div>
    </div>
  </AppShell>
</template>
