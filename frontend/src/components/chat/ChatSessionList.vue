<template>
  <section class="chat-session-list" aria-label="会话列表">
    <header class="session-list-heading">
      <span class="eyebrow">CONVERSATIONS</span>
      <h2>你的对话 <span class="session-count">{{ sessions.length }}</span></h2>
      <el-button type="primary" :icon="Plus" @click="$emit('create')">新建对话</el-button>
    </header>

    <div class="session-list-scroll">
      <div v-if="loading && !sessions.length" class="list-state" role="status">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon><p>正在加载会话…</p>
      </div>
      <div v-else-if="error && !sessions.length" class="list-state" role="alert">
        <p>{{ error }}</p><el-button @click="$emit('reload')">重新加载</el-button>
      </div>
      <div v-else-if="!sessions.length" class="list-state">
        <el-icon :size="32"><ChatLineRound /></el-icon>
        <h3>还没有对话</h3><p>选择一位角色，开始交流。</p>
        <el-button link type="primary" @click="$emit('create')">创建第一段对话</el-button>
      </div>
      <ul v-else class="chat-items">
        <li v-for="session in sessions" :key="session.id" class="session-row" :class="{ active: session.id === currentSessionId }">
          <button type="button" class="chat-item" :aria-pressed="session.id === currentSessionId" @click="$emit('select', session)">
            <ChatSessionAvatar :session="session" :user="user" :size="40" />
            <span class="chat-info">
              <strong>{{ session.title || assistantFor(session).name }}</strong>
              <small>与 {{ assistantFor(session).name }} 的对话</small>
            </span>
          </button>
          <button type="button" class="delete-session" :aria-label="`删除与 ${assistantFor(session).name} 的对话`" @click="$emit('delete', session)">
            <el-icon :size="16"><Delete /></el-icon>
          </button>
        </li>
      </ul>
    </div>
    <footer><RouterLink to="/characters">管理我的角色 <span aria-hidden="true">↗</span></RouterLink></footer>
  </section>
</template>

<script setup>
import { Plus, Loading, ChatLineRound, Delete } from '@element-plus/icons-vue'
import ChatSessionAvatar from './ChatSessionAvatar.vue'
import { resolveAssistantPresentation } from '../../utils/chatSessionPresentation'

defineProps({
  sessions: { type: Array, default: () => [] },
  currentSessionId: { type: [Number, String], default: null },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  user: { type: Object, default: () => ({}) }
})
defineEmits(['select', 'delete', 'create', 'reload'])
const assistantFor = resolveAssistantPresentation
</script>

<style lang="scss" scoped>
.chat-session-list{min-width:0;min-height:0;height:100%;display:flex;flex-direction:column;color:var(--app-text-primary)}
.session-list-heading{flex:none;padding:24px 18px 18px;border-bottom:1px solid var(--app-border)}
.eyebrow{color:var(--app-accent-primary-strong);font-size:var(--app-type-label);font-weight:800;letter-spacing:1.5px}
h2{display:flex;align-items:center;gap:10px;margin:8px 0 20px;font-size:22px;letter-spacing:-.02em}
.session-count{padding:3px 8px;border-radius:var(--app-radius-pill);color:var(--app-text-secondary);font-size:12px;font-weight:600;background:var(--app-surface-inset)}
.session-list-heading :deep(.el-button){width:100%;min-height:44px;margin:0;border-radius:var(--app-radius-sm);font-weight:750;--el-button-bg-color:var(--app-accent-primary);--el-button-border-color:var(--app-accent-primary);--el-button-text-color:var(--app-text-inverse);--el-button-hover-bg-color:var(--app-accent-primary-strong);--el-button-hover-border-color:var(--app-accent-primary-strong);--el-button-hover-text-color:var(--app-text-inverse)}
.session-list-scroll{min-height:0;flex:1;overflow-y:auto;overscroll-behavior:contain;padding:12px 8px}
.chat-items{display:flex;flex-direction:column;gap:6px}
.session-row{min-width:0;display:flex;align-items:center;gap:2px;border:1px solid transparent;border-radius:var(--app-radius-sm);transition:background var(--app-motion-fast),border-color var(--app-motion-fast)}
.session-row:hover{background:var(--app-surface-inset)}
.session-row.active{border-color:var(--app-accent-primary);background:var(--app-lavender-soft);box-shadow:inset 3px 0 var(--app-accent-primary)}
.chat-item{min-width:0;min-height:72px;flex:1;display:flex;align-items:center;gap:10px;padding:10px 4px 10px 10px;border-radius:var(--app-radius-sm);text-align:left;color:var(--app-text-primary)}
.chat-info{min-width:0;flex:1;display:flex;flex-direction:column;gap:5px}.chat-info strong,.chat-info small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.chat-info strong{font-size:14px;font-weight:750}.chat-info small{font-size:12px;color:var(--app-text-secondary)}
.delete-session{width:32px;min-height:44px;flex:none;display:grid;place-items:center;border-radius:var(--app-radius-sm);color:var(--app-text-secondary)}
.delete-session:hover{color:var(--app-danger);background:var(--app-danger-soft)}
.chat-item:focus-visible,.delete-session:focus-visible,footer a:focus-visible{outline:2px solid var(--app-accent-primary);outline-offset:2px;box-shadow:var(--app-focus-ring)}
.list-state{display:flex;flex-direction:column;align-items:center;gap:12px;padding:34px 12px;text-align:center;color:var(--app-text-secondary);line-height:1.6}.list-state .el-icon{color:var(--app-accent-primary)}.list-state h3{font-size:15px;color:var(--app-text-primary)}.list-state p{overflow-wrap:anywhere;font-size:13px}.list-state :deep(.el-button){color:var(--app-accent-primary-strong)}
footer{flex:none;padding:16px 18px;border-top:1px solid var(--app-border)}footer a{display:flex;align-items:center;justify-content:space-between;min-height:36px;border-radius:var(--app-radius-sm);color:var(--app-text-secondary);font-size:13px}footer a:hover{color:var(--app-accent-primary-strong)}
@media(max-width:1000px){.session-list-heading{padding-top:4px}.delete-session{width:44px}}
@media(prefers-reduced-motion:reduce){.session-row{transition:none}}
@media(forced-colors:active){.session-row.active{outline:2px solid Highlight}}
</style>
