<template>
  <div class="interaction-page">
    <WorldInteractionHeader :world="world" :round="activeRound" @back="backToWorld" />
    <main v-loading="loadingInitial">
      <div v-if="error && !world" class="state-card"><p>{{ error }}</p><el-button type="primary" @click="initialize">重新加载</el-button></div>
      <template v-else-if="world">
        <div class="interaction-layout">
          <WorldInteractionCast :world="world" :participants="orderedParticipants" :speaking-participant-id="speakingParticipantId" />
          <div class="conversation-column">
            <WorldTimeline :items="timelineItems" :participants="orderedParticipants" :has-more="hasMore" :loading-more="loadingMore" :history-error="historyError" @load-more="store.loadOlder" />
            <WorldComposer v-model="draftInput" :busy="busy" :sending="sending" :notice="notice" :queue-busy="queueBusy" :can-recover="canRecover" @send="send" @recover="store.recoverExecution" />
          </div>
        </div>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { useWorldInteractionStore } from '../stores/worldInteraction'
import WorldInteractionHeader from '../components/world/WorldInteractionHeader.vue'
import WorldInteractionCast from '../components/world/WorldInteractionCast.vue'
import WorldTimeline from '../components/world/WorldTimeline.vue'
import WorldComposer from '../components/world/WorldComposer.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const store = useWorldInteractionStore()
const {
  world, timelineItems, activeRound, draftInput, loadingInitial, loadingMore, sending,
  queueBusy, notice, error, hasMore, historyError, busy, orderedParticipants, speakingParticipantId, canRecover
} = storeToRefs(store)

const routeWorldId = computed(() => {
  const id = Number(route.params.worldId)
  return Number.isSafeInteger(id) && id > 0 ? id : null
})

async function initialize() {
  if (!routeWorldId.value) {
    store.error = '无效的World ID'
    return
  }
  await store.initialize(routeWorldId.value, userStore.userInfo?.id)
}

async function send() {
  const sent = await store.send(draftInput.value)
  if (!sent && !busy.value && !store.pendingSubmission) ElMessage.warning('请输入1到4000个字符')
}

function backToWorld() {
  router.push(`/worlds/${routeWorldId.value}`)
}

function handleVisibility() {
  store.setPageHidden(document.hidden)
}

watch(() => route.params.worldId, initialize)
onMounted(() => {
  document.addEventListener('visibilitychange', handleVisibility)
  initialize()
})
onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibility)
  store.dispose()
})
</script>

<style lang="scss" scoped>
.interaction-page{min-height:100vh;padding-bottom:30px;color:var(--world-ink);background:radial-gradient(circle at 3% 35%,rgba(144,231,197,.2),transparent 24%),radial-gradient(circle at 98% 18%,rgba(255,171,135,.2),transparent 25%),var(--world-bg)}main{width:min(var(--world-content),calc(100% - 48px));margin:auto}.interaction-layout{display:grid;grid-template-columns:310px minmax(0,1fr);align-items:start;gap:22px}.conversation-column{min-width:0}.state-card{padding:34px;border:1px solid var(--world-border);border-radius:var(--world-radius-lg);text-align:center;background:#fff;box-shadow:var(--world-shadow-sm)}.state-card p{margin-bottom:14px;color:var(--world-muted)}@media(max-width:850px){.interaction-layout{grid-template-columns:1fr}}@media(max-width:620px){main{width:calc(100% - 24px)}.interaction-page{padding-bottom:14px}}
</style>
