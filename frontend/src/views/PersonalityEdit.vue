<template>
  <div class="personality-edit-page">
    <header class="page-header">
      <button class="back-button" type="button" @click="goBack">← 返回</button>
      <div>
        <h1>编辑 AI 人格</h1>
        <p>人格修改只会影响之后创建的聊天会话</p>
      </div>
    </header>

    <main class="edit-layout">
      <section class="form-card" v-loading="loading">
        <div class="card-heading">
          <SimpleAvatar :name="personality?.name" :image-url="personality?.imageUrl" :avatar-color="personality?.avatarColor" :entity-key="avatarId" type="AI" size="lg" />
          <div class="identity-summary">
            <span class="eyebrow">PERSONALITY PROFILE</span>
            <h2>{{ personality?.name || '人格设置' }}</h2>
            <span class="avatar-badge">AI 形象 · #{{ avatarId }}</span>
          </div>
        </div>

        <el-alert
          title="已有聊天会继续使用创建时的人格快照，不会被本次修改覆盖。"
          type="info"
          :closable="false"
          show-icon
        />

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="personality-form"
        >
          <el-form-item label="核心性格" prop="corePersonality">
            <el-input v-model="form.corePersonality" maxlength="500" show-word-limit />
          </el-form-item>

          <el-form-item label="身份设定" prop="identity">
            <el-input v-model="form.identity" maxlength="100" show-word-limit />
          </el-form-item>

          <el-form-item label="语言风格" prop="languageStyle">
            <el-input
              v-model="form.languageStyle"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="兴趣爱好" prop="hobbies">
            <el-input
              v-model="form.hobbies"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="与用户的关系" prop="relationship">
            <el-input v-model="form.relationship" maxlength="100" show-word-limit />
          </el-form-item>

          <div class="form-actions">
            <el-button size="large" @click="goBack">取消</el-button>
            <el-button type="primary" size="large" :loading="saving" @click="save">
              保存人格
            </el-button>
          </div>
        </el-form>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPersonalityByAvatarId, updatePersonality } from '../api/personality'
import {
  buildPersonalityUpdatePayload,
  createPersonalityEditForm
} from '../utils/personalityForm'
import SimpleAvatar from '../components/ui/SimpleAvatar.vue'

const route = useRoute()
const router = useRouter()
const avatarId = Number(route.params.avatarId)
const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const personality = ref(null)
const form = reactive(createPersonalityEditForm())

const rules = {
  corePersonality: [{ required: true, message: '请输入核心性格', trigger: 'blur' }],
  identity: [{ required: true, message: '请输入身份设定', trigger: 'blur' }],
  languageStyle: [{ required: true, message: '请输入语言风格', trigger: 'blur' }]
}

function fillForm(data) {
  Object.assign(form, createPersonalityEditForm(data))
}

async function loadPersonality() {
  if (!Number.isSafeInteger(avatarId) || avatarId <= 0) {
    ElMessage.error('无效的形象ID')
    router.replace('/home')
    return
  }
  loading.value = true
  try {
    const response = await getPersonalityByAvatarId(avatarId)
    personality.value = response.data
    fillForm(response.data)
  } catch (error) {
    console.error('Load personality failed:', error)
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!personality.value || saving.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = buildPersonalityUpdatePayload(avatarId, personality.value, form)
    const response = await updatePersonality(payload)
    personality.value = response.data
    fillForm(response.data)
    ElMessage.success('人格已更新，新创建的聊天会话将使用新人格，已有聊天保持原人格。')
  } catch (error) {
    console.error('Update personality failed:', error)
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/home')
}

onMounted(loadPersonality)
</script>

<style lang="scss" scoped>
.personality-edit-page{min-height:100%;padding:clamp(22px,4vw,48px) 24px 56px;color:var(--app-text)}

.page-header {
  width: min(820px, 100%);
  margin: 0 auto 24px;
  display: flex;
  align-items: center;
  gap: 24px;

  h1 { margin: 0 0 6px; font-size: clamp(27px,4vw,34px); }
  p { margin: 0; color: var(--app-text-secondary); }
}

.back-button {
  padding: 10px 16px;
  min-height:44px;
  border: 1px solid var(--app-border-strong);
  border-radius: var(--app-radius-sm);
  color: var(--app-primary-strong);
  background: var(--app-surface);
  box-shadow:var(--app-shadow-sm);
  cursor: pointer;
}

.edit-layout { width: min(820px, 100%); margin: 0 auto; }

.form-card {
  padding: 32px;
  min-width:0;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  background: var(--app-surface);
  box-shadow: var(--app-shadow-lg);
}

.card-heading {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 22px;
  h2 { margin: 5px 0 0; }
}

.identity-summary{min-width:0}.eyebrow{color:var(--app-primary-strong);font-size:12px;font-weight:800;letter-spacing:1.5px}
.avatar-badge{display:inline-block;padding:6px 10px;border-radius:var(--app-radius-pill);color:var(--app-primary-strong);background:var(--app-primary-soft);font-size:12px;font-weight:700}
.personality-form { margin-top: 24px; }
.form-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 10px; }

:deep(.el-form-item__label) { color: var(--app-text); }
:deep(.el-input__wrapper), :deep(.el-textarea__inner) {
  background: #fff;
  box-shadow: 0 0 0 1px var(--app-border-strong) inset;
  color: var(--app-text);
}

@media (max-width: 640px) {
  .personality-edit-page { padding: 22px 16px 40px; }
  .page-header { align-items: flex-start; flex-direction: column; gap: 14px; }
  .form-card { padding: 22px 18px; }
  .card-heading{align-items:flex-start}
  .form-actions{flex-direction:column-reverse}.form-actions :deep(.el-button){width:100%;margin-left:0}
}
</style>
