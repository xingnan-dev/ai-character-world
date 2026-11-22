<template>
  <section class="payload-block">
    <header><h4>{{ title }}</h4><span>{{ payload.kind==='json'?'Structured JSON':'Text' }}</span></header>
    <pre :class="{ 'is-collapsed': payload.isLong&&!expanded }">{{ payload.formatted }}</pre>
    <button v-if="payload.isLong" type="button" :aria-label="`${expanded?'Collapse':'Expand'} ${title}`" @click="expanded=!expanded">{{ expanded?'收起':'展开全部' }}</button>
  </section>
</template>

<script setup>
import { ref } from 'vue'
defineProps({title:{type:String,required:true},payload:{type:Object,required:true}})
const expanded=ref(false)
</script>

<style lang="scss" scoped>
.payload-block{min-width:0;padding:14px;border:1px solid var(--app-border);border-radius:var(--app-radius-sm);background:var(--app-surface-subtle)}header{display:flex;justify-content:space-between;gap:8px;margin-bottom:9px}h4{font-size:12px}header span{color:var(--app-text-muted);font-size:10px}pre{max-width:100%;max-height:420px;overflow:auto;margin:0;padding:12px;border-radius:8px;color:var(--app-text);font:12px/1.55 ui-monospace,SFMono-Regular,Consolas,monospace;white-space:pre-wrap;overflow-wrap:anywhere;word-break:break-word;background:var(--app-surface)}pre.is-collapsed{max-height:150px;overflow:hidden;mask-image:linear-gradient(#000 65%,transparent)}button{margin-top:9px;color:var(--app-primary-strong);font-weight:750}button:focus-visible{outline:2px solid var(--app-primary);outline-offset:2px}
.payload-block{border-color:rgba(225,222,255,.12);background:rgba(11,14,27,.72);box-shadow:inset 0 1px 0 rgba(255,255,255,.035)}pre{border:1px solid rgba(225,222,255,.08);color:#e9e7f3;background:rgba(7,10,21,.76)}
/* Keep structured data crisp against the light, atmospheric shell. */
.payload-block{border-color:var(--agent-border,var(--app-border));background:rgba(255,255,253,.7);box-shadow:inset 0 1px 0 rgba(255,255,255,.82)}pre{border:1px solid rgba(76,91,96,.1);color:#27343c;background:rgba(248,248,245,.9)}
</style>
