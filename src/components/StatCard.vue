<script setup>
defineProps({
  label: String,
  value: [String, Number],
  meta: String,
})
</script>

<template>
  <button class="stat-card" type="button">
    <span class="stat-card__bar"></span>
    <span class="stat-card__label">{{ $tr(label) }}</span>
    <strong class="stat-card__value">{{ value }}</strong>
    <span class="stat-card__meta">{{ $tr(meta) }}</span>
  </button>
</template>
