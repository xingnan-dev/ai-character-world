<template>
  <div class="character-list-page">
    <header class="page-header">
      <div>
        <span class="eyebrow">CHARACTER SPACE</span>
        <h1>我的角色</h1>
        <p>管理你的 AI 角色与用户角色。</p>
      </div>
      <el-button type="primary" size="large" @click="router.push('/character/create')">＋ 创建角色</el-button>
    </header>

    <main>
      <div class="filters app-surface--inset" role="group" aria-label="角色类型筛选">
        <button v-for="option in filters" :key="option.value" type="button"
          :class="{ active: selectedType === option.value }" :aria-pressed="selectedType === option.value" @click="selectType(option.value)">
          {{ option.label }}
        </button>
      </div>
      <div v-loading="loading" class="list-content">
        <el-alert v-if="!loading && loadError" :title="loadError" type="error" :closable="false" show-icon>
          <template #default><button class="retry-button" type="button" @click="loadCharacters">重新加载</button></template>
        </el-alert>
        <el-empty v-else-if="!loading && characters.length === 0" description="还没有符合条件的角色">
          <el-button type="primary" @click="router.push('/character/create')">创建第一个角色</el-button>
        </el-empty>
        <div v-else class="character-grid">
          <article v-for="character in characters" :key="character.id" class="character-card app-surface--elevated" tabindex="0" role="link" :aria-label="`查看角色 ${character.name} 的完整资料`"
            @click="openCharacter(character.id)" @keydown.enter.prevent="openCharacter(character.id)" @keydown.space.prevent="openCharacter(character.id)">
            <div class="card-heading"><SimpleAvatar :name="character.name" :image-url="character.imageUrl" :avatar-color="character.avatarColor" :entity-key="character.id" :type="character.characterType" size="md" /></div>
            <h2>{{ character.name }}</h2>
            <p class="identity">{{ character.identity || '未设置身份' }}</p>
            <dl>
              <div><dt>性格</dt><dd>{{ character.corePersonality || '未设置' }}</dd></div>
              <div><dt>当前目标</dt><dd>{{ character.currentGoal || '未设置' }}</dd></div>
            </dl>
            <span class="view-link">查看完整资料 →</span>
            <el-button v-if="character.characterType === 'USER'" size="small"
              :type="userStore.userInfo.currentUserCharacterId === character.id ? 'success' : 'primary'"
              :disabled="userStore.userInfo.currentUserCharacterId === character.id"
              @click.stop="selectIdentity(character)">
              {{ userStore.userInfo.currentUserCharacterId === character.id ? '当前身份' : '设为当前身份' }}
            </el-button>
          </article>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCharacterList } from '../api/character'
import SimpleAvatar from '../components/ui/SimpleAvatar.vue'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const characters = ref([])
const selectedType = ref('')
const loadError = ref('')
const filters = [{ label: '全部', value: '' }, { label: 'AI 角色', value: 'AI' }, { label: '用户角色', value: 'USER' }]
let loadSequence = 0

async function loadCharacters() {
  const sequence = ++loadSequence
  loading.value = true
  loadError.value = ''
  try {
    const response = await getCharacterList(selectedType.value || undefined)
    if (sequence === loadSequence) characters.value = response.data || []
  } catch (error) {
    if (sequence === loadSequence) characters.value = []
    if (sequence === loadSequence) loadError.value = error?.message || '角色加载失败，请稍后重试。'
    console.error('Load characters failed:', error)
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

function selectType(type) { if (selectedType.value !== type) { selectedType.value = type; loadCharacters() } }
function openCharacter(id) { router.push(`/character/${id}`) }
async function selectIdentity(character) { await userStore.selectCurrentUserCharacter(character.id); ElMessage.success('当前用户身份已更新') }
onMounted(()=>{ userStore.getUserInfoAction(); loadCharacters() })
</script>

<style lang="scss" scoped>
.character-list-page { min-width:0; min-height:100%; padding:clamp(22px,4vw,48px) 24px 56px; color:var(--app-text-primary); }
.page-header, main { width: min(1120px, 100%); margin: auto; }
.page-header { margin-bottom:28px; display:flex; justify-content:space-between; align-items:flex-end; gap:20px; h1{margin:8px 0 4px;font-size:clamp(28px,4vw,38px)} p{margin:0;color:var(--app-text-secondary)} }
.eyebrow{color:var(--app-accent-primary-strong);font-size:var(--app-type-label);font-weight:800;letter-spacing:1.6px}
.filters{display:flex;gap:8px;margin-bottom:22px;padding:5px;width:fit-content;max-width:100%;button{min-height:44px;padding:9px 17px;border-radius:var(--app-radius-sm);color:var(--app-text-secondary)}button:hover{background:var(--app-surface-subtle)}button.active{color:var(--app-accent-primary-strong);font-weight:750;background:var(--app-primary-soft);box-shadow:inset 0 0 0 1px var(--app-accent-secondary)}}
.list-content { min-height: 300px; }
.retry-button{margin-top:10px;padding:8px 12px;border-radius:var(--app-radius-sm);color:var(--app-danger);font-weight:700;background:var(--app-danger-soft)}
.character-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; }
.character-card{min-width:0;padding:var(--app-card-padding);cursor:pointer;transition:border-color var(--app-motion-normal) var(--app-ease-standard),box-shadow var(--app-motion-normal) var(--app-ease-standard),transform var(--app-motion-normal) var(--app-ease-standard);&:hover,&:focus-visible{transform:translateY(-2px);border-color:var(--app-accent-secondary);box-shadow:var(--app-selected-glow)}h2{margin:18px 0 5px;overflow-wrap:anywhere}}
.card-heading { display: flex; justify-content: space-between; align-items: flex-start; }
.identity{min-height:22px;color:var(--app-text-secondary)}
dl{margin:20px 0;div{margin-top:12px;padding:12px;border-radius:var(--app-radius-sm);background:var(--app-surface-inset)}dt{color:var(--app-text-muted);font-size:var(--app-type-label)}dd{margin:5px 0 0;color:var(--app-text-primary);line-height:1.5;display:-webkit-box;overflow:hidden;overflow-wrap:anywhere;-webkit-line-clamp:2;-webkit-box-orient:vertical}}
.view-link{color:var(--app-accent-primary-strong);font-size:13px;font-weight:700}
@media (prefers-reduced-motion:reduce){.character-card{transition:none}.character-card:hover,.character-card:focus-visible{transform:none}}
@media (forced-colors:active){.filters,.character-card{border:1px solid CanvasText}.filters button.active{outline:2px solid Highlight}}
@media (max-width: 1024px) { .page-header, main { width:min(960px,100%); } .character-grid { gap:16px; } }
@media (max-width: 900px) { .character-grid { grid-template-columns: repeat(2, minmax(0,1fr)); } }
@media (max-width:620px){.character-list-page{padding:22px 16px 40px}.page-header{align-items:flex-start;flex-direction:column;gap:18px}.page-header :deep(.el-button){width:100%}.filters{width:100%}.filters button{flex:1 1 0;padding-inline:8px}.character-grid{grid-template-columns:1fr}}
</style>
