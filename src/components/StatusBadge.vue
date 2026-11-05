<script setup>
import { computed } from 'vue'

const props = defineProps({ value: { type: [String, Boolean], default: '' } })

const label = computed(() => {
  if (typeof props.value === 'boolean') return props.value ? 'YES' : 'NO'
  return String(props.value || '—').replaceAll('_', ' ')
})

const tone = computed(() => {
  const value = String(props.value).toUpperCase()
  if (['P1', 'HIGH', 'CRITICAL', 'BACKORDERED', 'ETA PASSED', 'NOT_READY', 'WAITING', 'CANCELLED', 'AT_RISK'].includes(value)) return 'critical'
  if (['P2', 'ORDERED', 'IN_TRANSIT', 'CHECKED_IN', 'IN_PROGRESS', 'APPOINTMENT_SCHEDULED', 'CUSTOMER_CONTACTED'].includes(value)) return 'info'
  if (['WATCH'].includes(value)) return 'warning'
  if (['P3', 'LOW', 'REQUESTED', 'NEW', 'SCHEDULED', 'NOT_STARTED'].includes(value)) return 'neutral'
  if (['READY', 'ARRIVED', 'COMPLETED', 'SERVICE_COMPLETED', 'ACTIVE', 'ELIGIBLE', 'TRUE', 'ALIGNED'].includes(value)) return 'success'
  return 'neutral'
})
</script>

<template>
  <span class="status-badge" :class="`status-badge--${tone}`">{{ $tr(label) }}</span>
</template>
