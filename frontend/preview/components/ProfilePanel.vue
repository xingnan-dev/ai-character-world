<template>
  <Transition name="profile-fade">
    <div v-if="open" class="profile-layer">
      <button type="button" class="profile-scrim" aria-label="关闭角色资料" @click="$emit('close')"></button>
    <Transition name="profile-slide" appear>
      <aside
        v-if="open"
        ref="panelRef"
        class="profile-panel"
        role="region"
        aria-labelledby="profile-title"
        tabindex="-1"
        @keydown.esc="$emit('close')"
      >
        <header class="profile-head">
          <span class="profile-kicker">CHARACTER FILE · 角色资料</span>
          <button ref="closeRef" type="button" class="profile-close" aria-label="收起角色资料" @click="$emit('close')">×</button>
        </header>

        <div class="profile-identity">
          <span class="profile-halo" aria-hidden="true"></span>
          <span class="profile-portrait" aria-hidden="true">
            <img :src="session.imageUrl" alt="" />
          </span>
          <span class="profile-orbit" aria-hidden="true"></span>
          <DuskStar class="profile-star" :size="18" tone="rose" />
          <h2 id="profile-title" class="profile-name dusk-display">{{ session.characterName }}</h2>
          <span class="profile-english dusk-script">{{ englishName }}</span>
          <span class="profile-role">{{ session.identity }}</span>
          <span class="profile-online"><i aria-hidden="true"></i>在线 · 灯还亮着</span>
        </div>

        <p class="profile-biography">{{ session.biography }}</p>

        <dl class="profile-details">
          <template v-for="([label, value]) in details" :key="label">
            <dt>{{ label }}</dt>
            <dd>{{ value }}</dd>
          </template>
        </dl>

        <div class="profile-memory">
          <span class="profile-memory__label">最近留在这里的话</span>
          <p>{{ session.snippet }}</p>
          <span class="profile-memory__meta">{{ session.topic }} · {{ session.time }}</span>
        </div>

        <footer class="profile-foot">
          <DuskStar :size="12" tone="ice" :spark="false" />
          <span>设计样稿中的虚构资料，不写入真实数据。</span>
        </footer>
      </aside>
    </Transition>
    </div>
  </Transition>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import DuskStar from './DuskStar.vue'

const props = defineProps({
  session: { type: Object, required: true },
  open: { type: Boolean, default: false }
})

defineEmits(['close'])

const panelRef = ref(null)
const closeRef = ref(null)
const englishNames = {
  沈知微: 'Shen Zhiwei',
  江屿: 'Jiang Yu',
  苏枕月: 'Su Zhenyue'
}
const englishName = computed(() => englishNames[props.session.characterName] || '')
const details = computed(() => [
  ['性格', props.session.corePersonality].filter(Boolean),
  ['说话风格', props.session.speakingStyle].filter(Boolean),
  ['当前目标', props.session.currentGoal].filter(Boolean),
  ['与你的关系', props.session.relationshipToUser].filter(Boolean)
].filter(([, value]) => Boolean(value)))

watch(
  () => props.open,
  (open) => {
    if (!open) return
    nextTick(() => closeRef.value?.focus())
  }
)
</script>

<style lang="scss" scoped>
.profile-layer {
  position: absolute;
  z-index: 60;
  inset: 0;
  display: flex;
  justify-content: flex-end;
}

.profile-scrim {
  position: absolute;
  inset: 0;
  cursor: default;
  background: rgba(4, 8, 18, 0.54);
  backdrop-filter: blur(2px);
}

.profile-panel {
  position: relative;
  width: min(340px, 88vw);
  height: 100%;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 18px 20px 22px;
  color: var(--dusk-text);
  border-left: 1px solid rgba(232, 182, 199, 0.2);
  background:
    radial-gradient(ellipse at 88% 0%, rgba(238, 157, 184, 0.13), transparent 36%),
    linear-gradient(180deg, #121c38, #0c142b 72%, #0a1124);
  box-shadow: -24px 0 70px rgba(1, 4, 12, 0.42);
}

.profile-panel:focus-visible {
  outline-offset: -3px;
}

.profile-head {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--dusk-line-soft);
  background: linear-gradient(180deg, #121c38 68%, rgba(18, 28, 56, 0));
}

.profile-kicker {
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 8.5px;
  letter-spacing: 0.16em;
}

.profile-close {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  margin: -8px -8px -8px 0;
  border-radius: 50%;
  color: var(--dusk-text-2);
  font-size: 19px;
}

.profile-close:hover {
  color: var(--dusk-rose);
  background: rgba(232, 182, 199, 0.06);
}

.profile-identity {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0 20px;
  text-align: center;
}

.profile-halo {
  position: absolute;
  top: 4px;
  width: 190px;
  height: 190px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(238, 157, 184, 0.16), transparent 68%);
  filter: blur(16px);
  pointer-events: none;
}

.profile-portrait {
  position: relative;
  z-index: 1;
  width: 88px;
  height: 88px;
  display: block;
  border: 1px solid rgba(244, 239, 229, 0.34);
  border-radius: 50%;
  overflow: hidden;
  box-shadow:
    0 0 0 5px rgba(244, 239, 229, 0.04),
    0 16px 36px rgba(2, 6, 16, 0.34);
}

.profile-portrait img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-orbit {
  position: absolute;
  z-index: 0;
  top: 32px;
  width: 164px;
  height: 72px;
  border: 1px solid rgba(232, 182, 199, 0.16);
  border-radius: 50%;
  transform: rotate(-18deg);
  pointer-events: none;
}

.profile-star {
  position: absolute;
  z-index: 2;
  top: 22px;
  right: calc(50% - 62px);
  opacity: 0.7;
}

.profile-name {
  margin-top: 14px;
  color: var(--dusk-text);
  font-size: 26px;
  line-height: 1.15;
}

.profile-english {
  margin-top: 2px;
  color: var(--dusk-text-3);
  font-size: 17px;
  line-height: 1.3;
}

.profile-role {
  margin-top: 8px;
  color: var(--dusk-text-2);
  font-size: 12px;
  letter-spacing: 0.04em;
}

.profile-online {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 9px;
  color: var(--dusk-text-3);
  font-size: 9px;
  letter-spacing: 0.06em;
}

.profile-online i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--dusk-success);
  box-shadow: 0 0 8px rgba(143, 201, 172, 0.4);
}

.profile-biography {
  padding: 14px 0 16px;
  border-top: 1px solid var(--dusk-line-soft);
  color: var(--dusk-text-2);
  font-size: 12px;
  line-height: 1.85;
}

.profile-details {
  display: flex;
  flex-direction: column;
  gap: 11px;
  padding: 14px 0;
  border-top: 1px solid var(--dusk-line-soft);
}

.profile-details dt {
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 8.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.profile-details dd {
  margin-top: 2px;
  color: var(--dusk-text-2);
  font-size: 11.5px;
  line-height: 1.75;
}

.profile-memory {
  margin-top: 8px;
  padding: 12px 13px;
  border: 1px solid rgba(232, 182, 199, 0.16);
  border-radius: 10px;
  background: rgba(232, 182, 199, 0.035);
}

.profile-memory__label {
  color: var(--dusk-rose);
  font-family: var(--dusk-font-annotation);
  font-size: 8px;
  letter-spacing: 0.13em;
  text-transform: uppercase;
}

.profile-memory p {
  margin-top: 5px;
  color: var(--dusk-text-2);
  font-family: var(--dusk-font-display);
  font-size: 12px;
  line-height: 1.75;
}

.profile-memory__meta {
  display: block;
  margin-top: 6px;
  color: var(--dusk-text-3);
  font-size: 9px;
}

.profile-foot {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 20px;
  padding-top: 14px;
  border-top: 1px solid var(--dusk-line-soft);
  color: var(--dusk-text-3);
  font-size: 9px;
  line-height: 1.6;
}

.profile-fade-enter-active,
.profile-fade-leave-active {
  transition: opacity var(--dusk-motion) var(--dusk-ease);
}

.profile-fade-enter-from,
.profile-fade-leave-to {
  opacity: 0;
}

.profile-slide-enter-active,
.profile-slide-leave-active {
  transition: transform 320ms var(--dusk-ease);
}

.profile-slide-enter-from,
.profile-slide-leave-to {
  transform: translateX(100%);
}

@media (max-width: 640px) {
  .profile-panel {
    width: min(350px, 92vw);
    padding-right: 17px;
    padding-left: 17px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .profile-slide-enter-active,
  .profile-slide-leave-active,
  .profile-fade-enter-active,
  .profile-fade-leave-active {
    transition: none;
  }
}

@media (forced-colors: active) {
  .profile-panel,
  .profile-memory {
    border: 1px solid CanvasText;
  }

  .profile-halo,
  .profile-orbit {
    display: none;
  }
}
</style>
