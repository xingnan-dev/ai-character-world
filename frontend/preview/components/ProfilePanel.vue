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
            <span class="profile-orbit profile-orbit--one" aria-hidden="true"></span>
            <span class="profile-orbit profile-orbit--two" aria-hidden="true"></span>
            <span class="profile-portrait" aria-hidden="true">
              <img :src="session.imageUrl" alt="" />
            </span>
            <DuskStar class="profile-mark" :size="16" tone="rose" />
            <h2 id="profile-title" class="profile-name dream-display">{{ session.characterName }}</h2>
            <span class="profile-english dream-script">{{ englishName }}</span>
            <span class="profile-role">{{ session.identity }}</span>
            <span class="profile-online"><i aria-hidden="true"></i>在线 · 房间还亮着</span>
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
            <DuskStar :size="11" tone="ice" :spark="false" />
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
  阿梨: 'Ali',
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
  background: rgba(48, 47, 64, 0.2);
  backdrop-filter: blur(2px);
}

.profile-panel {
  position: relative;
  width: min(352px, 90vw);
  height: 100%;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 18px 20px 22px;
  color: var(--dream-ink);
  border-left: 1px solid rgba(58, 62, 91, 0.14);
  background:
    radial-gradient(ellipse at 90% 0%, rgba(236, 186, 199, 0.32), transparent 36%),
    radial-gradient(ellipse at 10% 90%, rgba(170, 198, 214, 0.22), transparent 42%),
    linear-gradient(168deg, rgba(255, 252, 247, 0.98), rgba(244, 238, 237, 0.96));
  box-shadow: -20px 0 56px rgba(60, 54, 74, 0.18);
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
  padding-bottom: 13px;
  border-bottom: 1px solid var(--dream-line-soft);
  background: linear-gradient(180deg, rgba(255, 252, 247, 0.98) 68%, rgba(255, 252, 247, 0));
}

.profile-kicker {
  color: var(--dream-ink-3);
  font-family: var(--dream-font-annotation);
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
  color: var(--dream-ink-2);
  font-size: 19px;
}

.profile-close:hover {
  color: var(--dream-rose);
  background: rgba(199, 121, 144, 0.08);
}

.profile-identity {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 26px 0 20px;
  text-align: center;
}

.profile-halo {
  position: absolute;
  top: 4px;
  width: 196px;
  height: 196px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(239, 191, 202, 0.3), transparent 68%);
  filter: blur(18px);
  pointer-events: none;
}

.profile-orbit {
  position: absolute;
  z-index: 0;
  border: 1px solid rgba(92, 130, 153, 0.2);
  border-radius: 50%;
  pointer-events: none;
}

.profile-orbit--one {
  top: 40px;
  width: 176px;
  height: 70px;
  transform: rotate(-16deg);
}

.profile-orbit--two {
  top: 64px;
  width: 138px;
  height: 148px;
  border-color: rgba(188, 126, 143, 0.16);
  transform: rotate(29deg);
}

.profile-portrait {
  position: relative;
  z-index: 1;
  width: 104px;
  height: 104px;
  display: block;
  overflow: hidden;
  border: 1px solid rgba(72, 68, 88, 0.2);
  border-radius: 50%;
  box-shadow:
    0 0 0 5px rgba(255, 255, 255, 0.5),
    0 16px 34px rgba(72, 66, 87, 0.18);
}

.profile-portrait img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-mark {
  position: absolute;
  z-index: 2;
  top: 24px;
  right: calc(50% - 67px);
  opacity: 0.72;
}

.profile-name {
  margin-top: 15px;
  color: var(--dream-ink);
  font-size: 27px;
  line-height: 1.16;
}

.profile-english {
  margin-top: 1px;
  color: var(--dream-rose);
  font-size: 19px;
  line-height: 1.3;
}

.profile-role {
  margin-top: 8px;
  color: var(--dream-ink-2);
  font-size: 11px;
  letter-spacing: 0.03em;
}

.profile-online {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 24px;
  margin-top: 9px;
  padding: 0 9px;
  border: 1px solid var(--dream-line-soft);
  border-radius: var(--dream-radius-pill);
  color: var(--dream-ink-3);
  font-size: 8.5px;
  background: rgba(255, 255, 255, 0.38);
}

.profile-online i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--dream-success);
}

.profile-biography {
  padding: 15px 16px;
  border: 1px solid var(--dream-line-soft);
  border-radius: 14px;
  color: var(--dream-ink-2);
  font-size: 12px;
  line-height: 1.82;
  background: rgba(255, 252, 247, 0.54);
}

.profile-details {
  margin-top: 16px;
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  gap: 0;
  border-top: 1px solid var(--dream-line-soft);
}

.profile-details dt {
  padding: 10px 0;
  border-bottom: 1px solid var(--dream-line-soft);
  color: var(--dream-ink-3);
  font-size: 9.5px;
  letter-spacing: 0.03em;
}

.profile-details dd {
  padding: 10px 0;
  border-bottom: 1px solid var(--dream-line-soft);
  color: var(--dream-ink-2);
  font-size: 10.5px;
  line-height: 1.72;
}

.profile-memory {
  margin-top: 17px;
  padding: 14px 15px;
  border: 1px solid rgba(188, 126, 143, 0.2);
  border-radius: 14px;
  background:
    radial-gradient(ellipse at 100% 0%, rgba(239, 197, 204, 0.26), transparent 56%),
    rgba(255, 250, 245, 0.62);
}

.profile-memory__label {
  display: block;
  color: var(--dream-rose);
  font-family: var(--dream-font-annotation);
  font-size: 8px;
  letter-spacing: 0.13em;
}

.profile-memory p {
  margin-top: 5px;
  color: var(--dream-ink);
  font-family: var(--dream-font-display);
  font-size: 12.5px;
  line-height: 1.75;
}

.profile-memory__meta {
  display: block;
  margin-top: 6px;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-annotation);
  font-size: 8px;
  letter-spacing: 0.08em;
}

.profile-foot {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 18px;
  padding-top: 13px;
  border-top: 1px solid var(--dream-line-soft);
  color: var(--dream-ink-3);
  font-size: 8px;
  line-height: 1.6;
}

.profile-fade-enter-active,
.profile-fade-leave-active {
  transition: opacity var(--dream-motion) var(--dream-ease);
}

.profile-fade-enter-from,
.profile-fade-leave-to {
  opacity: 0;
}

.profile-slide-enter-active,
.profile-slide-leave-active {
  transition: transform 300ms var(--dream-ease);
}

.profile-slide-enter-from,
.profile-slide-leave-to {
  transform: translateX(100%);
}

@media (max-width: 640px) {
  .profile-panel {
    width: min(340px, 92vw);
    padding: 15px 16px 20px;
  }

  .profile-portrait {
    width: 92px;
    height: 92px;
  }

  .profile-name {
    font-size: 24px;
  }

  .profile-details {
    grid-template-columns: 54px minmax(0, 1fr);
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
  .profile-portrait,
  .profile-biography,
  .profile-memory {
    border: 1px solid CanvasText;
  }

  .profile-halo,
  .profile-orbit,
  .profile-mark {
    display: none;
  }
}
</style>
