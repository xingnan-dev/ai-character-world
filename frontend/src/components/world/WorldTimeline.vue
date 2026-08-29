<template>
  <section class="timeline-panel" aria-live="polite">
    <div class="timeline-heading">
      <div><span>STORY TIMELINE</span><h2>世界里的对话</h2></div>
      <el-button v-if="hasMore" :loading="loadingMore" @click="$emit('load-more')">加载更早记录</el-button>
    </div>
    <el-empty v-if="!items.length" description="还没有互动记录，说点什么开启故事吧" />
    <div v-else class="round-list">
      <article v-for="item in items" :key="item.round.id" class="round-block">
        <div class="round-meta"><time>{{ timeText(item.round.createTime) }}</time><span :class="`round-${item.round.status.toLowerCase()}`">{{ roundText(item.round.status) }}</span></div>
        <div v-for="event in item.events" :key="event.id" class="event" :class="event.eventType === 'USER_MESSAGE' ? 'user-event' : 'ai-event'">
          <template v-if="event.eventType === 'USER_MESSAGE'">
            <div class="event-body"><b>你</b><p>{{ event.content }}</p></div><span class="avatar user-avatar">我</span>
          </template>
          <template v-else>
            <span class="avatar" :style="{ background: participant(event)?.character?.avatarColor || '#83cdf3' }">{{ participant(event)?.character?.name?.charAt(0) || '角' }}</span>
            <div class="event-body"><b>{{ participant(event)?.character?.name || '未知角色' }}</b><p v-if="event.status === 'COMPLETED'">{{ event.content }}</p><p v-else class="failed">{{ errorText(event.errorCode) }}</p></div>
          </template>
        </div>
        <div v-if="['PENDING','RUNNING'].includes(item.round.status)" class="waiting"><i></i><i></i><i></i><span>角色正在依次回应</span></div>
        <p v-if="item.round.status === 'PARTIAL_FAILED'" class="round-warning">{{ errorText(item.round.errorCode) }}</p>
        <p v-if="item.round.status === 'FAILED'" class="round-warning failed">{{ errorText(item.round.errorCode) }}</p>
      </article>
    </div>
  </section>
</template>

<script setup>
import { participantForEvent, worldInteractionErrorMessage } from '../../utils/worldInteraction'

const props = defineProps({
  items: { type: Array, default: () => [] },
  participants: { type: Array, default: () => [] },
  hasMore: Boolean,
  loadingMore: Boolean
})
defineEmits(['load-more'])

const participant = event => participantForEvent(props.participants, event)
const errorText = code => worldInteractionErrorMessage(code, '该角色暂时未能回应')
const roundText = status => ({ PENDING: '等待中', RUNNING: '进行中', COMPLETED: '已完成', PARTIAL_FAILED: '部分完成', FAILED: '未完成' })[status] || status
const timeText = value => value ? new Date(value).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : ''
</script>

<style lang="scss" scoped>
.timeline-panel{min-width:0;padding:24px;border:1px solid var(--world-border);border-radius:var(--world-radius-lg);background:var(--world-surface);box-shadow:var(--world-shadow-sm)}.timeline-heading{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:22px}.timeline-heading span{color:var(--world-primary);font-size:10px;font-weight:850;letter-spacing:2px}.timeline-heading h2{margin:4px 0 0;color:var(--world-ink);font-size:23px}.round-list{display:flex;flex-direction:column;gap:25px}.round-block{position:relative;padding-bottom:23px;border-bottom:1px dashed var(--world-border)}.round-block:last-child{padding-bottom:0;border-bottom:0}.round-meta{display:flex;align-items:center;justify-content:space-between;margin-bottom:13px;color:#8b979d;font-size:11px}.round-meta span{padding:4px 9px;border-radius:999px;background:var(--world-mint-soft);color:#31735b;font-weight:750}.round-meta .round-running,.round-meta .round-pending{color:var(--world-primary-strong);background:var(--world-primary-soft)}.round-meta .round-failed{color:var(--world-danger);background:#fff0ed}.event{display:flex;align-items:flex-start;gap:10px;margin:12px 0}.user-event{justify-content:flex-end}.avatar{width:38px;height:38px;display:grid;place-items:center;flex:0 0 auto;border:3px solid #fff;border-radius:13px;color:#fff;font-weight:800;box-shadow:0 3px 10px rgba(41,76,90,.13)}.user-avatar{background:var(--world-peach);color:#6e3218}.event-body{max-width:min(78%,620px)}.event-body b{display:block;margin:0 4px 4px;color:var(--world-ink);font-size:12px}.user-event .event-body b{text-align:right}.event-body p{margin:0;padding:11px 14px;border-radius:5px 16px 16px 16px;color:var(--world-ink);line-height:1.65;white-space:pre-wrap;overflow-wrap:anywhere;background:var(--world-surface-blue)}.user-event .event-body p{border-radius:16px 5px 16px 16px;background:var(--world-peach-soft)}.event-body p.failed,.round-warning.failed{color:var(--world-danger);background:#fff3f1}.waiting{display:flex;align-items:center;gap:4px;margin:13px 0 0 49px;color:var(--world-muted);font-size:12px}.waiting i{width:6px;height:6px;border-radius:50%;background:var(--world-primary);animation:bounce 1.2s infinite}.waiting i:nth-child(2){animation-delay:.15s}.waiting i:nth-child(3){animation-delay:.3s}.waiting span{margin-left:6px}.round-warning{margin:10px 0 0 49px;padding:9px 12px;border-radius:10px;color:#8b5b18;background:#fff8e7}@keyframes bounce{0%,60%,100%{transform:translateY(0)}30%{transform:translateY(-4px)}}@media(max-width:620px){.timeline-panel{padding:18px}.timeline-heading{align-items:flex-start}.timeline-heading h2{font-size:20px}.timeline-heading .el-button{padding:7px 10px}.event-body{max-width:82%}.round-warning,.waiting{margin-left:0}}
</style>
