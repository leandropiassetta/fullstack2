<template>
  <v-app class="public-layout">
    <v-main class="d-flex align-center justify-center">
      <v-container class="py-8">
        <RouterView />
      </v-container>
    </v-main>
  </v-app>
</template>

<style scoped>
.public-layout {
  background:
    radial-gradient(circle at top left, rgb(194 148 74 / 0.2), transparent 28%),
    linear-gradient(135deg, #f4efe6, #fffaf4 55%, #f1e5d0);
}
</style>
