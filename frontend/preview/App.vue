<template>
  <div class="dream-app" :class="heroStudyClass">
    <PreviewTopNav />
    <RouterView />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PreviewTopNav from './components/PreviewTopNav.vue'

const route = useRoute()

// Hero study variants keep the rest of the page identical. Only the shared
// grain is reduced while a study is active so the hero carries the texture.
const heroStudyClass = computed(() => {
  if (route.path !== '/home') return null
  if (route.query.hero === 'a' || route.query.hero === 'b') {
    return 'hero-study-page'
  }
  return null
})
</script>
