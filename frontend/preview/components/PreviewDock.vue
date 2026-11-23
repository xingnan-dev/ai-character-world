<template>
  <aside class="preview-dock" :class="{ 'is-open': open }" aria-label="设计样稿预览控件">
    <button
      type="button"
      class="dock-tab"
      :aria-expanded="open"
      aria-controls="preview-dock-panel"
      @click="open = !open"
    >
      <span class="dock-led" aria-hidden="true"></span>
      <span class="dock-tab__label">样稿控件</span>
      <span class="dock-tab__tag">PREVIEW</span>
      <span class="dock-chevron" aria-hidden="true">{{ open ? '−' : '+' }}</span>
    </button>

    <div v-if="open" id="preview-dock-panel" class="dock-panel">
      <p class="dock-note">仅用于视觉与布局验证 · 不写入真实数据</p>

      <fieldset class="dock-group">
        <legend>对话状态</legend>
        <div class="state-grid">
          <button
            v-for="option in stateOptions"
            :key="option.value"
            type="button"
            class="state-button"
            :class="{ active: state === option.value }"
            :aria-pressed="state === option.value"
            @click="$emit('update:state', option.value)"
          >
            <span>{{ option.label }}</span>
            <small>{{ option.english }}</small>
          </button>
        </div>
      </fieldset>

      <fieldset class="dock-group">
        <legend>面板</legend>
        <div class="toggle-grid">
          <button
            type="button"
            class="toggle-button"
            :aria-pressed="profileOpen"
            @click="$emit('toggle-profile')"
          >
            <span>角色资料</span><b>{{ profileOpen ? '开' : '关' }}</b>
          </button>
          <button
            type="button"
            class="toggle-button"
            :aria-pressed="sessionsOpen"
            @click="$emit('toggle-sessions')"
          >
            <span>会话抽屉</span><b>{{ sessionsOpen ? '开' : '关' }}</b>
          </button>
        </div>
      </fieldset>

      <div class="dock-foot">
        <button type="button" class="reset-button" @click="$emit('reset')">重置样稿状态</button>
        <RouterLink to="/home" class="dock-home">首页 ↗</RouterLink>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  state: { type: String, default: 'normal' },
  profileOpen: { type: Boolean, default: false },
  sessionsOpen: { type: Boolean, default: false }
})

defineEmits(['update:state', 'toggle-profile', 'toggle-sessions', 'reset'])

const open = ref(false)

const stateOptions = [
  { value: 'normal', label: '对话', english: 'NORMAL' },
  { value: 'waiting', label: '等待', english: 'WAITING' },
  { value: 'generating', label: '生成', english: 'STREAM' },
  { value: 'long', label: '长回复', english: 'LONG' }
]
</script>

<style lang="scss" scoped>
.preview-dock {
  position: fixed;
  z-index: 85;
  bottom: 18px;
  left: 18px;
  width: 224px;
  color: var(--dream-ink-2);
  font-family: ui-monospace, "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  filter: drop-shadow(0 16px 34px rgba(65, 59, 76, 0.18));
}

.dock-tab {
  width: 100%;
  min-height: 44px;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 0 10px;
  border: 1px dashed rgba(199, 121, 144, 0.46);
  border-radius: 10px;
  color: var(--dream-ink-2);
  background: rgba(255, 252, 247, 0.9);
  backdrop-filter: blur(14px);
}

.dock-tab:hover {
  border-color: rgba(199, 121, 144, 0.7);
  color: var(--dream-ink);
}

.dock-led {
  width: 6px;
  height: 6px;
  flex: none;
  border-radius: 50%;
  background: var(--dream-rose);
  box-shadow: 0 0 8px rgba(199, 121, 144, 0.35);
}

.dock-tab__label {
  font-family: var(--dream-font-body);
  font-size: 11px;
  letter-spacing: 0.04em;
}

.dock-tab__tag {
  color: var(--dream-rose);
  font-size: 8px;
  letter-spacing: 0.12em;
}

.dock-chevron {
  margin-left: auto;
  color: var(--dream-ink-3);
  font-size: 13px;
}

.dock-panel {
  margin-top: 6px;
  padding: 12px;
  border: 1px dashed rgba(199, 121, 144, 0.36);
  border-radius: 12px;
  background: rgba(255, 252, 247, 0.96);
  box-shadow: var(--dream-shadow-soft);
  backdrop-filter: blur(16px);
}

.dock-note {
  margin-bottom: 11px;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-body);
  font-size: 9px;
  line-height: 1.6;
}

.dock-group {
  border: 0;
  margin-bottom: 11px;
}

.dock-group legend {
  margin-bottom: 6px;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-body);
  font-size: 9px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.state-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 5px;
}

.state-button {
  min-height: 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  gap: 1px;
  padding: 5px 9px;
  border: 1px solid rgba(58, 62, 91, 0.14);
  border-radius: 7px;
  color: var(--dream-ink-2);
  font-family: var(--dream-font-body);
  font-size: 11px;
  background: rgba(255, 253, 249, 0.6);
}

.state-button small {
  color: var(--dream-ink-3);
  font-size: 7px;
  letter-spacing: 0.1em;
}

.state-button:hover {
  border-color: rgba(85, 143, 168, 0.4);
}

.state-button.active {
  border-color: rgba(199, 121, 144, 0.52);
  color: var(--dream-ink);
  background: rgba(246, 224, 230, 0.56);
}

.state-button.active small {
  color: var(--dream-rose);
}

.toggle-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 5px;
}

.toggle-button {
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 0 8px;
  border: 1px solid rgba(58, 62, 91, 0.14);
  border-radius: 7px;
  color: var(--dream-ink-2);
  font-family: var(--dream-font-body);
  font-size: 10px;
  background: rgba(255, 253, 249, 0.6);
}

.toggle-button b {
  color: var(--dream-ink-3);
  font-size: 9px;
  font-weight: 500;
}

.toggle-button[aria-pressed="true"] {
  border-color: rgba(85, 143, 168, 0.4);
  background: rgba(232, 241, 246, 0.6);
}

.toggle-button[aria-pressed="true"] b {
  color: var(--dream-ice);
}

.dock-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-top: 9px;
  border-top: 1px dashed rgba(58, 62, 91, 0.14);
}

.reset-button {
  min-height: 44px;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-body);
  font-size: 9px;
}

.reset-button:hover {
  color: var(--dream-rose);
}

.dock-home {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-body);
  font-size: 9px;
}

.dock-home:hover {
  color: var(--dream-ice);
}

@media (max-width: 960px) {
  /* The chat room reserves a bottom strip for this tab when the side rail is
     gone, so the preview entry never sits on top of the composer. */
  .preview-dock {
    bottom: 12px;
    left: 12px;
    width: 204px;
  }

  .preview-dock.is-open {
    width: min(268px, calc(100vw - 24px));
  }

  .dock-panel {
    max-height: calc(100dvh - 210px);
    overflow-y: auto;
  }
}

@media (max-height: 700px) {
  .dock-panel {
    max-height: calc(100dvh - 160px);
    overflow-y: auto;
  }
}

@media (prefers-reduced-motion: reduce) {
  .preview-dock {
    transition: none;
  }
}

@media (forced-colors: active) {
  .dock-tab,
  .dock-panel,
  .state-button,
  .toggle-button {
    border: 1px solid CanvasText;
  }
}
</style>
