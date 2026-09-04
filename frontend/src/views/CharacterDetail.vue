<template>
  <div class="character-detail-page">
    <header class="page-header">
      <button type="button" @click="router.push('/characters')">← 返回角色列表</button>
      <div v-if="character" class="header-actions">
        <el-button v-if="!editing" @click="startEditing">编辑</el-button>
        <el-button v-if="!editing" type="danger" plain :loading="deleting" @click="removeCharacter">删除</el-button>
      </div>
    </header>

    <main v-loading="loading">
      <el-empty v-if="!loading && !character" description="角色不存在" />
      <section v-else-if="character" class="detail-card">
        <div class="profile-heading">
          <SimpleAvatar :name="character.name" :image-url="character.imageUrl" :avatar-color="character.avatarColor" :entity-key="character.id" :type="character.characterType" size="lg" />
          <div>
            <h1>{{ character.name }}</h1>
            <p>{{ character.identity || '未设置身份' }}</p>
          </div>
        </div>

        <el-form v-if="editing" ref="formRef" :model="form" :rules="rules" label-position="top" class="edit-form">
          <div class="form-grid">
            <el-form-item label="类型">
              <el-radio-group v-model="form.characterType">
                <el-radio-button value="AI">AI</el-radio-button><el-radio-button value="USER">USER</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="年龄"><el-input-number v-model="form.age" :min="0" :max="150" /></el-form-item>
            <el-form-item label="名称" prop="name"><el-input v-model="form.name" maxlength="80" /></el-form-item>
            <el-form-item label="身份"><el-input v-model="form.identity" maxlength="200" /></el-form-item>
          </div>
          <el-form-item label="核心性格"><el-input v-model="form.corePersonality" type="textarea" :rows="3" maxlength="1000" /></el-form-item>
          <el-form-item label="当前目标"><el-input v-model="form.currentGoal" type="textarea" :rows="2" maxlength="500" /></el-form-item>
          <el-form-item label="角色背景"><el-input v-model="form.biography" type="textarea" :rows="4" maxlength="5000" /></el-form-item>
          <el-form-item label="与用户关系"><el-input v-model="form.relationshipToUser" maxlength="300" /></el-form-item>
          <el-form-item label="说话风格"><el-input v-model="form.speakingStyle" type="textarea" :rows="2" maxlength="500" /></el-form-item>
          <div class="form-grid">
            <el-form-item v-for="field in profileFields" :key="field.key" :label="field.label">
              <el-input v-model="form.profile[field.key]" type="textarea" :rows="3" maxlength="2000" placeholder="每行一项" />
            </el-form-item>
          </div>
          <el-form-item label="头像颜色"><el-color-picker v-model="form.avatarColor" /></el-form-item>
          <div class="edit-actions">
            <el-button @click="cancelEditing">取消</el-button>
            <el-button type="primary" :loading="saving" @click="saveChanges">保存修改</el-button>
          </div>
        </el-form>

        <template v-else>
          <div class="details">
            <div><span>核心性格</span><p>{{ character.corePersonality || '未设置' }}</p></div>
            <div><span>当前目标</span><p>{{ character.currentGoal || '未设置' }}</p></div>
            <div><span>角色背景</span><p>{{ character.biography || '未设置' }}</p></div>
            <div><span>说话风格</span><p>{{ character.speakingStyle || '未设置' }}</p></div>
            <div><span>与用户关系</span><p>{{ character.relationshipToUser || '未设置' }}</p></div>
            <div><span>年龄</span><p>{{ character.age ?? '未设置' }}</p></div>
          </div>
          <div class="profile-section">
            <h2>完整人物资料</h2>
            <div class="profile-grid">
              <div v-for="field in profileFields" :key="field.key" class="profile-group">
                <span>{{ field.label }}</span>
                <div v-if="profileItems(field.key).length" class="tags">
                  <el-tag v-for="item in profileItems(field.key)" :key="item" effect="dark">{{ item }}</el-tag>
                </div>
                <p v-else>未设置</p>
              </div>
            </div>
          </div>
        </template>
      </section>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteCharacter, getCharacterById, updateCharacter } from '../api/character'
import { buildCharacterUpdatePayload, createCharacterForm } from '../utils/characterDraft'
import SimpleAvatar from '../components/ui/SimpleAvatar.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const editing = ref(false)
const character = ref(null)
const formRef = ref(null)
const form = reactive(createCharacterForm())
const profileFields = [
  { key: 'values', label: '价值观' }, { key: 'likes', label: '喜欢' },
  { key: 'dislikes', label: '不喜欢' }, { key: 'interests', label: '兴趣' },
  { key: 'fears', label: '恐惧' }, { key: 'secrets', label: '秘密' },
  { key: 'behaviorTendencies', label: '行为倾向' }
]
const rules = { name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }] }

function characterId() {
  const id = Number(route.params.id)
  return Number.isSafeInteger(id) && id > 0 ? id : null
}

async function loadCharacter() {
  const id = characterId()
  if (!id) { ElMessage.error('无效的角色ID'); return }
  loading.value = true
  try {
    const response = await getCharacterById(id)
    character.value = response.data
  } catch (error) {
    character.value = null
    console.error('Load character failed:', error)
  } finally {
    loading.value = false
  }
}

function startEditing() {
  const editable = createCharacterForm(character.value)
  Object.assign(form, editable)
  form.profile = { ...editable.profile }
  editing.value = true
}

function cancelEditing() { editing.value = false }
function profileItems(field) { return Array.isArray(character.value?.profile?.[field]) ? character.value.profile[field] : [] }

async function saveChanges() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || saving.value) return
  saving.value = true
  try {
    await updateCharacter(characterId(), buildCharacterUpdatePayload(form))
    await loadCharacter()
    editing.value = false
    ElMessage.success('角色资料已更新')
  } catch (error) {
    console.error('Update character failed:', error)
  } finally {
    saving.value = false
  }
}

async function removeCharacter() {
  if (deleting.value) return
  try {
    await ElMessageBox.confirm(`确定删除角色“${character.value.name}”吗？`, '删除角色', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
  } catch { return }
  deleting.value = true
  try {
    await deleteCharacter(characterId())
    ElMessage.success('角色已删除')
    await router.push('/characters')
  } catch (error) {
    console.error('Delete character failed:', error)
  } finally {
    deleting.value = false
  }
}

loadCharacter()
</script>

<style lang="scss" scoped>
.character-detail-page{min-height:100%;padding:clamp(22px,4vw,48px) 24px 56px;color:var(--app-text)}
.page-header, main { width: min(900px, 100%); margin: auto; }
.page-header{margin-bottom:24px;display:flex;justify-content:space-between;gap:12px;button{min-height:44px;padding:10px 15px;border:1px solid var(--app-border-strong);border-radius:var(--app-radius-sm);color:var(--app-primary-strong);background:var(--app-surface);box-shadow:var(--app-shadow-sm)}}
.header-actions { display: flex; gap: 10px; }
.detail-card{min-width:0;padding:36px;border:1px solid var(--app-border);border-radius:var(--app-radius-lg);background:var(--app-surface);box-shadow:var(--app-shadow-lg)}
.profile-heading{display:flex;align-items:center;gap:20px;h1{margin:7px 0 3px;font-size:clamp(26px,4vw,36px)}p{margin:0;color:var(--app-text-secondary)}}
.details{margin-top:28px;display:grid;grid-template-columns:1fr 1fr;gap:14px;div{min-width:0;padding:18px;border:1px solid var(--app-border);border-radius:var(--app-radius-md);background:var(--app-surface-subtle)}span,.profile-group>span{color:var(--app-text-muted);font-size:12px}p{margin:7px 0 0;line-height:1.6;overflow-wrap:anywhere}}
.profile-section{margin-top:28px;padding-top:22px;border-top:1px solid var(--app-border)}
.profile-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.profile-group{min-width:0;padding:16px;border-radius:var(--app-radius-md);background:var(--app-peach-soft);p{color:var(--app-text-secondary)}}
.tags { display: flex; flex-wrap: wrap; gap: 7px; margin-top: 10px; }
.edit-form{margin-top:30px;padding-top:24px;border-top:1px solid var(--app-border)}
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 18px; }
.edit-actions { display: flex; justify-content: flex-end; gap: 10px; }
:deep(.el-form-item__label){color:var(--app-text)}:deep(.el-input__wrapper),:deep(.el-textarea__inner){color:var(--app-text);background:#fff;box-shadow:0 0 0 1px var(--app-border-strong) inset}:deep(.el-tag){border-color:var(--app-mint);color:var(--app-success);background:var(--app-mint-soft)}
@media(max-width:650px){.character-detail-page{padding:22px 16px 40px}.page-header{align-items:stretch;flex-direction:column}.header-actions{width:100%}.header-actions :deep(.el-button){flex:1;margin-left:0}.detail-card{padding:23px 18px}.details,.profile-grid,.form-grid{grid-template-columns:1fr}.profile-heading{align-items:flex-start}.edit-actions{flex-direction:column-reverse}.edit-actions :deep(.el-button){width:100%;margin-left:0}}
</style>
