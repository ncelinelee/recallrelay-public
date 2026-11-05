<script setup>
defineProps({
  title: String,
  subtitle: String,
  padded: { type: Boolean, default: false },
})
</script>

<template>
  <section class="panel-card">
    <header class="panel-card__header">
      <div>
        <h2>{{ $tr(title) }}</h2>
        <p v-if="subtitle">{{ $tr(subtitle) }}</p>
      </div>
      <slot name="actions" />
    </header>
    <div :class="{ 'panel-card__body--padded': padded }">
      <slot />
    </div>
  </section>
</template>
