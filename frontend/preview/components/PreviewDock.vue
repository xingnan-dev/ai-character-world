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
  color: var(--dusk-text-2);
  font-family: ui-monospace, "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  filter: drop-shadow(0 18px 38px rgba(1, 4, 12, 0.45));
}

.dock-tab {
  width: 100%;
  min-height: 44px;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 0 10px;
  border: 1px dashed rgba(232, 182, 199, 0.42);
  border-radius: 10px;
  color: var(--dusk-text-2);
  background: rgba(9, 14, 29, 0.92);
  backdrop-filter: blur(14px);
}

.dock-tab:hover {
  border-color: rgba(232, 182, 199, 0.66);
  color: var(--dusk-text);
}

.dock-led {
  width: 6px;
  height: 6px;
  flex: none;
  border-radius: 50%;
  background: var(--dusk-rose);
  box-shadow: 0 0 8px rgba(232, 182, 199, 0.48);
}

.dock-tab__label {
  font-family: var(--dusk-font-body);
  font-size: 11px;
  letter-spacing: 0.04em;
}

.dock-tab__tag {
  color: var(--dusk-rose);
  font-size: 8px;
  letter-spacing: 0.12em;
}

.dock-chevron {
  margin-left: auto;
  color: var(--dusk-text-3);
  font-size: 13px;
}

.dock-panel {
  margin-top: 6px;
  padding: 12px;
  border: 1px dashed rgba(232, 182, 199, 0.32);
  border-radius: 12px;
  background: rgba(9, 14, 29, 0.96);
  backdrop-filter: blur(16px);
}

.dock-note {
  margin-bottom: 11px;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-body);
  font-size: 9px;
  line-height: 1.6;
}

.dock-group {
  border: 0;
  margin-bottom: 11px;
}

.dock-group legend {
  margin-bottom: 6px;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-body);
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
  border: 1px solid rgba(216, 224, 248, 0.14);
  border-radius: 7px;
  color: var(--dusk-text-2);
  font-family: var(--dusk-font-body);
  font-size: 11px;
  background: rgba(22, 31, 56, 0.6);
}

.state-button small {
  color: var(--dusk-text-3);
  font-size: 7px;
  letter-spacing: 0.1em;
}

.state-button:hover {
  border-color: rgba(184, 216, 232, 0.42);
}

.state-button.active {
  border-color: rgba(232, 182, 199, 0.52);
  color: var(--dusk-rose-soft);
  background: rgba(232, 182, 199, 0.09);
}

.state-button.active small {
  color: var(--dusk-rose);
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
  border: 1px solid rgba(216, 224, 248, 0.12);
  border-radius: 7px;
  color: var(--dusk-text-2);
  font-family: var(--dusk-font-body);
  font-size: 10px;
  background: rgba(22, 31, 56, 0.52);
}

.toggle-button b {
  color: var(--dusk-text-3);
  font-size: 9px;
  font-weight: 500;
}

.toggle-button[aria-pressed="true"] {
  border-color: rgba(184, 216, 232, 0.38);
}

.toggle-button[aria-pressed="true"] b {
  color: var(--dusk-ice);
}

.dock-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-top: 9px;
  border-top: 1px dashed rgba(216, 224, 248, 0.14);
}

.reset-button {
  min-height: 44px;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-body);
  font-size: 9px;
}

.reset-button:hover {
  color: var(--dusk-rose);
}

.dock-home {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-body);
  font-size: 9px;
}

.dock-home:hover {
  color: var(--dusk-ice);
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

@media (forced-colors: active) {
  .dock-tab,
  .dock-panel,
  .state-button,
  .toggle-button {
    border: 1px solid CanvasText;
  }
}
</style>
