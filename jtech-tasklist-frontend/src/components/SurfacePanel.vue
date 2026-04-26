<script setup lang="ts">
defineProps<{
  label: string
  title: string
  description: string
}>()
</script>

<template>
  <v-sheet rounded="xl" color="surface" class="panel pa-8">
    <p v-if="label" class="section-label">{{ label }}</p>
    <h1 class="text-h4 font-weight-bold text-high-emphasis">{{ title }}</h1>
    <p class="mt-3 text-medium-emphasis">{{ description }}</p>
    <slot />
  </v-sheet>
</template>

<style scoped>
.panel {
  border: 1px solid rgb(84 74 53 / 0.08);
  border-radius: 28px !important;
  background:
    linear-gradient(180deg, rgb(255 255 255 / 0.94), rgb(255 255 255 / 0.9)) !important;
  box-shadow:
    0 20px 50px rgb(13 43 77 / 0.05),
    0 2px 0 rgb(255 255 255 / 0.74) inset;
}

.section-label {
  color: rgb(40 83 107 / 0.56);
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
</style>
