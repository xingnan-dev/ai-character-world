<template>
  <section class="session-rail" aria-label="对话会话列表">
    <header class="rail-head">
      <span class="rail-kicker">DIALOGUES</span>
      <div class="rail-head__row">
        <h2>对话 <small>{{ sessions.length }}</small></h2>
        <span class="rail-head__note">最近</span>
      </div>
    </header>

    <div class="rail-scroll">
      <ul class="rail-list">
        <li v-for="item in sessions" :key="item.id">
          <button
            type="button"
            class="session-item"
            :class="[`is-${item.accent}`, { active: item.id === currentSessionId }]"
            :aria-pressed="item.id === currentSessionId"
            @click="$emit('select', item.id)"
          >
            <ChatSessionAvatar :session="item" :user="PREVIEW_USER" :size="36" />
            <span class="session-copy">
              <span class="session-top">
                <strong>{{ item.characterName }}</strong>
                <time>{{ item.time }}</time>
              </span>
              <span class="session-topic">
                <i class="topic-dot" aria-hidden="true"></i>
                {{ item.topic }}
              </span>
              <span class="session-snippet">{{ item.snippet }}</span>
            </span>
          </button>
        </li>
      </ul>
    </div>

    <footer class="rail-foot">
      <span class="rail-user">
        <span class="rail-user__initial" aria-hidden="true">{{ PREVIEW_USER.nickname.charAt(0) }}</span>
        <span>{{ PREVIEW_USER.nickname }}<small>预览用户</small></span>
      </span>
      <RouterLink to="/home" class="rail-home">回到首页 <span aria-hidden="true">↗</span></RouterLink>
    </footer>
  </section>
</template>

<script setup>
import ChatSessionAvatar from '../../src/components/chat/ChatSessionAvatar.vue'
import { PREVIEW_USER } from '../data.js'

defineProps({
  sessions: { type: Array, default: () => [] },
  currentSessionId: { type: [String, Number], default: null }
})

defineEmits(['select'])
</script>

<style lang="scss" scoped>
.session-rail {
  --rail-accent: var(--dream-rose);
  height: 100%;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  color: var(--dream-ink);
}

.rail-head {
  flex: none;
  padding: 18px 16px 13px;
}

.rail-kicker {
  color: var(--dream-ink-3);
  font-family: var(--dream-font-annotation);
  font-size: 8.5px;
  letter-spacing: 0.2em;
}

.rail-head__row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-top: 5px;
}

.rail-head h2 {
  color: var(--dream-ink);
  font-family: var(--dream-font-display);
  font-size: 21px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.rail-head h2 small {
  margin-left: 4px;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-annotation);
  font-size: 10px;
  font-weight: 400;
  letter-spacing: 0.06em;
}

.rail-head__note {
  color: var(--dream-rose);
  font-size: 9.5px;
  letter-spacing: 0.06em;
}

.rail-scroll {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 2px 8px 14px;
}

.rail-list {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.session-item {
  position: relative;
  width: 100%;
  min-width: 0;
  min-height: 82px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px;
  border: 1px solid transparent;
  border-radius: 12px;
  text-align: left;
  color: var(--dream-ink);
  transition:
    background var(--dream-motion) var(--dream-ease),
    border-color var(--dream-motion) var(--dream-ease);
}

.session-item.is-sun {
  --rail-accent: var(--dream-apricot);
}

.session-item.is-rose {
  --rail-accent: var(--dream-rose);
}

.session-item.is-ice {
  --rail-accent: var(--dream-ice);
}

.session-item.is-violet {
  --rail-accent: var(--dream-violet);
}

.session-item:hover {
  background: rgba(255, 252, 247, 0.5);
}

.session-item.active {
  border-color: rgba(58, 62, 91, 0.12);
  background: linear-gradient(105deg, rgba(255, 253, 249, 0.88), rgba(252, 244, 240, 0.6));
  box-shadow: 0 5px 16px rgba(74, 68, 88, 0.06);
}

.session-item.active::before {
  content: "";
  position: absolute;
  top: 17px;
  bottom: 17px;
  left: -1px;
  width: 2px;
  border-radius: 999px;
  background: linear-gradient(180deg, var(--rail-accent), rgba(199, 121, 144, 0.08));
}

.session-item :deep(.simple-avatar) {
  border-color: rgba(72, 68, 88, 0.18);
  border-width: 1px;
  box-shadow: 0 5px 14px rgba(71, 67, 91, 0.12);
}

.session-copy {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.session-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 6px;
  min-width: 0;
}

.session-top strong {
  overflow: hidden;
  color: var(--dream-ink-2);
  font-size: 10.5px;
  font-weight: 500;
  letter-spacing: 0.04em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-top time {
  flex: none;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-annotation);
  font-size: 8px;
  letter-spacing: 0.04em;
}

.session-topic {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow: hidden;
  color: var(--dream-ink);
  font-family: var(--dream-font-display);
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topic-dot {
  width: 4px;
  height: 4px;
  flex: none;
  border-radius: 50%;
  background: var(--rail-accent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--rail-accent) 12%, transparent);
}

.session-snippet {
  overflow: hidden;
  color: var(--dream-ink-3);
  font-size: 9.5px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item.active .session-top strong {
  color: var(--dream-ink);
}

.session-item.active .session-snippet {
  color: var(--dream-ink-2);
}

.session-item:focus-visible {
  outline-offset: 1px;
}

.rail-foot {
  flex: none;
  display: flex;
  flex-direction: column;
  gap: 9px;
  padding: 14px 16px 16px;
  border-top: 1px solid var(--dream-line-soft);
}

.rail-user {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--dream-ink-2);
  font-size: 10.5px;
}

.rail-user__initial {
  width: 27px;
  height: 27px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(58, 62, 91, 0.16);
  border-radius: 50%;
  color: var(--dream-ink);
  font-family: var(--dream-font-display);
  font-size: 12px;
  background: linear-gradient(145deg, rgba(216, 231, 238, 0.6), rgba(244, 227, 231, 0.56));
}

.rail-user small {
  display: block;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-annotation);
  font-size: 7.5px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.rail-home {
  min-height: 42px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: var(--dream-radius-sm);
  color: var(--dream-ink-3);
  font-size: 9.5px;
  letter-spacing: 0.05em;
}

.rail-home:hover {
  color: var(--dream-rose);
}

.rail-home:focus-visible {
  outline-offset: 2px;
}

@media (max-width: 1100px) {
  .rail-head {
    padding-right: 12px;
    padding-left: 12px;
  }

  .rail-foot {
    padding-right: 12px;
    padding-left: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .session-item {
    transition: none;
  }
}

@media (forced-colors: active) {
  .session-item.active {
    outline: 2px solid Highlight;
  }
}
</style>
