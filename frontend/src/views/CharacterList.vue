<template>
  <div class="character-list-page">
    <header class="page-header">
      <div>
        <button class="back-button" type="button" @click="router.push('/home')">← 返回首页</button>
        <span class="eyebrow">CHARACTER SPACE</span>
        <h1>我的角色</h1>
        <p>管理你的 AI 角色与用户角色。</p>
      </div>
      <el-button type="primary" size="large" @click="router.push('/character/create')">＋ 创建角色</el-button>
    </header>

    <main>
      <div class="filters">
        <button v-for="option in filters" :key="option.value" type="button"
          :class="{ active: selectedType === option.value }" @click="selectType(option.value)">
          {{ option.label }}
        </button>
      </div>
      <div v-loading="loading" class="list-content">
        <el-empty v-if="!loading && characters.length === 0" description="还没有符合条件的角色">
          <el-button type="primary" @click="router.push('/character/create')">创建第一个角色</el-button>
        </el-empty>
        <div v-else class="character-grid">
          <article v-for="character in characters" :key="character.id" class="character-card" tabindex="0"
            @click="openCharacter(character.id)" @keydown.enter="openCharacter(character.id)">
            <div class="card-heading">
              <div class="letter-avatar" :style="{ background: character.avatarColor || '#7c5cff' }">
                {{ character.name?.charAt(0) || '角' }}
              </div>
              <span class="type-badge" :class="character.characterType?.toLowerCase()">{{ character.characterType }}</span>
            </div>
            <h2>{{ character.name }}</h2>
            <p class="identity">{{ character.identity || '未设置身份' }}</p>
            <dl>
              <div><dt>性格</dt><dd>{{ character.corePersonality || '未设置' }}</dd></div>
              <div><dt>当前目标</dt><dd>{{ character.currentGoal || '未设置' }}</dd></div>
            </dl>
            <span class="view-link">查看完整资料 →</span>
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

const router = useRouter()
const loading = ref(false)
const characters = ref([])
const selectedType = ref('')
const filters = [{ label: '全部', value: '' }, { label: 'AI 角色', value: 'AI' }, { label: '用户角色', value: 'USER' }]
let loadSequence = 0

async function loadCharacters() {
  const sequence = ++loadSequence
  loading.value = true
  try {
    const response = await getCharacterList(selectedType.value || undefined)
    if (sequence === loadSequence) characters.value = response.data || []
  } catch (error) {
    if (sequence === loadSequence) characters.value = []
    console.error('Load characters failed:', error)
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

function selectType(type) { if (selectedType.value !== type) { selectedType.value = type; loadCharacters() } }
function openCharacter(id) { router.push(`/character/${id}`) }
onMounted(loadCharacters)
</script>

<style lang="scss" scoped>
.character-list-page { min-height: 100vh; padding: 36px; color: #fff; background: radial-gradient(circle at 12% 8%, rgba(124,92,255,.2), transparent 30%), radial-gradient(circle at 90% 80%, rgba(0,229,192,.1), transparent 28%), #060816; }
.page-header, main { width: min(1120px, 100%); margin: auto; }
.page-header { margin-bottom: 28px; display: flex; justify-content: space-between; align-items: flex-end; h1 { margin: 8px 0 4px; font-size: 34px; } p { margin: 0; color: #929ab7; } }
.back-button { display: block; margin-bottom: 22px; padding: 9px 14px; border: 1px solid rgba(124,92,255,.35); border-radius: 11px; color: #fff; background: rgba(124,92,255,.1); cursor: pointer; }
.eyebrow { color: #00e5c0; font-size: 12px; letter-spacing: 1.6px; }
.filters { display: flex; gap: 8px; margin-bottom: 22px; padding: 5px; width: fit-content; border-radius: 14px; background: rgba(255,255,255,.05); button { padding: 9px 17px; border: 0; border-radius: 10px; color: #929ab4; background: transparent; cursor: pointer; } button.active { color: #fff; background: rgba(124,92,255,.72); } }
.list-content { min-height: 300px; }
.character-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; }
.character-card { padding: 22px; border: 1px solid rgba(124,92,255,.2); border-radius: 20px; background: rgba(16,21,48,.88); cursor: pointer; transition: .2s ease; &:hover, &:focus { transform: translateY(-3px); border-color: rgba(124,92,255,.55); outline: none; box-shadow: 0 18px 45px rgba(0,0,0,.25); } h2 { margin: 16px 0 5px; } }
.card-heading { display: flex; justify-content: space-between; align-items: flex-start; }
.letter-avatar { width: 62px; height: 62px; display: grid; place-items: center; border-radius: 20px; font-size: 25px; font-weight: 700; }
.type-badge { padding: 4px 9px; border-radius: 999px; color: #b8adff; background: rgba(124,92,255,.15); font-size: 11px; &.user { color: #7cebd8; background: rgba(0,229,192,.12); } }
.identity { min-height: 22px; color: #9099b5; }
dl { margin: 20px 0; div { margin-top: 12px; } dt { color: #747e9d; font-size: 11px; } dd { margin: 5px 0 0; color: #d8dced; line-height: 1.5; display: -webkit-box; overflow: hidden; -webkit-line-clamp: 2; -webkit-box-orient: vertical; } }
.view-link { color: #9d8fff; font-size: 13px; }
@media (max-width: 900px) { .character-grid { grid-template-columns: repeat(2, minmax(0,1fr)); } }
@media (max-width: 620px) { .character-list-page { padding: 20px; } .page-header { align-items: flex-start; flex-direction: column; gap: 18px; } .character-grid { grid-template-columns: 1fr; } }
</style>
