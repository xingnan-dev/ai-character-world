<template>
  <aside class="world-sidebar">
    <section class="world-summary">
      <span class="eyebrow">WORLD NOTES</span>
      <h2>{{ world?.atmosphere || '世界氛围' }}</h2>
      <p>{{ world?.background || '这个世界的故事正在等待你开启。' }}</p>
      <div v-if="world?.rules" class="rules"><b>世界规则</b><p>{{ world.rules }}</p></div>
    </section>
    <section class="cast-panel">
      <div class="cast-title"><span class="eyebrow">CAST</span><b>{{ participants.length }} 位角色</b></div>
      <article v-for="participant in participants" :key="participant.id" :class="{ speaking: String(participant.id) === String(speakingParticipantId) }">
        <span class="avatar" :style="{ background: participant.character?.avatarColor || '#83cdf3' }">{{ participant.character?.name?.charAt(0) || '角' }}</span>
        <div><h3>{{ participant.character?.name || '未知角色' }}</h3><p>{{ summary(participant.character) }}</p><small v-if="String(participant.id) === String(speakingParticipantId)">正在准备回应…</small></div>
      </article>
    </section>
  </aside>
</template>

<script setup>
import { snapshotSummary } from '../../utils/worldBuilder'
defineProps({ world: Object, participants: { type: Array, default: () => [] }, speakingParticipantId: [Number, String] })
const summary = snapshot => snapshotSummary(snapshot)
</script>

<style lang="scss" scoped>
.world-sidebar{display:flex;flex-direction:column;gap:18px}.world-summary,.cast-panel{padding:22px;border:1px solid var(--world-border);border-radius:var(--world-radius-lg);background:var(--world-surface);box-shadow:var(--world-shadow-sm)}.world-summary{border-top:5px solid var(--world-peach)}.eyebrow{color:var(--world-primary);font-size:10px;font-weight:850;letter-spacing:1.8px}.world-summary h2{margin:7px 0 10px;color:var(--world-ink);font-size:20px}.world-summary p{color:var(--world-muted);line-height:1.65;white-space:pre-wrap}.rules{margin-top:15px;padding:13px;border-radius:var(--world-radius-sm);background:var(--world-peach-soft)}.rules b{color:#793b20}.rules p{margin-top:4px}.cast-panel{border-top:5px solid var(--world-mint)}.cast-title{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;color:var(--world-ink)}article{display:flex;align-items:center;gap:11px;padding:11px;margin-top:8px;border:1px solid transparent;border-radius:14px;background:var(--world-surface-blue);transition:.2s}article.speaking{border-color:var(--world-primary);background:var(--world-primary-soft);box-shadow:0 0 0 3px rgba(22,138,192,.08)}.avatar{width:43px;height:43px;display:grid;place-items:center;flex:0 0 auto;border:3px solid #fff;border-radius:14px;color:#fff;font-weight:800;box-shadow:0 4px 10px rgba(35,71,86,.13)}h3,p{margin:0}h3{color:var(--world-ink);font-size:14px}article p{margin-top:2px;color:var(--world-muted);font-size:12px;line-height:1.4}small{display:block;margin-top:4px;color:var(--world-primary);font-weight:700}@media(max-width:850px){.world-sidebar{display:grid;grid-template-columns:1fr 1.25fr}}@media(max-width:620px){.world-sidebar{display:flex}.world-summary p{display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden}.cast-panel{overflow:hidden}.cast-panel article{min-width:0}}
</style>
