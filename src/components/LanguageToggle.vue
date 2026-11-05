<script setup>
import { locale, setLocale } from '../i18n'
</script>

<template>
  <div class="language-toggle" role="group" aria-label="Language">
    <button type="button" :class="{ active: locale === 'en' }" @click="setLocale('en')">EN</button>
    <span></span>
    <button type="button" :class="{ active: locale === 'ko' }" @click="setLocale('ko')">한국어</button>
  </div>
</template>
