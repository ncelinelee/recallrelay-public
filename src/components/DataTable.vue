<script setup>
import StatusBadge from './StatusBadge.vue'

defineProps({
  columns: { type: Array, default: () => [] },
  rows: { type: Array, default: () => [] },
  compact: { type: Boolean, default: false },
  clickable: { type: Boolean, default: true },
})
defineEmits(['row-click'])

const badgeWords = [
  'P1','P2','P3','HIGH','CRITICAL','MEDIUM','LOW','REQUESTED','ORDERED','BACKORDERED','IN_TRANSIT','ARRIVED',
  'READY','NOT_READY','SCHEDULED','CHECKED_IN','IN_PROGRESS','WAITING','NOT_STARTED','COMPLETED','NEW','CUSTOMER_CONTACTED',
  'APPOINTMENT_SCHEDULED','SERVICE_COMPLETED','ACTIVE','ELIGIBLE','AT_RISK','WATCH','ALIGNED'
]

function cellValue(cell) {
  return cell && typeof cell === 'object' && !Array.isArray(cell) ? cell.value : cell
}

function cellSubtext(cell) {
  return cell && typeof cell === 'object' && !Array.isArray(cell) ? cell.subtext : ''
}

function shouldBadge(cell) {
  if (cell && typeof cell === 'object' && !Array.isArray(cell) && typeof cell.badge === 'boolean') return cell.badge
  const value = cellValue(cell)
  return typeof value === 'string' && badgeWords.includes(value)
}
</script>

<template>
  <div class="table-wrap" :class="{ 'table-wrap--compact': compact }">
    <table class="data-table">
      <thead>
        <tr>
          <th v-for="column in columns" :key="column">{{ $tr(column) }}</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(row, rIndex) in rows"
          :key="rIndex"
          :class="{ 'data-table__clickable': clickable }"
          @click="clickable && $emit('row-click', row, rIndex)"
        >
          <td v-for="(cell, cIndex) in row" :key="cIndex" :class="{ 'data-table__stacked': cellSubtext(cell) }">
            <StatusBadge v-if="shouldBadge(cell)" :value="cellValue(cell)" />
            <span v-else>{{ $tr(cellValue(cell)) }}</span>
            <small v-if="cellSubtext(cell)" class="cell-subtext">{{ $tr(cellSubtext(cell)) }}</small>
          </td>
        </tr>
        <tr v-if="!rows.length">
          <td :colspan="columns.length" class="table-empty">{{ $tr('No records found') }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
