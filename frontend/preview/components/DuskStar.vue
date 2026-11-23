<template>
  <svg
    class="dusk-star"
    :class="`dusk-star--${tone}`"
    :width="size"
    :height="size"
    viewBox="0 0 32 32"
    aria-hidden="true"
  >
    <path d="M16 2.5 18.6 13.4 29.5 16 18.6 18.6 16 29.5 13.4 18.6 2.5 16 13.4 13.4Z" fill="none" stroke="currentColor" stroke-width="1.1" />
    <circle cx="16" cy="16" r="1.5" fill="currentColor" opacity="0.7" />
    <circle v-if="spark" cx="25.5" cy="6.5" r="1.1" fill="currentColor" opacity="0.5" />
  </svg>
</template>

<script setup>
defineProps({
  size: { type: [Number, String], default: 18 },
  tone: { type: String, default: 'pearl' },
  spark: { type: Boolean, default: true }
})
</script>

<style scoped>
.dusk-star {
  display: inline-block;
  flex: none;
  color: rgba(244, 239, 229, 0.7);
}

.dusk-star--rose {
  color: var(--dusk-rose);
}

.dusk-star--ice {
  color: var(--dusk-ice);
}

.dusk-star--gold {
  color: var(--dusk-gold);
}

.dusk-star--pearl {
  color: var(--dusk-pearl);
}
</style>
