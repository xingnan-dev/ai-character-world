<template>
  <RouterView v-slot="{ Component, route }">
    <AppShell v-if="route.meta.appShell"><component :is="Component" /></AppShell>
    <component :is="Component" v-else />
  </RouterView>
</template>

<script setup>
import AppShell from './components/layout/AppShell.vue'
</script>

<style lang="scss">
#app {
  width: 100%;
  min-height: 100vh;
}
</style>
