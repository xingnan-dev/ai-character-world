<template>
  <section class="run-list" :aria-busy="loading">
    <header class="run-list__header">
      <div><span class="eyebrow">RECENT RUNS</span><h2>最近执行</h2></div>
      <button type="button" class="refresh-button" aria-label="重新加载 Agent Run 列表" :disabled="loading" @click="$emit('retry')">↻</button>
    </header>
    <div v-if="error" class="inline-error" role="alert"><span>{{ error }}</span><button type="button" @click="$emit('retry')">重试</button></div>
    <div v-loading="loading" class="run-list__body">
      <p v-if="!loading && runs.length === 0" class="muted-state">暂无执行记录</p>
      <button
        v-for="run in runs"
        :key="run.id"
        type="button"
        class="run-item"
        :class="{ 'is-active': selectedRunId === run.id }"
        :aria-pressed="selectedRunId === run.id"
        :aria-label="`选择 Agent Run ${run.id}：${summarizeAgentGoal(run.goal)}`"
        @click="$emit('select', run.id)"
      >
        <span class="run-item__top"><strong>Run #{{ run.id }}</strong><span class="status" :data-tone="agentStatusPresentation(run.status).tone">{{ agentStatusPresentation(run.status).label }}</span></span>
        <span class="run-item__goal">{{ summarizeAgentGoal(run.goal) }}</span>
        <span v-if="canShowResume(run)" class="resume-badge">Resume available</span>
        <span class="run-item__meta"><span>{{ getRunRecoveryPresentation(run).label }} · {{ run.currentStep }} / {{ run.maxSteps }} steps</span><time>{{ formatAgentTime(run.updateTime || run.createTime) }}</time></span>
      </button>
    </div>
  </section>
</template>

<script setup>
import { agentStatusPresentation, canShowResume, formatAgentTime, getRunRecoveryPresentation, summarizeAgentGoal } from '../../utils/agentPresentation'

defineProps({
  runs: { type: Array, default: () => [] },
  selectedRunId: { type: Number, default: null },
  loading: Boolean,
  error: { type: String, default: '' }
})
defineEmits(['select', 'retry'])
</script>

<style lang="scss" scoped>
.run-list{height:100%;min-width:0;padding:22px;border:1px solid var(--app-border);border-radius:var(--app-radius-lg);background:rgba(255,255,255,.9);box-shadow:var(--app-shadow-sm)}
.run-list__header,.run-item__top,.run-item__meta{display:flex;align-items:center;justify-content:space-between;gap:10px}.run-list__header{margin-bottom:18px}.eyebrow{color:var(--app-primary);font-size:10px;font-weight:800;letter-spacing:1.5px}.run-list h2{margin-top:4px;font-size:20px}.refresh-button{width:38px;height:38px;border:1px solid var(--app-border);border-radius:50%;color:var(--app-primary-strong);background:var(--app-surface)}.refresh-button:focus-visible,.run-item:focus-visible,.inline-error button:focus-visible{outline:2px solid var(--app-primary);outline-offset:2px;box-shadow:var(--app-focus-ring)}
.run-list__body{min-height:120px;display:grid;align-content:start;gap:10px}.run-item{width:100%;min-width:0;padding:14px;text-align:left;border:1px solid var(--app-border);border-radius:var(--app-radius-md);color:var(--app-text);background:var(--app-surface);box-shadow:0 4px 15px rgba(46,73,86,.04)}.run-item:hover,.run-item.is-active{border-color:var(--app-primary);background:var(--app-primary-soft)}.run-item.is-active{box-shadow:inset 3px 0 0 var(--app-primary)}.run-item__top strong{font-size:12px}.run-item__goal{display:-webkit-box;overflow:hidden;margin:10px 0;line-height:1.45;color:var(--app-text-secondary);-webkit-box-orient:vertical;-webkit-line-clamp:2}.run-item__meta{font-size:11px;color:var(--app-text-muted)}
.status{padding:4px 8px;border-radius:var(--app-radius-pill);font-size:10px;font-weight:750;background:var(--app-surface-subtle)}.resume-badge{width:max-content;margin-bottom:8px;padding:3px 7px;border-radius:var(--app-radius-pill);color:var(--app-warning);font-size:10px;font-weight:750;background:var(--app-warning-soft)}.status[data-tone="running"]{color:var(--app-primary-strong);background:var(--app-primary-soft)}.status[data-tone="completed"]{color:var(--app-success);background:var(--app-success-soft)}.status[data-tone="failed"]{color:var(--app-danger);background:var(--app-danger-soft)}.status[data-tone="pending"]{color:var(--app-warning);background:var(--app-warning-soft)}
.inline-error{display:grid;gap:8px;margin-bottom:12px;padding:11px;border-radius:var(--app-radius-sm);color:var(--app-danger);background:var(--app-danger-soft)}.inline-error button{width:max-content;color:var(--app-danger);font-weight:700}.muted-state{padding:24px 4px;text-align:center;color:var(--app-text-muted)}
.run-list{border-color:var(--agent-border,var(--app-border));background:linear-gradient(160deg,rgba(40,44,68,.62),rgba(18,22,39,.66));box-shadow:var(--agent-shadow,var(--app-shadow-sm));backdrop-filter:blur(var(--agent-blur,18px));-webkit-backdrop-filter:blur(var(--agent-blur,18px))}.run-list__body{gap:3px}.run-item{position:relative;padding:13px 12px;border-color:transparent;border-radius:14px;background:transparent;box-shadow:none;transition:background .2s ease,border-color .2s ease,transform .2s ease}.run-item:hover{border-color:rgba(225,222,255,.11);background:rgba(255,255,255,.045);transform:translateY(-1px)}.run-item.is-active{border-color:rgba(184,165,255,.24);background:linear-gradient(105deg,rgba(157,128,255,.16),rgba(92,197,207,.07));box-shadow:inset 2px 0 0 rgba(198,183,255,.82),0 10px 30px rgba(8,8,25,.12)}.run-item.is-active::after{content:'';position:absolute;inset:16% 8% 16% 30%;z-index:-1;border-radius:50%;background:radial-gradient(circle,rgba(157,128,255,.12),transparent 70%)}.status{border:1px solid rgba(255,255,255,.08);background:rgba(12,15,29,.34)}.run-item__goal{color:var(--agent-text,var(--app-text));font-weight:620}.refresh-button{border-color:var(--agent-border,var(--app-border));background:rgba(255,255,255,.04)}
/* Light diffuse-gradient overrides. */
.run-list{border-color:var(--agent-border,var(--app-border));background:linear-gradient(155deg,rgba(255,255,253,.78),rgba(250,249,245,.58));box-shadow:var(--agent-shadow,var(--app-shadow-sm));backdrop-filter:blur(var(--agent-blur,24px));-webkit-backdrop-filter:blur(var(--agent-blur,24px))}.run-item{color:var(--agent-text,var(--app-text));background:transparent}.run-item:hover{border-color:rgba(94,101,118,.12);background:rgba(255,255,255,.5)}.run-item.is-active{border-color:rgba(118,91,208,.2);background:linear-gradient(105deg,rgba(150,125,235,.13),rgba(88,205,215,.09),rgba(255,135,174,.07));box-shadow:inset 2px 0 0 rgba(112,86,204,.62),0 10px 30px rgba(84,78,113,.08)}.run-item.is-active::after{background:radial-gradient(circle,rgba(109,208,215,.13),transparent 70%)}.status{border-color:rgba(76,91,96,.1);background:rgba(255,255,255,.52)}.refresh-button{border-color:var(--agent-border,var(--app-border));color:var(--app-primary-strong);background:rgba(255,255,255,.54)}
</style>
