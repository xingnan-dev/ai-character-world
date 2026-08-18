<template>
  <div ref="containerRef" class="avatar-renderer">
    <div v-if="loading && showLoading" class="loading-overlay">
      <div class="loading-spinner"></div>
      <span>加载3D模型中...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, shallowRef } from 'vue'
import AvatarScene from '@/three/AvatarScene.js'
import { createAvatarLoadState, runAvatarLoad } from '@/utils/avatarLoadState'

const props = defineProps({
  modelUrl: {
    type: String,
    default: ''
  },
  avatarName: {
    type: String,
    default: ''
  },
  presentation: {
    type: String,
    default: 'standard'
  },
  behaviorState: {
    type: String,
    default: 'idle'
  }
})

const emit = defineEmits(['loaded', 'error'])

const containerRef = ref(null)
const loading = ref(false)
const vrmLoaded = ref(false)
const sceneInstance = shallowRef(null)
const loadState = createAvatarLoadState()
const pendingBehaviorState = ref(props.behaviorState)

const resolvedModelUrl = computed(() => props.modelUrl?.trim() || '')

const showLoading = computed(() => loading.value && !vrmLoaded.value)

const loadModel = async (url = resolvedModelUrl.value) => {
  if (!sceneInstance.value) return
  if (!url) {
    vrmLoaded.value = false
    return
  }

  const scene = sceneInstance.value
  await runAvatarLoad({
    state: loadState,
    url,
    load: (modelUrl) => scene.loadVRM(modelUrl),
    onStart: () => {
      loading.value = true
      vrmLoaded.value = false
    },
    onLoaded: () => {
      vrmLoaded.value = true
      emit('loaded')
    },
    onError: (err) => {
      vrmLoaded.value = false
      console.warn('VRM加载错误，使用占位形象:', err.message)
      emit('error', err)
    },
    onSettled: () => {
      loading.value = false
    }
  })
}

onMounted(() => {
  if (!containerRef.value) return
  const scene = new AvatarScene(containerRef.value, {
    presentation: props.presentation
  })
  scene.setBehaviorState(pendingBehaviorState.value)
  scene.init()
  sceneInstance.value = scene

  loadModel()
})

watch(
  resolvedModelUrl,
  (newUrl, oldUrl) => {
    if (newUrl !== oldUrl) loadModel(newUrl)
  }
)

watch(
  () => props.behaviorState,
  (state) => {
    pendingBehaviorState.value = state
    sceneInstance.value?.setBehaviorState(state)
  }
)

onBeforeUnmount(() => {
  loadState.dispose()
  if (sceneInstance.value) {
    sceneInstance.value.dispose()
    sceneInstance.value = null
  }
})

const exposed = {
  getScene: () => sceneInstance.value,
  setExpression: (name, value) => sceneInstance.value?.setExpression(name, value),
  setLookAt: (position) => sceneInstance.value?.setLookAt(position)
}

defineExpose(exposed)
</script>

<style lang="scss" scoped>
.avatar-renderer {
  width: 100%;
  height: 100%;
  min-height: 140px;
  display: block;
  border-radius: inherit;
  overflow: hidden;
  position: relative;

  :deep(canvas) {
    display: block;
    width: 100% !important;
    height: 100% !important;
  }
}

.loading-overlay {
  position: absolute;
  inset: 0;
  background: rgba(22, 33, 62, 0.7);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #fff;
  font-size: 13px;
  z-index: 10;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(255, 255, 255, 0.2);
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
