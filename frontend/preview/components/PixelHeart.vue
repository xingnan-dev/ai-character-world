<template>
  <svg
    class="pixel-heart"
    :class="`pixel-heart--${tone}`"
    :width="size"
    :height="size * 7 / 8"
    viewBox="0 0 8 7"
    aria-hidden="true"
    shape-rendering="crispEdges"
  >
    <g fill="currentColor">
      <rect v-for="cell in cells" :key="cell" :x="cell[0]" :y="cell[1]" width="1" height="1" />
    </g>
    <g class="pixel-heart__glint">
      <rect x="1" y="1" width="1" height="1" />
      <rect x="2" y="1" width="1" height="1" />
      <rect x="1" y="2" width="1" height="1" />
    </g>
  </svg>
</template>

<script setup>
defineProps({
  size: { type: [Number, String], default: 14 },
  tone: { type: String, default: 'rose' }
})

const grid = [
  '01100110',
  '11111111',
  '11111111',
  '11111111',
  '01111110',
  '00111100',
  '00011000'
]

const cells = grid.flatMap((row, y) =>
  [...row].flatMap((value, x) => (value === '1' ? [[x, y]] : []))
)
</script>

<style scoped>
.pixel-heart {
  display: inline-block;
  flex: none;
  color: var(--dream-rose);
  opacity: 0.6;
}

.pixel-heart--ice {
  color: var(--dream-ice);
}

.pixel-heart--gold {
  color: var(--dream-gold);
}

.pixel-heart__glint {
  fill: var(--dream-pearl);
  opacity: 0.72;
}
</style>
