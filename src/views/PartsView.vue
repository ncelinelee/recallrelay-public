<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import PageHeader from '../components/PageHeader.vue'
import PanelCard from '../components/PanelCard.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { partRows } from '../data/mock'
import { session, roleLabels } from '../stores/session'
import { tr } from '../i18n'

const router = useRouter()
const q = ref('')
const status = ref('')
const selected = ref({ ...partRows[0] })
const localRows = ref(partRows.map((r) => ({ ...r })))

const canEdit = computed(() => ['PARTS_SPECIALIST','SERVICE_MANAGER'].includes(session.role))
const accessLabel = computed(() => session.role === 'SERVICE_MANAGER'
  ? tr('Service Manager · Full Dealership Access')
  : `${tr(roleLabels[session.role] || 'Role')} · ${tr(canEdit.value ? 'Edit' : 'View Only')}`)
const feedback = ref('')
const error = ref('')
const filtered = computed(() => localRows.value.filter((row) => {
  const textMatch = !q.value || Object.values(row).some((v) => String(v).toLowerCase().includes(q.value.toLowerCase()))
  const statusMatch = !status.value || row.status === status.value
  return textMatch && statusMatch
}))


function calculateSupplyRisk(row) {
  if (row.status === 'ARRIVED') return 'ALIGNED'
  if (row.needBy === 'ASAP') return 'AT_RISK'
  if (!row.eta || row.eta === 'N/A') return 'AT_RISK'
  const eta = new Date(`${row.eta}T00:00:00`).getTime()
  const need = new Date(`${row.needBy}T00:00:00`).getTime()
  if (!Number.isNaN(eta) && !Number.isNaN(need) && eta > need) return 'AT_RISK'
  return 'WATCH'
}

function savePart() {
  if (!canEdit.value) return
  feedback.value = ''
  error.value = ''
  const needsOrderReference = ['ORDERED','BACKORDERED','IN_TRANSIT','ARRIVED'].includes(selected.value.status)
  if (needsOrderReference && (!selected.value.orderDate || selected.value.orderDate === '—' || !selected.value.orderNo || selected.value.orderNo === '—')) {
    error.value = tr('Order Date and Order No. are required for ORDERED or later statuses.')
    return
  }
  if (selected.value.status === 'IN_TRANSIT' && (!selected.value.eta || selected.value.eta === 'N/A')) {
    error.value = tr('A verifiable ETA is required for IN_TRANSIT status.')
    return
  }
  selected.value.supplyRisk = calculateSupplyRisk(selected.value)
  const target = localRows.value.find((r) => r.id === selected.value.id)
  Object.assign(target, selected.value)
  feedback.value = selected.value.status === 'ARRIVED'
    ? tr('Part updated. ARRIVED can trigger readiness / Next Action recalculation.')
    : tr('Part status / ETA updated.')
}
</script>

<template>
  <AppShell>
    <PageHeader eyebrow="Recall part workflow" title="Part Management" description="Track requested, ordered, backordered, in-transit and arrived recall parts against customer need dates.">
      <span class="access-pill">{{ accessLabel }}</span>
    </PageHeader>

    <PanelCard title="Search / Filter">
      <div class="filter-row">
        <input v-model="q" class="control" :placeholder="$tr('Part no., recall, customer, VIN…')" />
        <select v-model="status" class="control">
          <option value="">{{ $tr('All statuses') }}</option>
          <option value="REQUESTED">{{ $tr('REQUESTED') }}</option><option value="ORDERED">{{ $tr('ORDERED') }}</option><option value="BACKORDERED">{{ $tr('BACKORDERED') }}</option><option value="IN_TRANSIT">{{ $tr('IN_TRANSIT') }}</option><option value="ARRIVED">{{ $tr('ARRIVED') }}</option>
        </select>
        <button class="button button--ghost" @click="q='';status=''">{{ $tr('Clear') }}</button>
      </div>
    </PanelCard>

    <div v-if="feedback" class="employee-feedback employee-feedback--success">{{ feedback }}</div>
    <div v-if="error" class="employee-feedback employee-feedback--error">{{ error }}</div>

    <div class="management-grid">
      <PanelCard title="Parts Queue" :subtitle="`${filtered.length} part record(s)`">
        <div class="table-wrap">
          <table class="data-table">
            <thead><tr><th>{{ $tr('Priority') }}</th><th>{{ $tr('Part No.') }}</th><th>{{ $tr('Recall') }}</th><th>{{ $tr('Customer / VIN') }}</th><th>{{ $tr('Status') }}</th><th>{{ $tr('Need By') }}</th><th>{{ $tr('ETA') }}</th><th>{{ $tr('Risk') }}</th><th>{{ $tr('Action') }}</th></tr></thead>
            <tbody>
              <tr v-for="row in filtered" :key="row.id" class="data-table__clickable" @click="selected={...row}; feedback=''; error=''">
                <td><StatusBadge :value="row.priority" /></td>
                <td class="mono">{{ row.partNo }}</td>
                <td class="mono">{{ row.recall }}</td>
                <td>{{ row.customer }}</td>
                <td><StatusBadge :value="row.status" /></td>
                <td>{{ row.needBy }}</td>
                <td>{{ row.eta }}</td>
                <td><StatusBadge :value="row.supplyRisk" /></td>
                <td><button class="table-action" @click.stop="router.push(`/cases/${row.caseId || 20881}`)">{{ $tr('View Case') }}</button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </PanelCard>

      <PanelCard :title="canEdit ? 'Editable Part Fields' : 'Part Details'" subtitle="ARRIVED triggers readiness / Next Action recalculation." padded>
        <div class="side-form" v-if="selected">
          <div class="mini-decision-summary">
            <div><span>{{ $tr('Customer Need') }}</span><strong>{{ selected.needBy }}</strong></div>
            <div><span>{{ $tr('Supply Risk') }}</span><StatusBadge :value="selected.supplyRisk" /></div>
          </div>
          <label class="field"><span>{{ $tr('Status') }}</span><select v-model="selected.status" :disabled="!canEdit"><option value="REQUESTED">{{ $tr('REQUESTED') }}</option><option value="ORDERED">{{ $tr('ORDERED') }}</option><option value="BACKORDERED">{{ $tr('BACKORDERED') }}</option><option value="IN_TRANSIT">{{ $tr('IN_TRANSIT') }}</option><option value="ARRIVED">{{ $tr('ARRIVED') }}</option></select></label>
          <label class="field"><span>{{ $tr('Order Date') }}</span><input v-model="selected.orderDate" type="date" :disabled="!canEdit" /></label>
          <label class="field"><span>{{ $tr('Order No.') }}</span><input v-model="selected.orderNo" class="mono" :disabled="!canEdit" /></label>
          <label class="field"><span>{{ $tr('ETA') }}</span><input v-model="selected.eta" :disabled="!canEdit" :placeholder="$tr('N/A allowed')" /></label>
          <button class="button button--primary button--full" :disabled="!canEdit" @click="savePart">{{ $tr('Update ETA / Status') }}</button>
          <p v-if="!canEdit" class="field-help">{{ $tr('This role can review part status but cannot change the dealership part workflow.') }}</p>
        </div>
      </PanelCard>
    </div>
  </AppShell>
</template>
