<template>
  <aside class="chat-character-panel app-surface--glass" aria-label="会话角色资料">
    <span class="panel-eyebrow">会话角色</span>
    <ChatSessionAvatar :session="session" :user="user" size="xl" show-type />
    <h4>{{ character.name }}</h4>
    <p v-if="session?.biography" class="panel-biography">{{ session.biography }}</p>
    <dl v-if="details.length" class="panel-details">
      <template v-for="([label, value]) in details" :key="label">
        <dt>{{ label }}</dt><dd>{{ value }}</dd>
      </template>
    </dl>
    <p v-else class="panel-empty">角色资料未设置</p>
    <div class="panel-footer"><span class="status-dot"></span>{{ streaming ? '正在与你交流' : '陪伴在线' }}</div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import ChatSessionAvatar from './ChatSessionAvatar.vue'
import { resolveAssistantPresentation, resolveCharacterDetails } from '../../utils/chatSessionPresentation'

const props = defineProps({
  session: { type: Object, default: null },
  user: { type: Object, default: () => ({}) },
  streaming: { type: Boolean, default: false }
})
const character = computed(() => resolveAssistantPresentation(props.session))
const details = computed(() => resolveCharacterDetails(props.session))
</script>

<style lang="scss" scoped>
.chat-character-panel{min-width:0;min-height:0;display:flex;flex-direction:column;align-items:center;gap:18px;overflow:auto;overscroll-behavior:contain;padding:24px 20px;color:var(--app-text-primary)}
.panel-eyebrow{align-self:flex-start;color:var(--app-accent-primary-strong);font-size:var(--app-type-label);font-weight:800;letter-spacing:1.4px}.chat-character-panel h4{max-width:100%;margin:0;text-align:center;overflow-wrap:anywhere;font-size:20px;letter-spacing:-.02em}.panel-biography{margin:0;color:var(--app-text-secondary);font-size:13px;line-height:1.7;text-align:center;overflow-wrap:anywhere}.panel-details{align-self:stretch;display:flex;flex-direction:column;gap:6px;margin:0;padding:16px;border:1px solid var(--app-border);border-radius:var(--app-radius-sm);background:var(--app-surface-inset)}.panel-details dt{color:var(--app-text-secondary);font-size:12px}.panel-details dt:not(:first-child){margin-top:12px}.panel-details dd{min-width:0;margin:0;color:var(--app-text-primary);font-size:13px;line-height:1.65;overflow-wrap:anywhere}.panel-empty{color:var(--app-text-secondary);font-size:13px}.panel-footer{display:flex;align-items:center;gap:7px;margin-top:auto;padding-top:12px;color:var(--app-text-secondary);font-size:12px}.status-dot{width:6px;height:6px;border-radius:50%;background:var(--app-success)}
</style>
