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
            :class="{ active: item.id === currentSessionId }"
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
                <i class="topic-dot" :class="`is-${item.accent}`" aria-hidden="true"></i>
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
  height: 100%;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  color: var(--dusk-text);
}

.rail-head {
  flex: none;
  padding: 18px 16px 14px;
}

.rail-kicker {
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 9px;
  letter-spacing: 0.2em;
}

.rail-head__row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-top: 6px;
}

.rail-head h2 {
  color: var(--dusk-text);
  font-family: var(--dusk-font-display);
  font-size: 21px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.rail-head h2 small {
  margin-left: 4px;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 10px;
  font-weight: 400;
  letter-spacing: 0.06em;
}

.rail-head__note {
  color: var(--dusk-rose);
  font-size: 10px;
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
  gap: 2px;
}

.session-item {
  position: relative;
  width: 100%;
  min-width: 0;
  min-height: 78px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 10px;
  border: 1px solid transparent;
  border-radius: 12px;
  text-align: left;
  color: var(--dusk-text);
  transition:
    background var(--dusk-motion) var(--dusk-ease),
    border-color var(--dusk-motion) var(--dusk-ease);
}

.session-item:hover {
  background: rgba(232, 182, 199, 0.04);
}

.session-item.active {
  border-color: rgba(232, 182, 199, 0.2);
  background: linear-gradient(90deg, rgba(232, 182, 199, 0.11), rgba(232, 182, 199, 0.025));
}

.session-item.active::before {
  content: "";
  position: absolute;
  top: 15px;
  bottom: 15px;
  left: -1px;
  width: 2px;
  border-radius: 999px;
  background: linear-gradient(180deg, var(--dusk-rose-soft), rgba(232, 182, 199, 0.12));
}

.session-item :deep(.simple-avatar) {
  border-color: rgba(244, 239, 229, 0.26);
  border-width: 1px;
  box-shadow: 0 6px 18px rgba(3, 8, 20, 0.28);
}

.session-copy {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
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
  color: var(--dusk-text-2);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-top time {
  flex: none;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 8.5px;
  letter-spacing: 0.05em;
}

.session-topic {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow: hidden;
  color: var(--dusk-text);
  font-family: var(--dusk-font-display);
  font-size: 14px;
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
}

.topic-dot.is-rose {
  background: var(--dusk-rose);
  box-shadow: 0 0 0 3px rgba(232, 182, 199, 0.08);
}

.topic-dot.is-ice {
  background: var(--dusk-ice);
  box-shadow: 0 0 0 3px rgba(184, 216, 232, 0.08);
}

.topic-dot.is-gold {
  background: var(--dusk-gold);
  box-shadow: 0 0 0 3px rgba(214, 194, 154, 0.08);
}

.session-snippet {
  overflow: hidden;
  color: var(--dusk-text-3);
  font-size: 10px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item.active .session-top strong,
.session-item.active .session-snippet {
  color: var(--dusk-text-2);
}

.session-item:focus-visible {
  outline-offset: 1px;
}

.rail-foot {
  flex: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 15px 16px 17px;
  border-top: 1px solid var(--dusk-line-soft);
}

.rail-user {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--dusk-text-2);
  font-size: 11px;
}

.rail-user__initial {
  width: 27px;
  height: 27px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(244, 239, 229, 0.24);
  border-radius: 50%;
  color: var(--dusk-pearl);
  font-family: var(--dusk-font-display);
  font-size: 12px;
  background: rgba(184, 216, 232, 0.07);
}

.rail-user small {
  display: block;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 8px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.rail-home {
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: var(--dusk-radius-sm);
  color: var(--dusk-text-3);
  font-size: 10px;
  letter-spacing: 0.05em;
}

.rail-home:hover {
  color: var(--dusk-rose);
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
