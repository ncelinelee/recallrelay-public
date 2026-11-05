<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import PageHeader from '../components/PageHeader.vue'
import PanelCard from '../components/PanelCard.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { searchRows } from '../data/mock'
import { session } from '../stores/session'

const route = useRoute()
const router = useRouter()
const query = ref(route.query.q || '')
const searched = ref(Boolean(route.query.q))

watch(() => route.query.q, (value) => {
  query.value = value || ''
  searched.value = Boolean(value)
})

const filtered = computed(() => {
  if (!searched.value || !query.value.trim()) return searchRows
  const q = query.value.toLowerCase()
  return searchRows.filter((row) => Object.values(row).some((v) => String(v).toLowerCase().includes(q)))
})

function runSearch() {
  searched.value = true
}
</script>

<template>
  <AppShell>
    <PageHeader eyebrow="Universal case lookup" title="Universal Search" description="Search by Customer No., customer name, VIN, recall number or part number.">
      <button
        v-if="['APPOINTMENT_COORDINATOR','SERVICE_ADVISOR','SERVICE_MANAGER'].includes(session.role)"
        class="button button--secondary"
        @click="router.push('/customers/new')"
      >
        {{ $tr('+ Register New Customer') }}
      </button>
    </PageHeader>

    <div class="scope-bar">
      <div><span>{{ $tr('Organization') }}</span><strong>{{ session.organizationName }}</strong></div>
      <div><span>{{ $tr('Dealership') }}</span><strong>{{ session.role === 'HEAD_OFFICE_ADMIN' ? $tr('All dealerships') : session.dealershipName }}</strong></div>
      <div><span>{{ $tr('Data scope') }}</span><strong>{{ $tr(session.role === 'HEAD_OFFICE_ADMIN' ? 'Organization' : 'Assigned dealership') }}</strong></div>
    </div>

    <PanelCard title="Search">
      <div class="search-console">
        <div class="search-console__input">
          <span class="search-type">{{ $tr('ANY IDENTIFIER') }}</span>
          <input v-model="query" :placeholder="$tr('e.g. JF2BTABC1M000001 or WXR-2026-14')" @keyup.enter="runSearch" />
        </div>
        <button class="button button--primary" @click="runSearch">{{ $tr('Search') }}</button>
        <button class="button button--ghost" @click="query=''; searched=false">{{ $tr('Clear') }}</button>
      </div>
    </PanelCard>

    <PanelCard title="Search Results" :subtitle="`${filtered.length} result(s) within current access scope.`">
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>{{ $tr('Dealership') }}</th><th>{{ $tr('Customer') }}</th><th>{{ $tr('Vehicle / VIN') }}</th><th>{{ $tr('Recall') }}</th><th>{{ $tr('Priority') }}</th><th>{{ $tr('Case Status') }}</th><th>{{ $tr('Part Status') }}</th><th>{{ $tr('Supply Risk') }}</th><th>{{ $tr('Action') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in filtered" :key="row.id" class="data-table__clickable" @click="router.push(`/cases/${row.id}`)">
              <td>{{ row.dealership }}</td>
              <td><strong>{{ row.customer }}</strong></td>
              <td class="mono">{{ row.vin }}</td>
              <td class="mono">{{ row.recall }}</td>
              <td><StatusBadge :value="row.priority" /></td>
              <td><StatusBadge :value="row.caseStatus" /></td>
              <td><StatusBadge :value="row.partStatus" /></td>
              <td class="risk-cell"><StatusBadge :value="row.supplyRisk" /><small>{{ $tr(row.supplyRiskReason) }}</small></td>
              <td><button class="table-action">{{ $tr('View Case') }}</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </PanelCard>

    <div class="info-strip">
      <strong>{{ $tr('Search scope rule') }}</strong>
      <span>{{ $tr('Head Office Admin: Organization-wide · Dealership roles: Assigned dealership only · Cross-organization results are never returned.') }}</span>
    </div>
  </AppShell>
</template>
