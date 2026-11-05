<script setup>
defineProps({
  name: { type: String, default: 'dashboard' },
  size: { type: Number, default: 18 },
})
</script>

<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <template v-if="name === 'dashboard'">
      <rect x="3" y="3" width="7" height="7" rx="1" />
      <rect x="14" y="3" width="7" height="7" rx="1" />
      <rect x="3" y="14" width="7" height="7" rx="1" />
      <rect x="14" y="14" width="7" height="7" rx="1" />
    </template>
    <template v-else-if="name === 'search'">
      <circle cx="11" cy="11" r="6.5" />
      <path d="M16 16l5 5" />
    </template>
    <template v-else-if="name === 'calendar'">
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M7 3v4M17 3v4M3 10h18" />
    </template>
    <template v-else-if="name === 'parts'">
      <path d="M7 7h10l3 5-3 5H7l-3-5 3-5z" />
      <circle cx="12" cy="12" r="2.5" />
    </template>
    <template v-else-if="name === 'campaign'">
      <path d="M4 13V7l12-3v12L4 13z" />
      <path d="M16 8h3a2 2 0 010 4h-3M7 14v5" />
    </template>
    <template v-else-if="name === 'employees'">
      <circle cx="9" cy="8" r="3" />
      <path d="M3 20c0-4 2.5-6 6-6s6 2 6 6" />
      <path d="M16 8h5M18.5 5.5v5" />
    </template>
    <template v-else-if="name === 'case'">
      <rect x="4" y="3" width="16" height="18" rx="2" />
      <path d="M8 8h8M8 12h8M8 16h5" />
    </template>
    <template v-else-if="name === 'logout'">
      <path d="M10 4H5v16h5M14 8l4 4-4 4M18 12H9" />
    </template>
    <template v-else>
      <circle cx="12" cy="12" r="8" />
    </template>
  </svg>
</template>

<style scoped>
svg { stroke: currentColor; stroke-width: 1.65; stroke-linecap: round; stroke-linejoin: round; }
</style>
