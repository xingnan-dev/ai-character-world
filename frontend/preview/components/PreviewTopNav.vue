<template>
  <header class="preview-nav">
    <div class="preview-nav__inner">
      <RouterLink class="preview-nav__brand" to="/home" aria-label="AIworld 暮色卷，设计样稿首页">
        <span class="preview-nav__mark" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="20" height="20">
            <path d="M12 2.6 14 10 21.4 12 14 14 12 21.4 10 14 2.6 12 10 10Z" fill="none" stroke="currentColor" stroke-width="1.2" />
            <circle cx="12" cy="12" r="1.6" fill="currentColor" />
          </svg>
        </span>
        <span class="preview-nav__wordmark">AIworld</span>
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
  color: var(--dusk-text);
  background: rgba(8, 13, 28, 0.82);
  border-bottom: 1px solid var(--dusk-line-soft);
  backdrop-filter: blur(18px) saturate(1.1);
}

.preview-nav__inner {
  width: min(1180px, calc(100% - 48px));
  min-height: var(--dusk-nav-h);
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
  border-radius: var(--dusk-radius-sm);
  color: var(--dusk-text);
}

.preview-nav__mark {
  color: var(--dusk-rose-soft);
  filter: drop-shadow(0 0 9px var(--dusk-rose-glow));
}

.preview-nav__wordmark {
  font-family: var(--dusk-font-script);
  font-size: 25px;
  font-style: italic;
  line-height: 1;
  letter-spacing: 0.015em;
}

.preview-nav__links {
  display: flex;
  align-items: center;
  gap: 4px;
}

.preview-nav__link {
  position: relative;
  min-height: 44px;
  padding: 0 14px;
  border-radius: var(--dusk-radius-pill);
  color: var(--dusk-text-3);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.04em;
  transition: color var(--dusk-motion) var(--dusk-ease), background var(--dusk-motion) var(--dusk-ease);
}

.preview-nav__link::after {
  content: "";
  position: absolute;
  right: 14px;
  bottom: 7px;
  left: 14px;
  height: 1px;
  transform: scaleX(0);
  background: linear-gradient(90deg, var(--dusk-rose), var(--dusk-ice));
  transition: transform var(--dusk-motion) var(--dusk-ease);
}

.preview-nav__link:hover {
  color: var(--dusk-text);
  background: rgba(232, 182, 199, 0.05);
}

.preview-nav__link.is-active {
  color: var(--dusk-text);
  font-weight: 700;
}

.preview-nav__link.is-active::after {
  transform: scaleX(1);
}

.preview-nav__right {
  justify-self: end;
  display: flex;
  align-items: center;
  gap: 14px;
}

.preview-nav__badge {
  min-height: 24px;
  display: inline-flex;
  align-items: center;
  padding: 0 9px;
  border: 1px dashed rgba(232, 182, 199, 0.32);
  border-radius: var(--dusk-radius-pill);
  color: var(--dusk-rose);
  font-family: var(--dusk-font-annotation);
  font-size: 9px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: 0.16em;
}

.preview-nav__user {
  position: relative;
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(244, 239, 229, 0.34);
  border-radius: 50%;
  color: var(--dusk-text);
  font-family: var(--dusk-font-display);
  font-size: 15px;
  background:
    radial-gradient(circle at 30% 25%, rgba(232, 182, 199, 0.26), transparent 60%),
    rgba(25, 34, 60, 0.82);
  box-shadow: 0 0 0 3px rgba(244, 239, 229, 0.05);
}

.preview-nav__user-dot {
  position: absolute;
  right: 1px;
  bottom: 2px;
  width: 7px;
  height: 7px;
  border: 1px solid var(--dusk-bg-1);
  border-radius: 50%;
  background: var(--dusk-success);
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
    min-height: var(--dusk-nav-h);
    grid-template-columns: auto minmax(0, 1fr) auto;
    gap: 8px;
  }

  .preview-nav__brand {
    gap: 6px;
  }

  .preview-nav__mark svg {
    width: 16px;
    height: 16px;
  }

  .preview-nav__wordmark {
    font-size: 19px;
  }

  .preview-nav__links {
    justify-content: center;
    gap: 0;
  }

  .preview-nav__link {
    min-width: 0;
    padding: 0 7px;
    font-size: 12px;
    letter-spacing: 0.02em;
  }

  .preview-nav__link::after {
    right: 8px;
    bottom: 5px;
    left: 8px;
  }

  .preview-nav__user {
    width: 30px;
    height: 30px;
    font-size: 13px;
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
    padding: 0 5px;
    font-size: 11.5px;
  }
}

@media (forced-colors: active) {
  .preview-nav,
  .preview-nav__user {
    border: 1px solid CanvasText;
  }
}
</style>
