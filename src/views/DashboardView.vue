<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import PageHeader from '../components/PageHeader.vue'
import StatCard from '../components/StatCard.vue'
import PanelCard from '../components/PanelCard.vue'
import DataTable from '../components/DataTable.vue'
import { dashboardProfiles } from '../data/mock'
import { session } from '../stores/session'

const router = useRouter()
const profile = computed(() => dashboardProfiles[session.role] || dashboardProfiles.SERVICE_MANAGER)

function openPrimaryRow(row) {
  const text = Array.isArray(row) ? row.join(' ') : String(row || '')
  const explicit = text.match(/RC-(\d{5})/)
  if (explicit) return router.push(`/cases/${explicit[1]}`)

  const customerMap = {
    'Amy Henderson': 20881,
    'Michael Chen': 20879,
    'Emma Wilson': 20866,
    'Lucas Moore': 20852,
    'Sophia Kim': 20845,
  }
  const match = Object.entries(customerMap).find(([name]) => text.includes(name))
  router.push(`/cases/${match ? match[1] : 20881}`)
}
</script>

<template>
  <AppShell>
    <PageHeader :eyebrow="profile.eyebrow" :title="profile.title" description="Current operational workload and recall exceptions requiring attention.">
      <button class="button button--secondary" @click="router.push('/search')">{{ $tr('Open Search') }}</button>
      <button v-if="session.role === 'SERVICE_MANAGER'" class="button button--primary" @click="router.push({ path: '/search', query: { q: 'AT_RISK' } })">{{ $tr('View All Risks') }}</button>
      <button v-else-if="session.role === 'HEAD_OFFICE_ADMIN'" class="button button--primary" @click="router.push('/campaigns')">{{ $tr('Campaigns') }}</button>
    </PageHeader>

    <div class="stats-grid">
      <StatCard
        v-for="item in profile.stats"
        :key="item[0]"
        :label="item[0]"
        :value="item[1]"
        :meta="item[2]"
      />
    </div>

    <div class="dashboard-grid">
      <PanelCard :title="profile.primaryTitle" subtitle="Sorted by operational priority and action urgency.">
        <template #actions>
          <button class="panel-link" @click="router.push('/search')">{{ $tr('View all') }}</button>
        </template>
        <DataTable :columns="profile.primaryColumns" :rows="profile.primaryRows" @row-click="openPrimaryRow" />
      </PanelCard>

      <div class="dashboard-side">
        <PanelCard
          v-for="section in profile.secondary"
          :key="section.title"
          :title="section.title"
        >
          <DataTable :columns="section.columns" :rows="section.rows" compact @row-click="openPrimaryRow" />
        </PanelCard>
      </div>
    </div>
  </AppShell>
</template>
