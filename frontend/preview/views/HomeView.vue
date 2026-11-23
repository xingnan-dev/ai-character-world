<template>
  <main class="home-view" id="main-content">
    <section class="hero" aria-labelledby="hero-title">
      <div class="hero-art" aria-hidden="true">
        <img :src="heroDream" alt="" />
        <span class="hero-art__wash"></span>
        <span class="hero-art__beam"></span>
      </div>
      <span class="hero-orbit hero-orbit--one" aria-hidden="true"></span>
      <span class="hero-orbit hero-orbit--two" aria-hidden="true"></span>
      <DuskStar class="hero-mark hero-mark--one" :size="18" tone="rose" />
      <DuskStar class="hero-mark hero-mark--two" :size="11" tone="ice" :spark="false" />

      <div class="home-wrap hero-inner">
        <div class="hero-copy">
          <span class="dream-kicker">AIWORLD · 浮光卷 / DREAMLIGHT</span>
          <p class="hero-script dream-script" aria-hidden="true">kept, softly</p>
          <h1 id="hero-title" class="hero-title dream-display">
            <span class="hero-title__line">有人记得</span>
            <span class="hero-title__line">你说过的话<span class="hero-title__dot">。</span></span>
          </h1>
          <p class="hero-lede">
            和不同脾气的人聊天：旧书的纸页、夜里的唱片，还有阳台上刚结出的番茄。你不必每次都从头解释自己。
          </p>

          <button
            type="button"
            class="continue-card"
            :aria-label="`继续与${recentSession.characterName}的对话：${recentSession.topic}`"
            @click="openChat(RECENT_SESSION_ID)"
          >
            <span class="continue-avatar" aria-hidden="true">
              <img :src="recentSession.imageUrl" alt="" />
              <i></i>
            </span>
            <span class="continue-copy">
              <span class="continue-eyebrow">继续上次 · LAST CONVERSATION</span>
              <span class="continue-title-row">
                <strong>{{ recentSession.topic }}</strong>
                <em>{{ recentSession.characterName }}</em>
              </span>
              <span class="continue-quote">{{ recentSession.snippet }}</span>
            </span>
            <span class="continue-side">
              <time>{{ recentSession.time }}</time>
              <span class="continue-cta">继续对话 <b aria-hidden="true">→</b></span>
            </span>
          </button>

          <div class="hero-foot">
            <span>04 个房间 · 01 条微光街</span>
            <span class="hero-foot__line" aria-hidden="true"></span>
            <span class="hero-script hero-foot__script" aria-hidden="true">a quieter way to talk</span>
          </div>
        </div>
      </div>

      <div class="art-note" aria-hidden="true">
        <span>MATERIAL STUDY · N°02</span>
        <small>silk / light / pearl</small>
      </div>
    </section>

    <div class="home-wrap home-body">
      <section class="characters" id="characters" aria-labelledby="characters-title">
        <header class="section-heading">
          <div>
            <span class="dream-kicker">CHARACTERS / 02</span>
            <h2 id="characters-title" class="dream-display">四种脾气，四张门</h2>
          </div>
          <p>他们各自记得，上一次谈话停在哪里。</p>
        </header>

        <ul class="character-grid">
          <li v-for="character in CHARACTERS" :key="character.id">
            <button
              type="button"
              class="character-card"
              :class="`is-${character.accent}`"
              :aria-label="`打开与 ${character.name} 的对话`"
              @click="openChat(character.sessionId)"
            >
              <span class="character-portrait" aria-hidden="true">
                <img :src="character.portrait" alt="" loading="lazy" />
                <i class="character-tint"></i>
              </span>
              <span class="character-copy">
                <span class="character-name">
                  <strong>{{ character.name }}</strong>
                  <em class="dream-script">{{ character.englishName }}</em>
                </span>
                <span class="character-identity">{{ character.identity }}</span>
                <span class="character-note">{{ character.lastNote }}</span>
              </span>
              <span class="character-arrow" aria-hidden="true">↗</span>
            </button>
          </li>
        </ul>
        <div class="character-foot">
          <span>四个房间都亮着灯</span>
          <span class="character-foot__line" aria-hidden="true"></span>
          <span class="dream-script" aria-hidden="true">more rooms, later</span>
        </div>
      </section>

      <section class="world-band" id="world" aria-labelledby="world-title">
        <div class="world-art" aria-hidden="true">
          <img :src="worldDream" alt="" loading="lazy" />
          <span class="world-art__veil"></span>
          <span class="world-orbit"></span>
        </div>
        <div class="world-copy">
          <span class="dream-kicker">WORLD / 03</span>
          <h2 id="world-title" class="dream-display">微光街区</h2>
          <p>一条小街，几盏还亮着的灯。角色们共享天气、时间和一些很小的事。</p>
          <div class="world-meta">
            <span><i aria-hidden="true"></i>街区亮着</span>
            <span>4 位角色</span>
            <span>今晚 · 小雨</span>
          </div>
          <button type="button" class="world-link" @click="showWorldNotice">
            进入世界 <span aria-hidden="true">→</span>
          </button>
        </div>
        <span class="world-stamp" aria-hidden="true">02:47 / STREET LIGHT</span>
      </section>

      <footer class="home-footer">
        <div class="home-footer__brand">
          <span class="dream-script">AIworld</span>
          <small>浮光卷 · dreamlight edition</small>
        </div>
        <p>DESIGN PREVIEW · 虚构角色与内容，仅用于视觉与布局验证；不写入任何真实数据。</p>
        <button type="button" class="footer-top" @click="backToTop">
          回到顶部 <span aria-hidden="true">↑</span>
        </button>
      </footer>
    </div>

    <Transition name="notice">
      <div v-if="worldNotice" class="preview-notice" role="status" aria-live="polite">
        <span class="preview-notice__tag">PREVIEW NOTE</span>
        <span>{{ WORLD_PREVIEW_NOTE }}</span>
        <button type="button" aria-label="关闭样稿说明" @click="worldNotice = false">×</button>
      </div>
    </Transition>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DuskStar from '../components/DuskStar.vue'
import { CHARACTERS, RECENT_SESSION_ID, SESSIONS, WORLD_PREVIEW_NOTE } from '../data.js'
import heroDream from '../assets/art/hero-dream.webp'
import worldDream from '../assets/art/world-dream.webp'

const route = useRoute()
const router = useRouter()
const worldNotice = ref(false)
let noticeTimer

const recentSession = computed(
  () => SESSIONS.find((item) => item.id === RECENT_SESSION_ID) || SESSIONS[0]
)

function openChat(sessionId) {
  router.push({ path: '/chat', query: sessionId ? { session: sessionId } : {} })
}

function showWorldNotice() {
  worldNotice.value = true
  clearTimeout(noticeTimer)
  noticeTimer = setTimeout(() => {
    worldNotice.value = false
  }, 5200)
}

function backToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function applySectionQuery() {
  const section = route.query.section
  if (!section) return
  requestAnimationFrame(() => {
    document.getElementById(section)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

watch(() => route.query.section, applySectionQuery)
onMounted(applySectionQuery)
onBeforeUnmount(() => clearTimeout(noticeTimer))
</script>

<style lang="scss" scoped>
.home-view {
  position: relative;
  flex: 1;
  min-width: 0;
  overflow-x: clip;
  color: var(--dream-ink);
}

.home-wrap {
  width: min(1180px, calc(100% - 48px));
  margin: 0 auto;
}

.hero {
  position: relative;
  min-height: min(612px, calc(100dvh - var(--dream-nav-h) - 96px));
  display: flex;
  align-items: center;
  overflow: hidden;
  isolation: isolate;
  background:
    radial-gradient(ellipse 32% 46% at 3% 42%, rgba(207, 150, 166, 0.16), transparent 72%),
    radial-gradient(ellipse 38% 52% at 84% 10%, rgba(143, 185, 208, 0.12), transparent 70%);
}

.hero-art {
  position: absolute;
  z-index: 0;
  top: 0;
  right: 0;
  bottom: 0;
  left: 37%;
  pointer-events: none;
}

.hero-art img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 52% 50%;
  transform: scale(1.04);
  -webkit-mask-image:
    linear-gradient(90deg, transparent 0%, rgba(0, 0, 0, 0.42) 17%, #000 42%),
    linear-gradient(0deg, transparent 0%, rgba(0, 0, 0, 0.62) 13%, #000 30%),
    linear-gradient(180deg, transparent 0%, #000 12%);
  mask-image:
    linear-gradient(90deg, transparent 0%, rgba(0, 0, 0, 0.42) 17%, #000 42%),
    linear-gradient(0deg, transparent 0%, rgba(0, 0, 0, 0.62) 13%, #000 30%),
    linear-gradient(180deg, transparent 0%, #000 12%);
  -webkit-mask-composite: source-in;
  mask-composite: intersect;
}

.hero-art__wash {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 30% 38% at 48% 44%, rgba(255, 243, 235, 0.32), transparent 68%),
    linear-gradient(92deg, rgba(244, 239, 237, 0.34), transparent 32%),
    linear-gradient(0deg, rgba(241, 236, 238, 0.48), transparent 24%);
  pointer-events: none;
}

.hero-art__beam {
  position: absolute;
  top: 4%;
  right: 8%;
  width: 34%;
  height: 76%;
  background: linear-gradient(111deg, transparent 8%, rgba(255, 246, 236, 0.28) 48%, transparent 86%);
  filter: blur(18px);
  transform: rotate(-7deg);
  opacity: 0.82;
}

.hero-orbit {
  position: absolute;
  z-index: 1;
  border: 1px solid rgba(91, 135, 160, 0.2);
  border-radius: 50%;
  pointer-events: none;
}

.hero-orbit--one {
  top: 11%;
  right: -9%;
  width: 56%;
  height: 54%;
  transform: rotate(-13deg);
}

.hero-orbit--two {
  right: 17%;
  bottom: 7%;
  width: 24%;
  height: 13%;
  border-color: rgba(187, 119, 140, 0.22);
  transform: rotate(7deg);
}

.hero-mark {
  position: absolute;
  z-index: 2;
  opacity: 0.72;
  pointer-events: none;
}

.hero-mark--one {
  top: 16%;
  right: 7%;
}

.hero-mark--two {
  right: 39%;
  bottom: 14%;
}

.hero-inner {
  position: relative;
  z-index: 2;
  padding-top: 34px;
  padding-bottom: 32px;
}

.hero-copy {
  min-width: 0;
  max-width: 548px;
}

.hero-script {
  margin: 16px 0 -3px 2px;
  color: var(--dream-rose);
  font-size: clamp(28px, 2.7vw, 38px);
  line-height: 1;
  opacity: 0.9;
}

.hero-title {
  margin-top: 12px;
  color: var(--dream-ink);
  font-size: clamp(40px, 4.4vw, 60px);
  line-height: 1.18;
  letter-spacing: 0.015em;
}

.hero-title__line {
  display: block;
}

.hero-title__line:last-child {
  color: #30344f;
}

.hero-title__dot {
  color: var(--dream-rose);
}

.hero-lede {
  max-width: 438px;
  margin-top: 18px;
  color: var(--dream-ink-2);
  font-size: 14.5px;
  line-height: 1.92;
}

.continue-card {
  position: relative;
  width: min(100%, 486px);
  min-height: 96px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  margin-top: 26px;
  padding: 12px 13px 12px 12px;
  overflow: hidden;
  text-align: left;
  color: var(--dream-ink);
  border: 1px solid rgba(58, 62, 91, 0.16);
  border-radius: 18px;
  background: linear-gradient(118deg, rgba(255, 252, 247, 0.88), rgba(252, 242, 236, 0.67));
  box-shadow:
    var(--dream-shadow-inset),
    0 16px 36px rgba(81, 73, 91, 0.12);
  backdrop-filter: blur(14px);
  transition:
    transform var(--dream-motion) var(--dream-ease),
    border-color var(--dream-motion) var(--dream-ease),
    box-shadow var(--dream-motion) var(--dream-ease);
}

.continue-card::after {
  content: "";
  position: absolute;
  top: 0;
  right: 18px;
  width: 38%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.9));
  pointer-events: none;
}

.continue-card:hover {
  transform: translateY(-2px);
  border-color: rgba(184, 112, 133, 0.32);
  box-shadow:
    var(--dream-shadow-inset),
    0 21px 44px rgba(81, 73, 91, 0.16),
    0 0 0 5px rgba(218, 168, 181, 0.1);
}

.continue-avatar {
  position: relative;
  width: 62px;
  height: 62px;
  flex: none;
  overflow: hidden;
  border: 1px solid rgba(94, 81, 83, 0.22);
  border-radius: 50%;
  background: #e8dfe5;
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.5);
}

.continue-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.continue-avatar i {
  position: absolute;
  right: 2px;
  bottom: 3px;
  width: 8px;
  height: 8px;
  border: 1px solid #fffaf4;
  border-radius: 50%;
  background: var(--dream-success);
}

.continue-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.continue-eyebrow {
  overflow: hidden;
  color: var(--dream-rose);
  font-family: var(--dream-font-annotation);
  font-size: 8.5px;
  font-weight: 600;
  letter-spacing: 0.16em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.continue-title-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.continue-title-row strong {
  overflow: hidden;
  font-family: var(--dream-font-display);
  font-size: 19px;
  font-weight: 600;
  line-height: 1.45;
  letter-spacing: 0.01em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.continue-title-row em {
  flex: none;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-body);
  font-size: 10px;
  font-style: normal;
}

.continue-quote {
  overflow: hidden;
  color: var(--dream-ink-2);
  font-size: 11.5px;
  line-height: 1.6;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.continue-side {
  min-width: 92px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  align-self: stretch;
  gap: 8px;
  padding: 3px 2px 2px 0;
}

.continue-side time {
  color: var(--dream-ink-3);
  font-family: var(--dream-font-annotation);
  font-size: 9px;
  letter-spacing: 0.08em;
}

.continue-cta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  padding: 0 10px;
  border-radius: var(--dream-radius-pill);
  color: var(--dream-ink-inverse);
  font-size: 9.5px;
  font-weight: 500;
  letter-spacing: 0.03em;
  white-space: nowrap;
  background: #303453;
}

.continue-cta b {
  font-family: var(--dream-font-annotation);
  font-size: 12px;
  font-weight: 400;
  line-height: 1;
}

.hero-foot {
  display: flex;
  align-items: center;
  gap: 11px;
  margin-top: 23px;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-annotation);
  font-size: 9px;
  letter-spacing: 0.11em;
  text-transform: uppercase;
}

.hero-foot__line {
  width: 30px;
  height: 1px;
  background: linear-gradient(90deg, rgba(58, 62, 91, 0.26), transparent);
}

.hero-foot__script {
  color: var(--dream-rose);
  font-size: 17px;
  letter-spacing: 0;
  text-transform: none;
}

.art-note {
  position: absolute;
  z-index: 3;
  right: max(24px, calc((100vw - 1180px) / 2));
  bottom: 22px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  color: rgba(48, 52, 79, 0.58);
  font-family: var(--dream-font-annotation);
  font-size: 8.5px;
  letter-spacing: 0.16em;
  text-align: right;
  text-transform: uppercase;
  pointer-events: none;
}

.art-note small {
  color: rgba(48, 52, 79, 0.46);
  font-family: var(--dream-font-body);
  font-size: 8px;
  letter-spacing: 0.08em;
}

.home-body {
  position: relative;
  z-index: 3;
  padding-bottom: 28px;
}

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  padding: 32px 0 16px;
  border-top: 1px solid var(--dream-line-soft);
}

.section-heading h2 {
  margin-top: 5px;
  color: var(--dream-ink);
  font-size: clamp(26px, 2.6vw, 33px);
  line-height: 1.24;
}

.section-heading > p {
  max-width: 270px;
  padding-bottom: 3px;
  color: var(--dream-ink-3);
  font-size: 11.5px;
  line-height: 1.7;
  text-align: right;
}

.characters {
  scroll-margin-top: calc(var(--dream-nav-h) + 18px);
}

.character-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.character-card {
  --character-accent: var(--dream-rose);
  --character-wash: rgba(245, 222, 229, 0.4);
  width: 100%;
  min-width: 0;
  min-height: 122px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 15px 15px 15px 14px;
  overflow: hidden;
  text-align: left;
  color: var(--dream-ink);
  border: 1px solid var(--dream-line-soft);
  border-radius: 18px;
  background:
    radial-gradient(ellipse 44% 78% at 4% 50%, var(--character-wash), transparent 72%),
    linear-gradient(118deg, rgba(255, 252, 247, 0.8), rgba(255, 250, 244, 0.46));
  box-shadow: var(--dream-shadow-inset), 0 10px 26px rgba(78, 71, 90, 0.07);
  transition:
    transform var(--dream-motion) var(--dream-ease),
    border-color var(--dream-motion) var(--dream-ease),
    box-shadow var(--dream-motion) var(--dream-ease);
}

.character-card.is-sun {
  --character-accent: var(--dream-apricot);
  --character-wash: rgba(236, 190, 155, 0.38);
}

.character-card.is-rose {
  --character-accent: var(--dream-rose);
  --character-wash: rgba(233, 193, 205, 0.36);
}

.character-card.is-ice {
  --character-accent: var(--dream-ice);
  --character-wash: rgba(174, 203, 219, 0.34);
}

.character-card.is-violet {
  --character-accent: var(--dream-violet);
  --character-wash: rgba(199, 192, 219, 0.34);
}

.character-card:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--character-accent) 42%, transparent);
  box-shadow: var(--dream-shadow-inset), 0 16px 34px rgba(78, 71, 90, 0.12);
}

.character-portrait {
  position: relative;
  width: 64px;
  height: 64px;
  flex: none;
  overflow: hidden;
  border: 1px solid rgba(72, 69, 88, 0.18);
  border-radius: 50%;
  box-shadow:
    0 0 0 4px rgba(255, 255, 255, 0.46),
    0 8px 20px rgba(74, 67, 88, 0.13);
}

.character-portrait img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.character-tint {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(160deg, transparent 42%, var(--character-wash));
  mix-blend-mode: soft-light;
  pointer-events: none;
}

.character-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.character-name {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.character-name strong {
  color: var(--dream-ink);
  font-family: var(--dream-font-display);
  font-size: 20px;
  font-weight: 600;
  line-height: 1.45;
  letter-spacing: 0.02em;
}

.character-name em {
  overflow: hidden;
  min-width: 0;
  color: var(--character-accent);
  font-size: 13px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.character-identity {
  overflow: hidden;
  color: var(--dream-ink-2);
  font-size: 11px;
  line-height: 1.6;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.character-note {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 3px;
  color: var(--dream-ink-3);
  font-size: 10.5px;
  line-height: 1.58;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.character-arrow {
  align-self: flex-start;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  margin-top: 2px;
  border: 1px solid var(--dream-line-soft);
  border-radius: 50%;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-annotation);
  font-size: 11px;
  transition:
    color var(--dream-motion) var(--dream-ease),
    border-color var(--dream-motion) var(--dream-ease),
    transform var(--dream-motion) var(--dream-ease);
}

.character-card:hover .character-arrow {
  color: var(--character-accent);
  border-color: color-mix(in srgb, var(--character-accent) 42%, transparent);
  transform: translate(2px, -2px);
}

.character-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-annotation);
  font-size: 8.5px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.character-foot__line {
  width: 26px;
  height: 1px;
  background: linear-gradient(90deg, var(--dream-line), transparent);
}

.character-foot .dream-script {
  color: var(--dream-rose);
  font-size: 16px;
  letter-spacing: 0;
  text-transform: none;
}

.world-band {
  position: relative;
  min-height: 226px;
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  margin-top: 32px;
  overflow: hidden;
  border: 1px solid var(--dream-line-soft);
  border-radius: 22px;
  background: linear-gradient(116deg, rgba(255, 251, 244, 0.82), rgba(244, 238, 236, 0.58));
  box-shadow: var(--dream-shadow-inset), 0 16px 38px rgba(79, 72, 91, 0.08);
  scroll-margin-top: calc(var(--dream-nav-h) + 18px);
}

.world-art {
  position: absolute;
  z-index: 0;
  top: 0;
  right: 0;
  bottom: 0;
  left: 48%;
  overflow: hidden;
  pointer-events: none;
}

.world-art img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 62% 50%;
  -webkit-mask-image: linear-gradient(90deg, transparent 0, rgba(0, 0, 0, 0.48) 18%, #000 46%);
  mask-image: linear-gradient(90deg, transparent 0, rgba(0, 0, 0, 0.48) 18%, #000 46%);
}

.world-art__veil {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 52% 60% at 62% 40%, rgba(255, 246, 238, 0.22), transparent 70%),
    linear-gradient(0deg, rgba(238, 231, 235, 0.34), transparent 34%);
  pointer-events: none;
}

.world-orbit {
  position: absolute;
  top: 16%;
  right: 6%;
  width: 260px;
  height: 92px;
  border: 1px solid rgba(96, 137, 160, 0.26);
  border-radius: 50%;
  transform: rotate(-9deg);
}

.world-copy {
  position: relative;
  z-index: 2;
  min-width: 0;
  max-width: 500px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  padding: 26px 28px 24px;
}

.world-copy h2 {
  margin-top: 3px;
  color: var(--dream-ink);
  font-size: clamp(25px, 2.4vw, 31px);
  line-height: 1.24;
}

.world-copy p {
  max-width: 440px;
  color: var(--dream-ink-2);
  font-size: 12.5px;
  line-height: 1.78;
}

.world-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 5px 11px;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-annotation);
  font-size: 8.5px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.world-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.world-meta span:not(:first-child)::before {
  content: "·";
  color: var(--dream-ink-3);
}

.world-meta i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--dream-gold);
  box-shadow: 0 0 8px rgba(183, 157, 112, 0.35);
}

.world-link {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  padding: 0 1px;
  border-bottom: 1px solid rgba(58, 62, 91, 0.24);
  color: var(--dream-ink);
  font-size: 11.5px;
  font-weight: 500;
  letter-spacing: 0.03em;
  transition:
    border-color var(--dream-motion) var(--dream-ease),
    color var(--dream-motion) var(--dream-ease);
}

.world-link span {
  font-family: var(--dream-font-annotation);
  font-size: 13px;
  transition: transform var(--dream-motion) var(--dream-ease);
}

.world-link:hover {
  color: var(--dream-rose);
  border-color: rgba(184, 112, 133, 0.46);
}

.world-link:hover span {
  transform: translateX(3px);
}

.world-stamp {
  position: absolute;
  z-index: 2;
  right: 20px;
  bottom: 16px;
  color: rgba(46, 52, 78, 0.52);
  font-family: var(--dream-font-annotation);
  font-size: 8px;
  letter-spacing: 0.16em;
  pointer-events: none;
}

.home-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-top: 42px;
  padding: 22px 0 16px;
  border-top: 1px solid var(--dream-line-soft);
}

.home-footer__brand {
  display: flex;
  align-items: baseline;
  gap: 11px;
  min-width: 0;
}

.home-footer__brand span {
  color: var(--dream-ink-2);
  font-size: 24px;
  line-height: 1;
}

.home-footer__brand small {
  color: var(--dream-ink-3);
  font-family: var(--dream-font-annotation);
  font-size: 8px;
  letter-spacing: 0.13em;
  text-transform: uppercase;
}

.home-footer p {
  max-width: 440px;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-annotation);
  font-size: 8.5px;
  line-height: 1.7;
  letter-spacing: 0.05em;
  text-align: right;
}

.footer-top {
  min-height: 44px;
  padding: 0 4px;
  border-radius: var(--dream-radius-sm);
  color: var(--dream-ink-3);
  font-size: 11px;
  white-space: nowrap;
}

.footer-top:hover {
  color: var(--dream-ink);
}

.footer-top span {
  margin-left: 5px;
  color: var(--dream-rose);
}

.preview-notice {
  position: fixed;
  z-index: 90;
  right: 50%;
  bottom: 20px;
  width: min(520px, calc(100vw - 24px));
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  transform: translateX(50%);
  border: 1px dashed rgba(199, 121, 144, 0.45);
  border-radius: 12px;
  color: var(--dream-ink-2);
  font-size: 12px;
  line-height: 1.6;
  background: rgba(255, 252, 247, 0.94);
  box-shadow: var(--dream-shadow-raised);
  backdrop-filter: blur(12px);
}

.preview-notice__tag {
  flex: none;
  padding-top: 1px;
  color: var(--dream-rose);
  font-family: var(--dream-font-annotation);
  font-size: 8px;
  letter-spacing: 0.14em;
}

.preview-notice button {
  flex: none;
  width: 44px;
  min-height: 44px;
  margin: -8px -8px -8px 0;
  color: var(--dream-ink-3);
  font-size: 16px;
}

.notice-enter-active,
.notice-leave-active {
  transition: opacity var(--dream-motion) var(--dream-ease), transform var(--dream-motion) var(--dream-ease);
}

.notice-enter-from,
.notice-leave-to {
  opacity: 0;
  transform: translate(50%, 8px);
}

@media (max-width: 1024px) {
  .home-wrap {
    width: calc(100% - 40px);
  }

  .hero-art {
    left: 40%;
  }

  .hero-copy {
    max-width: 500px;
  }

  .character-grid {
    gap: 10px;
  }
}

@media (max-width: 860px) {
  .hero {
    min-height: 0;
    display: block;
    overflow: visible;
  }

  .hero-art {
    position: relative;
    left: auto;
    width: 100%;
    height: 290px;
    margin-top: 0;
    overflow: hidden;
  }

  .hero-art img {
    transform: scale(1.02);
    object-position: 55% 52%;
    -webkit-mask-image:
      linear-gradient(0deg, transparent 0%, #000 24%),
      linear-gradient(90deg, transparent 0%, #000 9%),
      linear-gradient(270deg, transparent 0%, #000 9%);
    mask-image:
      linear-gradient(0deg, transparent 0%, #000 24%),
      linear-gradient(90deg, transparent 0%, #000 9%),
      linear-gradient(270deg, transparent 0%, #000 9%);
    -webkit-mask-composite: source-in;
    mask-composite: intersect;
  }

  .hero-inner {
    padding-top: 26px;
  }

  .hero-copy {
    max-width: 620px;
  }

  .hero-orbit--one {
    top: 4%;
    right: -14%;
    width: 68%;
    height: 200px;
    opacity: 0.7;
  }

  .hero-orbit--two,
  .hero-mark--one {
    display: none;
  }

  .art-note {
    top: 248px;
    right: 20px;
    bottom: auto;
  }

  .section-heading {
    padding-top: 28px;
  }

  .character-grid {
    grid-template-columns: 1fr;
  }

  .character-card {
    min-height: 108px;
  }

  .world-band {
    grid-template-columns: 1fr;
    min-height: 0;
  }

  .world-art {
    position: relative;
    left: auto;
    order: 2;
    height: 184px;
    margin: 0 18px 18px;
    border-radius: 14px;
    overflow: hidden;
  }

  .world-art img {
    -webkit-mask-image: none;
    mask-image: none;
  }

  .world-art__veil {
    background: linear-gradient(0deg, rgba(238, 231, 235, 0.28), transparent 42%);
  }

  .world-copy {
    order: 1;
    max-width: 100%;
    padding: 24px 24px 18px;
  }

  .world-link {
    margin-top: 4px;
  }

  .world-stamp {
    display: none;
  }

  .home-footer {
    flex-wrap: wrap;
  }

  .home-footer p {
    order: 3;
    max-width: none;
    text-align: left;
  }
}

@media (max-width: 640px) {
  .home-wrap {
    width: calc(100% - 30px);
  }

  .hero-art {
    height: 232px;
  }

  .hero-script {
    margin-top: 12px;
    font-size: 29px;
  }

  .hero-title {
    font-size: clamp(33px, 9.4vw, 43px);
  }

  .hero-lede {
    margin-top: 13px;
    font-size: 13.5px;
    line-height: 1.86;
  }

  .continue-card {
    grid-template-columns: 54px minmax(0, 1fr) 34px;
    gap: 11px;
    min-height: 88px;
    margin-top: 20px;
    padding: 11px;
    border-radius: 16px;
  }

  .continue-avatar {
    width: 54px;
    height: 54px;
  }

  .continue-title-row {
    gap: 6px;
  }

  .continue-title-row strong {
    font-size: 17px;
  }

  .continue-title-row em {
    display: none;
  }

  .continue-quote {
    font-size: 10.5px;
  }

  .continue-side {
    min-width: 0;
    align-items: flex-end;
    justify-content: center;
    padding: 0;
  }

  .continue-side time {
    display: none;
  }

  .continue-cta {
    width: 32px;
    min-height: 32px;
    justify-content: center;
    padding: 0;
    font-size: 0;
  }

  .continue-cta b {
    font-size: 13px;
  }

  .hero-foot {
    margin-top: 17px;
    font-size: 8px;
  }

  .hero-foot__script {
    display: none;
  }

  .art-note {
    top: 194px;
    font-size: 7.5px;
  }

  .section-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 3px;
    padding-top: 24px;
  }

  .section-heading > p {
    max-width: 100%;
    text-align: left;
  }

  .character-card {
    grid-template-columns: 56px minmax(0, 1fr) 26px;
    gap: 11px;
    min-height: 96px;
    padding: 12px;
    border-radius: 16px;
  }

  .character-portrait {
    width: 56px;
    height: 56px;
  }

  .character-name strong {
    font-size: 18px;
  }

  .character-name em {
    font-size: 12px;
  }

  .character-note {
    -webkit-line-clamp: 2;
  }

  .character-foot {
    font-size: 8px;
  }

  .character-foot .dream-script {
    display: none;
  }

  .world-band {
    margin-top: 24px;
    border-radius: 18px;
  }

  .world-copy {
    padding: 20px 18px 16px;
  }

  .world-art {
    height: 154px;
    margin: 0 12px 12px;
  }

  .home-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    margin-top: 30px;
  }

  .home-footer p {
    order: 2;
    font-size: 8px;
  }

  .preview-notice {
    bottom: 12px;
    font-size: 11px;
  }
}

@media (max-width: 390px) {
  .home-wrap {
    width: calc(100% - 24px);
  }

  .hero-title {
    font-size: 32px;
  }

  .hero-script {
    font-size: 26px;
  }

  .continue-card {
    width: 100%;
  }

  .character-name em {
    display: none;
  }

  .hero-foot {
    letter-spacing: 0.06em;
  }
}

@media (prefers-reduced-motion: reduce) {
  .continue-card,
  .character-card {
    transition: none;
  }
}

@media (forced-colors: active) {
  .continue-card,
  .character-card,
  .character-portrait,
  .character-arrow,
  .world-band {
    border: 1px solid CanvasText;
  }

  .hero-art,
  .hero-orbit,
  .hero-mark,
  .hero-art__wash,
  .world-art {
    display: none;
  }
}
</style>
