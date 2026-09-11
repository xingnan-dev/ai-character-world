<template>
  <aside class="chat-character-panel">
    <span class="panel-eyebrow">CHAT CHARACTER</span>
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
.chat-character-panel{min-width:0;min-height:0;display:flex;flex-direction:column;align-items:center;gap:14px;overflow:auto;padding:24px;color:#f7f8ff;background:radial-gradient(circle at 50% 28%,rgba(111,98,255,.26),transparent 38%),linear-gradient(160deg,#1a203b 0%,#11182c 58%,#152641 100%);border:1px solid rgba(126,139,255,.22);border-radius:20px;box-shadow:0 20px 55px rgba(24,31,62,.18)}
.panel-eyebrow{color:rgba(190,199,255,.68);font-size:10px;font-weight:700;letter-spacing:.16em}.chat-character-panel h4{margin:2px 0 0;font-size:20px}.panel-biography{margin:0;color:rgba(235,238,255,.74);font-size:13px;line-height:1.6;text-align:center}.panel-details{align-self:stretch;display:grid;grid-template-columns:72px 1fr;gap:10px 12px;margin:4px 0;padding:16px;background:rgba(255,255,255,.06);border-radius:14px}.panel-details dt{color:rgba(190,199,255,.64);font-size:12px}.panel-details dd{margin:0;color:#f4f6ff;font-size:13px;line-height:1.45}.panel-empty{color:rgba(231,235,255,.55);font-size:13px}.panel-footer{display:flex;align-items:center;gap:7px;margin-top:auto;color:rgba(231,235,255,.68);font-size:12px}.status-dot{width:8px;height:8px;border-radius:50%;background:#67c23a}
</style>
