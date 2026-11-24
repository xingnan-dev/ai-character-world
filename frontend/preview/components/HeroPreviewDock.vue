<template>
  <aside class="hero-study" :class="{ 'is-open': open }" aria-label="首页主视觉预览控件">
    <button
      type="button"
      class="hero-study__tab"
      :aria-expanded="open"
      aria-controls="hero-study-panel"
      @click="open = !open"
    >
      <span class="hero-study__led" aria-hidden="true"></span>
      <span class="hero-study__label">主视觉</span>
      <span class="hero-study__tag">{{ currentOption.key }}</span>
      <span class="hero-study__chevron" aria-hidden="true">{{ open ? '−' : '+' }}</span>
    </button>

    <div v-if="open" id="hero-study-panel" class="hero-study__panel">
      <p class="hero-study__note">设计预览控件 · 只切换首页主视觉，导航、文案与功能保持一致</p>

      <div class="hero-study__options" role="group" aria-label="选择首页主视觉方案">
        <button
          v-for="option in options"
          :key="option.value"
          type="button"
          class="hero-study__option"
          :class="{ 'is-active': mode === option.value }"
          :aria-pressed="mode === option.value"
          @click="choose(option.value)"
        >
          <span class="hero-study__option-key">{{ option.key }}</span>
          <span class="hero-study__option-copy">
            <strong>{{ option.label }}</strong>
            <small>{{ option.english }}</small>
          </span>
        </button>
      </div>

      <p v-if="mode === 'b'" class="hero-study__warning">
        B 的人物为程序化剪影占位，尚未达到成品人像质量；本地缺少可用的原创人物图像生成能力。
      </p>
      <p v-else class="hero-study__hint">
        {{ currentOption.hint }}
      </p>

      <div class="hero-study__foot">
        <span>?hero=a / b / v2</span>
        <RouterLink to="/chat">对话页 ↗</RouterLink>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  mode: { type: String, default: 'v2' }
})

const emit = defineEmits(['update:hero'])
const open = ref(false)

const options = [
  {
    value: 'a',
    key: 'A',
    label: '物件拼贴',
    english: 'OBJECT COLLAGE',
    hint: '水晶、蝴蝶结与像素爱心为主体的自由拼贴。'
  },
  {
    value: 'b',
    key: 'B',
    label: '人物剪影',
    english: 'SILHOUETTE',
    hint: '原创虚构人物剪影，漂移叠影与装饰组成的拼贴。'
  },
  {
    value: 'v2',
    key: 'V2',
    label: '浮光卷',
    english: 'ACCEPTED BASE',
    hint: '已确认的第二版，作为回看与对照基线。'
  }
]

const currentOption = computed(
  () => options.find((option) => option.value === props.mode) || options[2]
)

function choose(value) {
  open.value = false
  emit('update:hero', value)
}
</script>

<style lang="scss" scoped>
.hero-study {
  position: absolute;
  z-index: 6;
  top: 16px;
  right: max(24px, calc((100vw - 1180px) / 2));
  width: 176px;
  color: var(--dream-ink-2);
  font-family: ui-monospace, "SFMono-Regular", Consolas, "Liberation Mono", monospace;
}

.hero-study__tab {
  width: 100%;
  min-height: 38px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 8px;
  border: 1px dashed rgba(199, 121, 144, 0.38);
  border-radius: 9px;
  color: var(--dream-ink-2);
  background: rgba(255, 252, 247, 0.76);
  backdrop-filter: blur(12px);
  transition:
    border-color var(--dream-motion) var(--dream-ease),
    background var(--dream-motion) var(--dream-ease);
}

.hero-study__tab:hover {
  border-color: rgba(199, 121, 144, 0.68);
  background: rgba(255, 252, 247, 0.92);
}

.hero-study__led {
  width: 5px;
  height: 5px;
  flex: none;
  border-radius: 50%;
  background: var(--dream-rose);
  box-shadow: 0 0 7px rgba(199, 121, 144, 0.3);
}

.hero-study__label {
  color: var(--dream-ink-3);
  font-family: var(--dream-font-body);
  font-size: 9px;
  letter-spacing: 0.05em;
}

.hero-study__tag {
  color: var(--dream-rose);
  font-size: 8px;
  letter-spacing: 0.12em;
}

.hero-study__chevron {
  margin-left: auto;
  color: var(--dream-ink-3);
  font-size: 12px;
}

.hero-study__panel {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  width: 100%;
  padding: 11px;
  border: 1px dashed rgba(199, 121, 144, 0.38);
  border-radius: 11px;
  background: rgba(255, 252, 247, 0.95);
  box-shadow: 0 18px 40px rgba(73, 66, 84, 0.16);
  backdrop-filter: blur(16px);
}

.hero-study__note {
  margin-bottom: 9px;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-body);
  font-size: 8.5px;
  line-height: 1.65;
}

.hero-study__options {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hero-study__option {
  min-height: 44px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 8px;
  border: 1px solid rgba(58, 62, 91, 0.13);
  border-radius: 7px;
  color: var(--dream-ink-2);
  text-align: left;
  background: rgba(255, 253, 249, 0.58);
  transition:
    border-color var(--dream-motion) var(--dream-ease),
    background var(--dream-motion) var(--dream-ease);
}

.hero-study__option:hover {
  border-color: rgba(85, 143, 168, 0.38);
}

.hero-study__option.is-active {
  border-color: rgba(199, 121, 144, 0.52);
  background: rgba(246, 224, 230, 0.52);
}

.hero-study__option-key {
  width: 27px;
  flex: none;
  color: var(--dream-rose);
  font-family: var(--dream-font-annotation);
  font-size: 14px;
  line-height: 1;
  text-align: center;
}

.hero-study__option-copy {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.hero-study__option-copy strong {
  color: var(--dream-ink);
  font-family: var(--dream-font-body);
  font-size: 10.5px;
  font-weight: 500;
}

.hero-study__option-copy small {
  color: var(--dream-ink-3);
  font-size: 7px;
  letter-spacing: 0.1em;
}

.hero-study__option.is-active .hero-study__option-copy small {
  color: var(--dream-rose);
}

.hero-study__hint,
.hero-study__warning {
  margin-top: 9px;
  color: var(--dream-ink-3);
  font-family: var(--dream-font-body);
  font-size: 8.5px;
  line-height: 1.6;
}

.hero-study__warning {
  color: var(--dream-rose);
}

.hero-study__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 9px;
  padding-top: 8px;
  border-top: 1px dashed rgba(58, 62, 91, 0.14);
  color: var(--dream-ink-3);
  font-family: var(--dream-font-body);
  font-size: 7.5px;
}

.hero-study__foot a {
  color: var(--dream-ink-3);
  font-size: 8.5px;
}

.hero-study__foot a:hover {
  color: var(--dream-ice);
}

@media (max-width: 1180px) {
  .hero-study {
    right: 24px;
  }
}

@media (max-width: 860px) {
  .hero-study {
    top: 12px;
    right: 12px;
    width: 164px;
  }

  .hero-study__panel {
    width: min(286px, calc(100vw - 24px));
    max-height: calc(100dvh - 150px);
    overflow-y: auto;
  }
}

@media (max-width: 640px) {
  .hero-study {
    width: 138px;
  }

  .hero-study__label {
    display: none;
  }

  .hero-study__tag::before {
    content: "HERO ";
  }
}

@media (forced-colors: active) {
  .hero-study__tab,
  .hero-study__panel,
  .hero-study__option {
    border: 1px solid CanvasText;
  }
}
</style>
