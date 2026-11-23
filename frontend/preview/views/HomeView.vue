<template>
  <main class="home-view" id="main-content">
    <div class="home-wrap">
      <section class="hero" aria-labelledby="hero-title">
        <div class="hero-copy">
          <span class="dusk-kicker">AIWORLD · 暮色卷 / DUSK EDITION</span>
          <p class="hero-script dusk-script" aria-hidden="true">Into the quiet</p>
          <h1 id="hero-title" class="hero-title dusk-display">
            让未说完的故事，<br />
            <span>留在这个夜晚。</span>
          </h1>
          <p class="hero-lede">
            夜班灯还亮着。窗边的人替你把书翻到上次那一页，也把那封没有寄出的信，留在了桌角。
          </p>
          <div class="hero-actions">
            <button type="button" class="dusk-btn dusk-btn--pearl" @click="openChat('snow')">
              继续上次对话
              <span class="dusk-btn__arrow" aria-hidden="true">→</span>
            </button>
            <div class="hero-meta" aria-label="最近一次对话信息">
              <span class="hero-meta__status"><i aria-hidden="true"></i>最近 · 窗边的雪</span>
              <small>与沈知微 · 昨晚 23:41 离开</small>
            </div>
          </div>
          <div class="hero-footnote" aria-hidden="true">
            <span>36.06° N</span>
            <span class="hero-footnote__line"></span>
            <span>夜色正在变蓝</span>
          </div>
        </div>

        <div class="hero-scene">
          <span class="scene-halo" aria-hidden="true"></span>
          <span class="scene-orbit" aria-hidden="true"></span>
          <div class="scene-frame">
            <SceneWindow />
          </div>
          <div class="scene-label scene-label--top" aria-hidden="true">
            <span>NIGHT WINDOW</span><b>N°01</b>
          </div>
          <div class="scene-label scene-label--side" aria-hidden="true">
            <span>21:47</span><small>城市刚亮起灯</small>
          </div>
          <DuskStar class="scene-star scene-star--one" :size="22" tone="rose" />
          <DuskStar class="scene-star scene-star--two" :size="14" tone="ice" :spark="false" />
          <div class="scene-caption" aria-hidden="true">
            <span class="scene-caption__tick"></span>
            <span>window study · dusk</span>
          </div>
        </div>
      </section>

      <section class="continue-section" aria-labelledby="continue-title">
        <div class="section-line">
          <span class="dusk-kicker">CONTINUE / 01</span>
          <span class="section-line__count">最近一次留在这里</span>
        </div>
        <button
          type="button"
          class="continue-card"
          aria-labelledby="continue-title"
          @click="openChat('snow')"
        >
          <span class="continue-card__glow" aria-hidden="true"></span>
          <span class="continue-portrait" aria-hidden="true">
            <img src="/portraits/shen-zhiwei.svg" alt="" />
            <DuskStar :size="16" tone="rose" />
          </span>
          <span class="continue-copy">
            <span class="continue-eyebrow">上次对话 · THE LAST PAGE</span>
            <strong id="continue-title" class="continue-title dusk-display">窗边的雪</strong>
            <span class="continue-person">
              沈知微
              <i aria-hidden="true"></i>
              旧书店的夜班编辑
            </span>
            <span class="continue-snippet">“它还没有写完，所以我没有把那页收进档案。”</span>
          </span>
          <span class="continue-side">
            <span class="continue-stamps">
              <span>第 12 次谈话</span>
              <span>23:41</span>
            </span>
            <span class="continue-go" aria-hidden="true">
              <span>继续</span>
              <b>→</b>
            </span>
          </span>
        </button>
      </section>

      <div class="lower-grid">
        <section class="characters" id="characters" aria-labelledby="characters-title">
          <header class="section-heading">
            <div>
              <span class="dusk-kicker">CHARACTERS / 02</span>
              <h2 id="characters-title" class="dusk-display">认识角色</h2>
            </div>
            <p>他们各自记得，上一次谈话停在哪里。</p>
          </header>

          <ul class="character-list">
            <li v-for="character in CHARACTERS" :key="character.id">
              <button
                type="button"
                class="character-row"
                :aria-label="`打开与 ${character.name} 的对话`"
                @click="openChat(character.sessionId)"
              >
                <span class="character-portrait" :class="`is-${character.accent}`" aria-hidden="true">
                  <img :src="character.portrait" alt="" loading="lazy" />
                </span>
                <span class="character-copy">
                  <span class="character-name">
                    <strong>{{ character.name }}</strong>
                    <em>{{ character.englishName }}</em>
                  </span>
                  <span class="character-identity">{{ character.identity }}</span>
                  <span class="character-note">{{ character.lastNote }}</span>
                </span>
                <span class="character-arrow" aria-hidden="true">↗</span>
              </button>
            </li>
          </ul>
          <span class="character-count" aria-hidden="true">03 CHARACTERS IN THIS ROUND</span>
        </section>

        <section class="world-panel" id="world" aria-labelledby="world-title">
          <div class="world-art" aria-hidden="true">
            <div class="world-window">
              <span class="world-moon"></span>
              <DuskStar class="world-star world-star--one" :size="16" tone="pearl" />
              <DuskStar class="world-star world-star--two" :size="11" tone="rose" :spark="false" />
              <DuskStar class="world-star world-star--three" :size="9" tone="ice" :spark="false" />
              <div class="world-letter">
                <span class="world-letter__eyebrow">AMBER POST · NIGHT MAIL</span>
                <span class="world-letter__line"></span>
                <span class="world-letter__line is-short"></span>
                <span class="world-letter__line"></span>
                <span class="world-seal">S</span>
              </div>
              <span class="world-window__sill"></span>
            </div>
            <span class="world-art__caption">archive window / letter N°03</span>
          </div>
          <div class="world-copy">
            <span class="dusk-kicker">WORLDS / 03</span>
            <h2 id="world-title" class="dusk-display">进入世界</h2>
            <p>琥珀邮局｜把一封信放进夜里，等它抵达另一扇窗。</p>
            <div class="world-meta">
              <span><i aria-hidden="true"></i>夜航进行中</span>
              <span>3 位角色</span>
              <span>第 3 封信</span>
            </div>
            <button type="button" class="dusk-btn dusk-btn--ghost" @click="showWorldNotice">
              进入世界
              <span class="dusk-btn__arrow" aria-hidden="true">→</span>
            </button>
          </div>
        </section>
      </div>

      <footer class="home-footer">
        <div class="home-footer__brand">
          <span class="dusk-script">AIworld</span>
          <small>暮色卷 · a quieter place for stories</small>
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
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneWindow from '../components/SceneWindow.vue'
import DuskStar from '../components/DuskStar.vue'
import { CHARACTERS, WORLD_PREVIEW_NOTE } from '../data.js'

const route = useRoute()
const router = useRouter()
const worldNotice = ref(false)
let noticeTimer

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
  flex: 1;
  min-width: 0;
  overflow-x: clip;
  color: var(--dusk-text);
}

.home-wrap {
  width: min(1180px, calc(100% - 48px));
  margin: 0 auto;
  padding-bottom: 28px;
}

.hero {
  min-width: 0;
  min-height: min(560px, calc(100dvh - var(--dusk-nav-h) - 84px));
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  align-items: center;
  gap: clamp(32px, 5vw, 80px);
  padding: 44px 0 38px;
}

.hero-copy {
  position: relative;
  z-index: 2;
  min-width: 0;
  max-width: 540px;
}

.hero-script {
  margin: 18px 0 4px;
  color: var(--dusk-rose-soft);
  font-size: clamp(42px, 4.4vw, 62px);
  line-height: 1;
  text-shadow: 0 0 34px rgba(238, 157, 184, 0.18);
}

.hero-title {
  margin: 12px 0 0;
  color: var(--dusk-text);
  font-size: clamp(38px, 4.2vw, 62px);
  line-height: 1.2;
}

.hero-title span {
  color: var(--dusk-pearl);
  text-shadow: 0 0 42px rgba(184, 216, 232, 0.1);
}

.hero-lede {
  max-width: 440px;
  margin-top: 24px;
  color: var(--dusk-text-2);
  font-size: 15px;
  line-height: 1.95;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 20px;
  margin-top: 30px;
}

.hero-meta {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding-left: 16px;
  border-left: 1px solid var(--dusk-line);
}

.hero-meta__status {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--dusk-text-2);
  font-size: 12px;
  letter-spacing: 0.03em;
}

.hero-meta__status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--dusk-rose);
  box-shadow: 0 0 10px rgba(232, 182, 199, 0.66);
}

.hero-meta small {
  color: var(--dusk-text-3);
  font-size: 11px;
}

.hero-footnote {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 38px;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 9px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-footnote__line {
  width: 40px;
  height: 1px;
  background: linear-gradient(90deg, var(--dusk-line-bright), transparent);
}

.hero-scene {
  position: relative;
  z-index: 1;
  min-width: 0;
  justify-self: end;
  width: min(100%, 570px);
}

.scene-halo {
  position: absolute;
  z-index: -1;
  top: 4%;
  right: -6%;
  width: 70%;
  height: 74%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(238, 157, 184, 0.22), transparent 68%);
  filter: blur(24px);
  pointer-events: none;
}

.scene-orbit {
  position: absolute;
  z-index: 3;
  inset: 8% -5% 6% 8%;
  border: 1px solid rgba(232, 196, 207, 0.13);
  border-radius: 50%;
  transform: rotate(-14deg);
  pointer-events: none;
}

.scene-frame {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(235, 230, 240, 0.16);
  border-radius: 28px;
  box-shadow:
    0 34px 90px rgba(2, 6, 16, 0.44),
    0 0 0 8px rgba(244, 239, 229, 0.025);
}

.scene-frame::after {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  background: linear-gradient(150deg, rgba(255, 255, 255, 0.05), transparent 30% 70%, rgba(238, 157, 184, 0.05));
}

.scene-label {
  position: absolute;
  z-index: 5;
  display: flex;
  align-items: baseline;
  gap: 8px;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 9px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  pointer-events: none;
}

.scene-label b {
  color: var(--dusk-rose);
  font-family: var(--dusk-font-display);
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.08em;
}

.scene-label--top {
  top: -32px;
  left: 4px;
}

.scene-label--top::before {
  content: "";
  width: 24px;
  height: 1px;
  margin-right: 2px;
  background: var(--dusk-line-bright);
}

.scene-label--side {
  right: 4px;
  bottom: -29px;
  flex-direction: row;
  align-items: baseline;
  gap: 10px;
  text-align: right;
  letter-spacing: 0.08em;
}

.scene-label--side span {
  color: var(--dusk-text-2);
  font-family: var(--dusk-font-display);
  font-size: 16px;
  letter-spacing: 0.04em;
}

.scene-label--side small {
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-body);
  font-size: 9px;
  letter-spacing: 0.1em;
}

.scene-star {
  position: absolute;
  z-index: 5;
  opacity: 0.7;
  pointer-events: none;
}

.scene-star--one {
  top: 12%;
  left: -30px;
}

.scene-star--two {
  right: 6%;
  bottom: -18px;
}

.scene-caption {
  position: absolute;
  z-index: 5;
  bottom: -28px;
  left: 18px;
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 9px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  pointer-events: none;
}

.scene-caption__tick {
  width: 5px;
  height: 5px;
  border: 1px solid var(--dusk-line-bright);
  transform: rotate(45deg);
}

.section-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding-top: 24px;
  border-top: 1px solid var(--dusk-line-soft);
}

.section-line__count {
  color: var(--dusk-text-3);
  font-size: 11px;
  letter-spacing: 0.04em;
}

.continue-section {
  scroll-margin-top: calc(var(--dusk-nav-h) + 20px);
}

.continue-card {
  position: relative;
  width: 100%;
  min-width: 0;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: clamp(16px, 2.8vw, 30px);
  margin-top: 14px;
  padding: 24px 26px;
  overflow: hidden;
  text-align: left;
  color: var(--dusk-text);
  border: 1px solid var(--dusk-line);
  border-radius: var(--dusk-radius-lg);
  background:
    linear-gradient(112deg, rgba(25, 35, 64, 0.9), rgba(14, 22, 43, 0.86) 62%, rgba(19, 27, 51, 0.9));
  box-shadow: var(--dusk-shadow-soft);
  transition:
    border-color var(--dusk-motion) var(--dusk-ease),
    transform var(--dusk-motion) var(--dusk-ease),
    box-shadow var(--dusk-motion) var(--dusk-ease);
}

.continue-card:hover {
  transform: translateY(-2px);
  border-color: rgba(232, 182, 199, 0.34);
  box-shadow: var(--dusk-shadow-soft), 0 0 46px rgba(238, 157, 184, 0.08);
}

.continue-card__glow {
  position: absolute;
  top: -60%;
  left: 4%;
  width: 240px;
  height: 220%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(238, 157, 184, 0.15), transparent 66%);
  pointer-events: none;
}

.continue-card::after {
  content: "";
  position: absolute;
  top: 0;
  right: 26px;
  width: 34%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(244, 239, 229, 0.4));
  pointer-events: none;
}

.continue-portrait {
  position: relative;
  width: 94px;
  height: 94px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(244, 239, 229, 0.34);
  border-radius: 50%;
  background: #111a32;
  box-shadow:
    0 0 0 5px rgba(244, 239, 229, 0.04),
    0 16px 38px rgba(2, 6, 16, 0.38);
}

.continue-portrait img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.continue-portrait .dusk-star {
  position: absolute;
  top: -9px;
  right: -7px;
  filter: drop-shadow(0 0 8px rgba(232, 182, 199, 0.4));
}

.continue-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.continue-eyebrow {
  overflow: hidden;
  color: var(--dusk-rose);
  font-family: var(--dusk-font-annotation);
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.18em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.continue-title {
  color: var(--dusk-text);
  font-size: clamp(24px, 2.4vw, 30px);
  line-height: 1.2;
}

.continue-person {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--dusk-text-2);
  font-size: 12px;
  letter-spacing: 0.03em;
}

.continue-person i {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--dusk-text-3);
}

.continue-snippet {
  max-width: 640px;
  margin-top: 3px;
  overflow: hidden;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-display);
  font-size: 14px;
  line-height: 1.7;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.continue-side {
  min-width: 104px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 16px;
}

.continue-stamps {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 9px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.continue-stamps span:last-child {
  color: var(--dusk-text-2);
  font-family: var(--dusk-font-display);
  font-size: 13px;
  letter-spacing: 0.04em;
}

.continue-go {
  width: 58px;
  height: 58px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  border: 1px solid rgba(232, 182, 199, 0.32);
  border-radius: 50%;
  color: var(--dusk-rose-soft);
  background: rgba(232, 182, 199, 0.04);
  transition:
    transform var(--dusk-motion) var(--dusk-ease),
    background var(--dusk-motion) var(--dusk-ease);
}

.continue-go span {
  font-size: 9px;
  letter-spacing: 0.12em;
}

.continue-go b {
  font-family: var(--dusk-font-annotation);
  font-size: 17px;
  font-weight: 400;
  line-height: 1;
}

.continue-card:hover .continue-go {
  transform: translateX(2px);
  background: rgba(232, 182, 199, 0.1);
}

.lower-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(0, 0.82fr);
  gap: clamp(24px, 4vw, 52px);
  margin-top: 46px;
}

.characters,
.world-panel {
  min-width: 0;
  scroll-margin-top: calc(var(--dusk-nav-h) + 20px);
}

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--dusk-line-soft);
}

.section-heading h2,
.world-copy h2 {
  margin-top: 5px;
  color: var(--dusk-text);
  font-size: clamp(25px, 2.5vw, 32px);
  line-height: 1.25;
}

.section-heading > p {
  max-width: 260px;
  color: var(--dusk-text-3);
  font-size: 12px;
  line-height: 1.7;
  text-align: right;
}

.character-list {
  display: flex;
  flex-direction: column;
}

.character-list li {
  border-bottom: 1px solid var(--dusk-line-soft);
}

.character-row {
  width: 100%;
  min-height: 92px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  padding: 14px 6px;
  text-align: left;
  color: var(--dusk-text);
  border-radius: var(--dusk-radius-sm);
  transition: background var(--dusk-motion) var(--dusk-ease);
}

.character-row:hover {
  background: linear-gradient(90deg, rgba(232, 182, 199, 0.055), transparent 78%);
}

.character-portrait {
  position: relative;
  width: 58px;
  height: 58px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(244, 239, 229, 0.24);
  border-radius: 50%;
  background: #111a30;
  box-shadow: 0 8px 24px rgba(2, 6, 16, 0.28);
}

.character-portrait::after {
  content: "";
  position: absolute;
  inset: -7px;
  border-radius: 50%;
  opacity: 0.5;
  pointer-events: none;
}

.character-portrait.is-rose::after {
  box-shadow: 0 0 26px rgba(238, 157, 184, 0.34);
}

.character-portrait.is-ice::after {
  box-shadow: 0 0 26px rgba(184, 216, 232, 0.22);
}

.character-portrait.is-gold::after {
  box-shadow: 0 0 26px rgba(214, 194, 154, 0.22);
}

.character-portrait img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.character-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.character-name {
  display: flex;
  align-items: baseline;
  gap: 9px;
  min-width: 0;
}

.character-name strong {
  color: var(--dusk-text);
  font-family: var(--dusk-font-display);
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.character-name em {
  overflow: hidden;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-script);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.character-identity {
  color: var(--dusk-text-2);
  font-size: 12px;
}

.character-note {
  overflow: hidden;
  color: var(--dusk-text-3);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.character-arrow {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border: 1px solid var(--dusk-line);
  border-radius: 50%;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 13px;
  transition:
    color var(--dusk-motion) var(--dusk-ease),
    border-color var(--dusk-motion) var(--dusk-ease),
    transform var(--dusk-motion) var(--dusk-ease);
}

.character-row:hover .character-arrow {
  color: var(--dusk-rose);
  border-color: rgba(232, 182, 199, 0.38);
  transform: translate(2px, -2px);
}

.character-count {
  display: block;
  margin-top: 15px;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 9px;
  letter-spacing: 0.16em;
}

.world-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--dusk-line);
  border-radius: var(--dusk-radius-lg);
  background: linear-gradient(155deg, rgba(25, 35, 64, 0.86), rgba(12, 19, 38, 0.9));
  box-shadow: var(--dusk-shadow-soft);
}

.world-art {
  position: relative;
  flex: none;
  padding: 20px 20px 0;
}

.world-window {
  position: relative;
  height: 168px;
  overflow: hidden;
  border: 1px solid rgba(244, 239, 229, 0.2);
  border-radius: 100px 100px 12px 12px;
  background:
    radial-gradient(circle at 68% 28%, rgba(255, 238, 231, 0.2), transparent 16%),
    linear-gradient(180deg, #243057 0%, #4f5079 48%, #8c6f8b 78%, #b48c9c 100%);
}

.world-window::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(110deg, rgba(255, 255, 255, 0.09), transparent 34% 66%, rgba(238, 157, 184, 0.1));
}

.world-window__sill {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 9px;
  background: linear-gradient(180deg, #3a405f, #222943);
  box-shadow: 0 1px 0 rgba(244, 239, 229, 0.2) inset;
}

.world-moon {
  position: absolute;
  top: 24px;
  right: 38px;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 32%, #fff9ed, #e4ccc8);
  box-shadow: 0 0 30px rgba(255, 239, 224, 0.34);
}

.world-star {
  position: absolute;
  opacity: 0.55;
}

.world-star--one {
  top: 32px;
  left: 46px;
}

.world-star--two {
  top: 78px;
  left: 72px;
}

.world-star--three {
  top: 58px;
  right: 88px;
}

.world-letter {
  position: absolute;
  z-index: 2;
  left: 50%;
  bottom: 18px;
  width: 168px;
  height: 98px;
  padding: 14px 15px 12px;
  transform: translateX(-50%) rotate(-5deg);
  border-radius: 3px;
  color: var(--dusk-paper-ink);
  background:
    repeating-linear-gradient(0deg, transparent 0 16px, rgba(78, 68, 63, 0.08) 16px 16.6px),
    linear-gradient(145deg, #f5efe3, #ded6c9);
  box-shadow: 0 16px 34px rgba(1, 4, 12, 0.38);
}

.world-letter::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 0;
  height: 0;
  border-top: 22px solid #bfb4a6;
  border-right: 22px solid transparent;
  opacity: 0.5;
}

.world-letter__eyebrow {
  display: block;
  color: #7f705f;
  font-family: var(--dusk-font-annotation);
  font-size: 7.5px;
  letter-spacing: 0.13em;
}

.world-letter__line {
  display: block;
  width: 100%;
  height: 1px;
  margin-top: 10px;
  background: rgba(91, 78, 71, 0.28);
}

.world-letter__line.is-short {
  width: 72%;
  margin-top: 9px;
}

.world-letter__line:last-of-type {
  margin-top: 9px;
  width: 54%;
}

.world-seal {
  position: absolute;
  right: 12px;
  bottom: 10px;
  width: 25px;
  height: 25px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #f1e3cf;
  font-family: var(--dusk-font-display);
  font-size: 11px;
  background: radial-gradient(circle at 35% 30%, #a96f70, #6f424f);
  box-shadow: 0 3px 8px rgba(60, 26, 38, 0.32);
}

.world-art__caption {
  display: block;
  margin-top: 9px;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 8px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.world-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  padding: 20px 22px 22px;
}

.world-copy p {
  max-width: 340px;
  color: var(--dusk-text-2);
  font-size: 13px;
  line-height: 1.8;
}

.world-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 9px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.world-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.world-meta span:not(:first-child)::before {
  content: "·";
  color: var(--dusk-text-3);
}

.world-meta i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--dusk-gold);
  box-shadow: 0 0 9px rgba(214, 194, 154, 0.4);
}

.world-copy .dusk-btn {
  margin-top: auto;
}

.home-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-top: 54px;
  padding: 22px 0 18px;
  border-top: 1px solid var(--dusk-line-soft);
}

.home-footer__brand {
  display: flex;
  align-items: baseline;
  gap: 12px;
  min-width: 0;
}

.home-footer__brand span {
  font-size: 24px;
  font-style: italic;
  line-height: 1;
  color: var(--dusk-text-2);
}

.home-footer__brand small {
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 8px;
  letter-spacing: 0.13em;
  text-transform: uppercase;
}

.home-footer p {
  max-width: 440px;
  color: var(--dusk-text-3);
  font-family: var(--dusk-font-annotation);
  font-size: 9px;
  line-height: 1.7;
  letter-spacing: 0.06em;
  text-align: right;
}

.footer-top {
  min-height: 44px;
  padding: 0 4px;
  border-radius: var(--dusk-radius-sm);
  color: var(--dusk-text-3);
  font-size: 11px;
  white-space: nowrap;
}

.footer-top:hover {
  color: var(--dusk-text);
}

.footer-top span {
  margin-left: 5px;
  color: var(--dusk-rose);
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
  border: 1px dashed rgba(232, 182, 199, 0.42);
  border-radius: 12px;
  color: var(--dusk-text-2);
  font-size: 12px;
  line-height: 1.6;
  background: rgba(10, 15, 31, 0.94);
  box-shadow: 0 16px 48px rgba(1, 4, 12, 0.5);
  backdrop-filter: blur(12px);
}

.preview-notice__tag {
  flex: none;
  padding-top: 1px;
  color: var(--dusk-rose);
  font-family: var(--dusk-font-annotation);
  font-size: 8px;
  letter-spacing: 0.14em;
}

.preview-notice button {
  flex: none;
  width: 44px;
  min-height: 44px;
  margin: -8px -8px -8px 0;
  color: var(--dusk-text-3);
  font-size: 16px;
}

.notice-enter-active,
.notice-leave-active {
  transition: opacity var(--dusk-motion) var(--dusk-ease), transform var(--dusk-motion) var(--dusk-ease);
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

  .hero {
    gap: 32px;
  }

  .lower-grid {
    grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
    gap: 28px;
  }

  .continue-snippet {
    max-width: 460px;
  }
}

@media (max-width: 860px) {
  .hero {
    min-height: 0;
    grid-template-columns: 1fr;
    gap: 40px;
    padding: 42px 0 34px;
  }

  .hero-copy {
    max-width: 620px;
  }

  .hero-scene {
    justify-self: center;
    width: min(100%, 600px);
  }

  .lower-grid {
    grid-template-columns: 1fr;
    gap: 40px;
  }

  .world-panel {
    max-width: 620px;
  }

  .section-heading > p {
    max-width: 230px;
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
    width: calc(100% - 32px);
  }

  .hero {
    padding: 30px 0 26px;
    gap: 34px;
  }

  .hero-script {
    font-size: clamp(38px, 11vw, 52px);
    margin-top: 16px;
  }

  .hero-title {
    font-size: clamp(31px, 8.6vw, 42px);
  }

  .hero-lede {
    margin-top: 18px;
    font-size: 14px;
    line-height: 1.85;
  }

  .hero-actions {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
    margin-top: 24px;
  }

  .hero-actions .dusk-btn {
    width: 100%;
  }

  .hero-meta {
    padding-left: 0;
    border-left: 0;
  }

  .hero-footnote {
    margin-top: 26px;
  }

  .hero-scene {
    width: 100%;
  }

  .scene-label--top {
    top: -26px;
  }

  .scene-label--side {
    bottom: -27px;
    right: 2px;
  }

  .scene-caption {
    display: none;
  }

  .scene-star--one {
    left: -14px;
  }

  .section-line__count {
    display: none;
  }

  .continue-card {
    grid-template-columns: 66px minmax(0, 1fr);
    gap: 14px;
    padding: 18px;
  }

  .continue-portrait {
    width: 66px;
    height: 66px;
  }

  .continue-title {
    font-size: 23px;
  }

  .continue-snippet {
    max-width: 100%;
    font-size: 13px;
    line-height: 1.65;
    white-space: normal;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  .continue-side {
    grid-column: 1 / -1;
    min-width: 0;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .continue-stamps {
    flex-direction: row;
    gap: 12px;
  }

  .continue-go {
    width: 48px;
    height: 48px;
  }

  .lower-grid {
    margin-top: 36px;
    gap: 34px;
  }

  .section-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 4px;
  }

  .section-heading > p {
    max-width: 100%;
    text-align: left;
  }

  .character-row {
    min-height: 84px;
    gap: 12px;
    padding: 12px 2px;
  }

  .character-portrait {
    width: 52px;
    height: 52px;
  }

  .character-name strong {
    font-size: 18px;
  }

  .character-identity,
  .character-note {
    font-size: 11px;
  }

  .character-arrow {
    width: 34px;
    height: 34px;
  }

  .world-copy h2 {
    font-size: 25px;
  }

  .world-window {
    height: 152px;
  }

  .home-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
    margin-top: 38px;
  }

  .home-footer p {
    order: 2;
    font-size: 8.5px;
  }

  .home-footer__brand {
    flex-wrap: wrap;
    gap: 8px;
  }

  .preview-notice {
    bottom: 12px;
    font-size: 11px;
  }
}

@media (max-width: 390px) {
  .hero-title {
    font-size: 31px;
  }

  .hero-script {
    font-size: 39px;
  }

  .world-letter {
    width: 152px;
    height: 92px;
  }

  .character-name em {
    display: none;
  }
}

@media (forced-colors: active) {
  .continue-card,
  .character-portrait,
  .world-panel,
  .character-arrow {
    border: 1px solid CanvasText;
  }

  .scene-orbit,
  .scene-halo,
  .continue-card__glow {
    display: none;
  }
}
</style>
