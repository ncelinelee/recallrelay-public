<script setup>
defineProps({
  eyebrow: String,
  title: String,
  description: String,
})
</script>

<template>
  <div class="page-header">
    <div>
      <span v-if="eyebrow" class="page-eyebrow">{{ $tr(eyebrow) }}</span>
      <h1>{{ $tr(title) }}</h1>
      <p v-if="description">{{ $tr(description) }}</p>
    </div>
    <div class="page-header__actions">
      <slot />
    </div>
  </div>
</template>
