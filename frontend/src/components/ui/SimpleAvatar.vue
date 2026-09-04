<template>
  <div
    class="simple-avatar"
    :class="[`simple-avatar--${shape}`, { 'has-status': normalizedStatus }]"
    :style="avatarStyle"
    role="img"
    :aria-label="accessibleLabel"
  >
    <img v-if="displayImage" :src="displayImage" alt="" @error="handleImageError" />
    <span v-if="!displayImage" class="simple-avatar__person" aria-hidden="true">
      <span class="simple-avatar__body"></span>
      <span class="simple-avatar__neck"></span>
      <span class="simple-avatar__head">
        <span class="simple-avatar__hair"></span>
        <span class="simple-avatar__ear simple-avatar__ear--left"></span>
        <span class="simple-avatar__ear simple-avatar__ear--right"></span>
        <span class="simple-avatar__eye simple-avatar__eye--left"></span>
        <span class="simple-avatar__eye simple-avatar__eye--right"></span>
        <span class="simple-avatar__mouth"></span>
      </span>
      <span class="simple-avatar__initial">{{ initial }}</span>
    </span>
    <span v-if="normalizedType" class="simple-avatar__type">{{ normalizedType }}</span>
    <span v-if="normalizedStatus" class="simple-avatar__status" :class="`is-${normalizedStatus}`">
      <span class="simple-avatar__status-dot" aria-hidden="true"></span>{{ statusText }}
    </span>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { getAvatarInitial, resolveAvatarColor, safeAvatarImageUrl, stableAvatarAppearance } from '../../utils/simpleAvatar'

const props = defineProps({
  name: { type: String, default: '' },
  imageUrl: { type: String, default: '' },
  avatarColor: { type: String, default: '' },
  entityKey: { type: [String, Number], default: '' },
  type: { type: String, default: '' },
  status: { type: String, default: '' },
  size: { type: [String, Number], default: 'md' },
  shape: { type: String, default: 'rounded', validator: value => ['circle', 'rounded'].includes(value) }
})

const failedImage = ref('')
const safeImage = computed(() => safeAvatarImageUrl(props.imageUrl))
const displayImage = computed(() => safeImage.value && failedImage.value !== safeImage.value ? safeImage.value : '')
const initial = computed(() => getAvatarInitial(props.name))
const normalizedType = computed(() => ['AI', 'USER'].includes(props.type?.toUpperCase()) ? props.type.toUpperCase() : '')
const normalizedStatus = computed(() => ['online', 'responding', 'failed', 'offline'].includes(props.status) ? props.status : '')
const statusLabels = { online: '在线', responding: '回复中', failed: '异常', offline: '离线' }
const sizeMap = { xs: 36, sm: 48, md: 64, lg: 88, xl: 112 }
const pixelSize = computed(() => typeof props.size === 'number' ? Math.max(28, Math.min(props.size, 160)) : sizeMap[props.size] || sizeMap.md)
const appearance = computed(() => stableAvatarAppearance(props.entityKey, props.name))
const avatarStyle = computed(() => ({
  '--simple-avatar-size': `${pixelSize.value}px`,
  '--simple-avatar-color': resolveAvatarColor(props.avatarColor, props.entityKey, props.name),
  '--simple-avatar-hair': appearance.value.hair,
  '--simple-avatar-skin': appearance.value.skin,
  '--simple-avatar-clothes': appearance.value.clothes,
  '--simple-avatar-hair-style': appearance.value.hairStyle,
  '--simple-avatar-eye-scale': appearance.value.eyeStyle ? 1 : .75
}))
const statusText = computed(() => statusLabels[normalizedStatus.value] || '')
const accessibleLabel = computed(() => [props.name?.trim() || '未命名角色', normalizedType.value && `${normalizedType.value}角色`, statusText.value].filter(Boolean).join('，'))

watch(safeImage, () => { failedImage.value = '' })
function handleImageError() { failedImage.value = safeImage.value }
</script>

<style lang="scss" scoped>
.simple-avatar{position:relative;width:var(--simple-avatar-size);height:var(--simple-avatar-size);display:grid;flex:0 0 auto;place-items:center;overflow:visible;border:3px solid rgba(255,255,255,.92);color:#17323e;background:linear-gradient(145deg,rgba(255,255,255,.64),transparent 58%),var(--simple-avatar-color);box-shadow:var(--app-shadow-sm)}
.simple-avatar--circle{border-radius:50%}.simple-avatar--rounded{border-radius:28%}
.simple-avatar img{width:100%;height:100%;border-radius:inherit;object-fit:cover}.simple-avatar__person{position:absolute;inset:0;overflow:hidden;border-radius:inherit}.simple-avatar__body{position:absolute;left:13%;right:13%;bottom:-22%;height:48%;border-radius:50% 50% 18% 18%;background:var(--simple-avatar-clothes)}.simple-avatar__neck{position:absolute;z-index:1;left:43%;bottom:26%;width:14%;height:17%;border-radius:30%;background:var(--simple-avatar-skin)}.simple-avatar__head{position:absolute;z-index:2;left:26%;top:17%;width:48%;height:51%;border-radius:46% 46% 42% 42%;background:var(--simple-avatar-skin);box-shadow:inset 0 -2px 0 rgba(116,64,42,.08)}.simple-avatar__hair{position:absolute;z-index:2;left:-5%;top:-8%;width:110%;height:42%;border-radius:55% 52% 35% 28%;background:var(--simple-avatar-hair);transform:skewX(calc((var(--simple-avatar-hair-style) - 1) * 5deg))}.simple-avatar__hair::after{content:'';position:absolute;right:5%;top:35%;width:22%;height:55%;border-radius:0 0 60% 60%;background:inherit}.simple-avatar__ear{position:absolute;top:43%;width:14%;height:19%;border-radius:50%;background:var(--simple-avatar-skin)}.simple-avatar__ear--left{left:-9%}.simple-avatar__ear--right{right:-9%}.simple-avatar__eye{position:absolute;top:51%;width:8%;height:8%;border-radius:50%;background:#26343a;transform:scaleY(var(--simple-avatar-eye-scale))}.simple-avatar__eye--left{left:25%}.simple-avatar__eye--right{right:25%}.simple-avatar__mouth{position:absolute;left:42%;top:72%;width:16%;height:7%;border-bottom:2px solid rgba(116,54,47,.62);border-radius:50%}.simple-avatar__initial{position:absolute;width:1px;height:1px;overflow:hidden;clip-path:inset(50%);white-space:nowrap}
.simple-avatar__type,.simple-avatar__status{position:absolute;z-index:1;display:inline-flex;align-items:center;white-space:nowrap;border:2px solid #fff;border-radius:var(--app-radius-pill);font-size:max(9px,calc(var(--simple-avatar-size) * .13));font-weight:800;line-height:1;background:#fff;box-shadow:0 3px 10px rgba(30,66,80,.14)}
.simple-avatar__type{top:-5px;right:-8px;padding:4px 6px;color:var(--app-primary-strong);background:var(--app-sky-soft)}
.simple-avatar__status{right:-8px;bottom:-7px;gap:4px;padding:4px 6px;color:var(--app-text-secondary)}.simple-avatar__status-dot{width:7px;height:7px;border-radius:50%;background:var(--app-text-muted)}
.simple-avatar__status.is-online .simple-avatar__status-dot{background:var(--app-success)}.simple-avatar__status.is-responding .simple-avatar__status-dot{background:var(--app-primary)}.simple-avatar__status.is-failed .simple-avatar__status-dot{background:var(--app-danger)}
</style>
