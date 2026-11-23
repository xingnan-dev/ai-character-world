<template>
  <header class="preview-nav">
    <div class="preview-nav__inner">
      <RouterLink class="preview-nav__brand" to="/home" aria-label="AIworld 浮光卷，设计样稿首页">
        <span class="preview-nav__mark" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="20" height="20">
            <ellipse cx="12" cy="12" rx="9.5" ry="5.4" transform="rotate(-18 12 12)" fill="none" stroke="currentColor" stroke-width="1.1" />
            <circle cx="12" cy="12" r="2.1" fill="none" stroke="currentColor" stroke-width="1.1" />
            <circle cx="12" cy="12" r="0.9" fill="currentColor" opacity="0.68" />
          </svg>
        </span>
        <span class="preview-nav__wordmark">AIworld</span>
        <span class="preview-nav__edition dream-script" aria-hidden="true">dreamlight</span>
      </RouterLink>

      <nav class="preview-nav__links" aria-label="设计样稿导航">
        <button
          v-for="item in items"
          :key="item.label"
          type="button"
          class="preview-nav__link"
          :class="{ 'is-active': activeKey === item.key }"
          :aria-current="activeKey === item.key ? 'page' : undefined"
          @click="navigate(item)"
        >
          {{ item.label }}
        </button>
      </nav>

      <div class="preview-nav__right">
        <span class="preview-nav__badge">DESIGN PREVIEW</span>
        <span class="preview-nav__user" role="img" :aria-label="`预览用户 ${PREVIEW_USER.nickname}`">
          {{ PREVIEW_USER.nickname.charAt(0) }}
          <i class="preview-nav__user-dot" aria-hidden="true"></i>
        </span>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { PREVIEW_USER } from '../data.js'

const route = useRoute()
const router = useRouter()

const items = [
  { key: 'home', label: '首页', path: '/home' },
  { key: 'characters', label: '角色', path: '/home', section: 'characters' },
  { key: 'world', label: '世界', path: '/home', section: 'world' },
  { key: 'chat', label: '对话', path: '/chat' }
]

const activeKey = computed(() => {
  if (route.path === '/chat') return 'chat'
  if (route.query.section === 'characters') return 'characters'
  if (route.query.section === 'world') return 'world'
  return 'home'
})

function scrollToSection(section) {
  const target = document.getElementById(section)
  if (!target) return
  target.scrollIntoView({ behavior: 'smooth', block: 'start' })
  target.setAttribute('tabindex', '-1')
  target.focus({ preventScroll: true })
}

function navigate(item) {
  if (!item.section) {
    router.push(item.path)
    return
  }
  if (route.path === '/home') {
    scrollToSection(item.section)
    return
  }
  router.push({ path: '/home', query: { section: item.section } })
}
</script>

<style lang="scss" scoped>
.preview-nav {
  position: sticky;
  top: 0;
  z-index: 60;
  flex: none;
  color: var(--dream-ink);
  background: rgba(249, 245, 243, 0.74);
  border-bottom: 1px solid rgba(58, 62, 91, 0.09);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20px) saturate(1.06);
}

.preview-nav__inner {
  width: min(1180px, calc(100% - 48px));
  min-height: var(--dream-nav-h);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(max-content, 1fr) auto minmax(40px, 1fr);
  align-items: center;
  gap: 20px;
}

.preview-nav__brand {
  width: max-content;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  gap: 9px;
  border-radius: var(--dream-radius-sm);
  color: var(--dream-ink);
}

.preview-nav__mark {
  color: var(--dream-rose);
  opacity: 0.86;
}

.preview-nav__wordmark {
  font-family: var(--dream-font-latin);
  font-size: 22px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: 0.02em;
}

.preview-nav__edition {
  margin-left: -2px;
  padding-top: 8px;
  color: var(--dream-rose);
  font-size: 15px;
  line-height: 1;
  opacity: 0.82;
}

.preview-nav__links {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 3px;
  border: 1px solid var(--dream-line-soft);
  border-radius: var(--dream-radius-pill);
  background: rgba(255, 252, 247, 0.42);
}

.preview-nav__link {
  position: relative;
  min-height: 38px;
  padding: 0 14px;
  border-radius: var(--dream-radius-pill);
  color: var(--dream-ink-3);
  font-size: 12.5px;
  font-weight: 400;
  letter-spacing: 0.04em;
  transition:
    color var(--dream-motion) var(--dream-ease),
    background var(--dream-motion) var(--dream-ease);
}

.preview-nav__link::after {
  content: "";
  position: absolute;
  right: 16px;
  bottom: 5px;
  left: 16px;
  height: 1px;
  transform: scaleX(0);
  background: linear-gradient(90deg, var(--dream-rose), var(--dream-ice));
  transition: transform var(--dream-motion) var(--dream-ease);
}

.preview-nav__link:hover {
  color: var(--dream-ink);
  background: rgba(255, 255, 255, 0.38);
}

.preview-nav__link.is-active {
  color: var(--dream-ink);
  font-weight: 500;
  background: rgba(255, 253, 249, 0.76);
}

.preview-nav__link.is-active::after {
  transform: scaleX(1);
}

.preview-nav__right {
  justify-self: end;
  display: flex;
  align-items: center;
  gap: 13px;
}

.preview-nav__badge {
  min-height: 24px;
  display: inline-flex;
  align-items: center;
  padding: 0 9px;
  border: 1px dashed rgba(199, 121, 144, 0.36);
  border-radius: var(--dream-radius-pill);
  color: var(--dream-rose);
  font-family: var(--dream-font-annotation);
  font-size: 8px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: 0.16em;
}

.preview-nav__user {
  position: relative;
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(58, 62, 91, 0.18);
  border-radius: 50%;
  color: var(--dream-ink);
  font-family: var(--dream-font-display);
  font-size: 14px;
  background:
    radial-gradient(circle at 30% 25%, rgba(255, 255, 255, 0.72), transparent 58%),
    linear-gradient(145deg, rgba(216, 231, 238, 0.72), rgba(244, 227, 231, 0.68));
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.44);
}

.preview-nav__user-dot {
  position: absolute;
  right: 0;
  bottom: 1px;
  width: 7px;
  height: 7px;
  border: 1px solid #fbf6f2;
  border-radius: 50%;
  background: var(--dream-success);
}

.preview-nav__brand:focus-visible,
.preview-nav__link:focus-visible {
  outline-offset: 2px;
}

@media (max-width: 900px) {
  .preview-nav__inner {
    width: calc(100% - 32px);
    gap: 12px;
  }

  .preview-nav__badge {
    display: none;
  }
}

@media (max-width: 760px) {
  .preview-nav__inner {
    width: calc(100% - 24px);
    min-height: var(--dream-nav-h);
    grid-template-columns: auto minmax(0, 1fr) auto;
    gap: 8px;
  }

  .preview-nav__brand {
    gap: 6px;
  }

  .preview-nav__mark svg {
    width: 17px;
    height: 17px;
  }

  .preview-nav__wordmark {
    font-size: 18px;
  }

  .preview-nav__edition {
    display: none;
  }

  .preview-nav__links {
    justify-content: center;
    gap: 0;
    padding: 2px;
  }

  .preview-nav__link {
    min-width: 0;
    min-height: 36px;
    padding: 0 8px;
    font-size: 11.5px;
    letter-spacing: 0.01em;
  }

  .preview-nav__link::after {
    right: 9px;
    bottom: 4px;
    left: 9px;
  }

  .preview-nav__user {
    width: 30px;
    height: 30px;
    font-size: 12px;
  }

  .preview-nav__right {
    gap: 0;
  }
}

@media (max-width: 420px) {
  .preview-nav__inner {
    width: calc(100% - 18px);
  }

  .preview-nav__link {
    padding: 0 6px;
    font-size: 11px;
  }
}

@media (forced-colors: active) {
  .preview-nav,
  .preview-nav__user {
    border: 1px solid CanvasText;
  }
}
</style>
