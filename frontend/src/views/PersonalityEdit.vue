<template>
  <div class="personality-edit-page">
    <header class="page-header">
      <button class="back-button" type="button" @click="goBack">← 返回 AI 空间</button>
      <div>
        <h1>编辑 AI 人格</h1>
        <p>人格修改只会影响之后创建的聊天会话</p>
      </div>
    </header>

    <main class="edit-layout">
      <section class="form-card" v-loading="loading">
        <div class="card-heading">
          <div>
            <span class="eyebrow">PERSONALITY PROFILE</span>
            <h2>{{ personality?.name || '人格设置' }}</h2>
          </div>
          <span class="avatar-badge">Avatar #{{ avatarId }}</span>
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
.personality-edit-page {
  min-height: 100vh;
  padding: 40px;
  color: #fff;
  background:
    radial-gradient(circle at 20% 10%, rgba(124, 92, 255, 0.2), transparent 30%),
    radial-gradient(circle at 85% 80%, rgba(0, 229, 192, 0.12), transparent 30%),
    #060816;
}

.page-header {
  width: min(820px, 100%);
  margin: 0 auto 24px;
  display: flex;
  align-items: center;
  gap: 24px;

  h1 { margin: 0 0 6px; font-size: 30px; }
  p { margin: 0; color: rgba(255, 255, 255, 0.58); }
}

.back-button {
  padding: 10px 16px;
  border: 1px solid rgba(124, 92, 255, 0.4);
  border-radius: 12px;
  color: #fff;
  background: rgba(124, 92, 255, 0.12);
  cursor: pointer;
}

.edit-layout { width: min(820px, 100%); margin: 0 auto; }

.form-card {
  padding: 32px;
  border: 1px solid rgba(124, 92, 255, 0.25);
  border-radius: 24px;
  background: rgba(16, 21, 48, 0.88);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(20px);
}

.card-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 22px;
  h2 { margin: 5px 0 0; }
}

.eyebrow { color: #00e5c0; font-size: 12px; letter-spacing: 1.5px; }
.avatar-badge { padding: 7px 12px; border-radius: 20px; background: rgba(124, 92, 255, 0.18); }
.personality-form { margin-top: 24px; }
.form-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 10px; }

:deep(.el-form-item__label) { color: rgba(255, 255, 255, 0.82); }
:deep(.el-input__wrapper), :deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.06);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.12) inset;
  color: #fff;
}

@media (max-width: 640px) {
  .personality-edit-page { padding: 20px; }
  .page-header { align-items: flex-start; flex-direction: column; gap: 14px; }
  .form-card { padding: 22px; }
}
</style>
