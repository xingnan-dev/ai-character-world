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
import { getModelUrlByName } from '@/config/avatarModels'

const props = defineProps({
  modelUrl: {
    type: String,
    default: ''
  },
  avatarName: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['loaded', 'error'])

const containerRef = ref(null)
const loading = ref(false)
const vrmLoaded = ref(false)
const sceneInstance = shallowRef(null)

const resolvedModelUrl = computed(() => {
  if (props.modelUrl) return props.modelUrl
  return getModelUrlByName(props.avatarName)
})

const showLoading = computed(() => loading.value && !vrmLoaded.value)

const loadModel = async () => {
  if (!sceneInstance.value) return

  const url = resolvedModelUrl.value
  if (!url) {
    vrmLoaded.value = false
    return
  }

  loading.value = true
  vrmLoaded.value = false

  try {
    await sceneInstance.value.loadVRM(url)
    // loadVRM 内部已处理 onLoadCallback/onErrorCallback
    // 这里仅处理加载完成后的状态更新
    vrmLoaded.value = true
    emit('loaded')
  } catch (err) {
    // loadVRM 内部已通过 onErrorCallback 报告错误
    // 这里仅做兜底处理
    vrmLoaded.value = false
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!containerRef.value) return
  const scene = new AvatarScene(containerRef.value)
  scene.init()
  sceneInstance.value = scene

  scene.onLoad((vrm) => {
    loading.value = false
    vrmLoaded.value = true
  })

  scene.onError((err) => {
    loading.value = false
    vrmLoaded.value = false
    console.warn('VRM加载错误，使用占位形象:', err.message)
    emit('error', err)
  })

  loadModel()
})

watch(
  () => [props.modelUrl, props.avatarName],
  ([newUrl, newName], [oldUrl, oldName]) => {
    if (newUrl !== oldUrl || newName !== oldName) {
      loadModel()
    }
  }
)

onBeforeUnmount(() => {
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