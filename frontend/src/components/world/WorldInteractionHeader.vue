<template>
  <header class="interaction-header">
    <button type="button" class="back" @click="$emit('back')">← 返回世界详情</button>
    <div v-if="world" class="world-title">
      <span>WORLD INTERACTION</span>
      <h1>{{ world.name }}</h1>
      <p>{{ world.scene || world.atmosphere || '一个等待故事发生的世界' }}</p>
    </div>
    <div v-if="round" class="round-status" :class="statusClass">
      <i></i>{{ statusText }}
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ world: Object, round: Object })
defineEmits(['back'])

const statusText = computed(() => ({
  PENDING: '等待开始',
  RUNNING: '角色依次回应中',
  COMPLETED: '本轮已完成',
  PARTIAL_FAILED: '部分角色未回应',
  FAILED: '本轮未完成'
})[props.round?.status] || '可以开始互动')

const statusClass = computed(() => `status-${(props.round?.status || 'ready').toLowerCase()}`)
</script>

<style lang="scss" scoped>
.interaction-header{width:min(var(--world-content),calc(100% - 48px));margin:auto;padding:27px 0 22px;display:grid;grid-template-columns:190px 1fr auto;align-items:center;gap:24px}.back{justify-self:start;color:var(--world-muted);font-weight:750}.world-title{text-align:center}.world-title span{color:var(--world-primary);font-size:11px;font-weight:850;letter-spacing:2px}.world-title h1{margin:4px 0;color:var(--world-ink);font-size:30px}.world-title p{color:var(--world-muted)}.round-status{display:flex;align-items:center;gap:8px;padding:9px 13px;border:1px solid var(--world-border);border-radius:999px;color:var(--world-muted);background:#fff;font-weight:700;white-space:nowrap}.round-status i{width:8px;height:8px;border-radius:50%;background:var(--world-mint)}.status-pending i,.status-running i{background:var(--world-primary);animation:pulse 1.4s infinite}.status-partial_failed{color:#976019;background:#fff8e7}.status-failed{color:var(--world-danger);background:#fff3f1}@media(max-width:700px){.interaction-header{width:calc(100% - 32px);grid-template-columns:1fr auto;gap:12px;padding:18px 0}.world-title{grid-column:1/-1;grid-row:2;text-align:left}.world-title h1{font-size:25px}.round-status{font-size:12px}}
</style>
