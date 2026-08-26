<template>
  <div class="character-create-page">
    <header class="page-header">
      <button class="back-button" type="button" @click="router.push('/home')">← 返回首页</button>
      <div>
        <span class="eyebrow">CHARACTER STUDIO</span>
        <h1>创建你的角色</h1>
        <p>用自然语言生成角色草稿，确认并编辑后再保存。</p>
      </div>
    </header>

    <main class="create-layout">
      <section class="creator-card">
        <div class="mode-tabs">
          <button :class="{ active: mode === 'ai' }" type="button" @click="mode = 'ai'">AI 辅助创建</button>
          <button :class="{ active: mode === 'manual' }" type="button" @click="mode = 'manual'">手动创建</button>
        </div>

        <div class="type-row">
          <span>角色类型</span>
          <el-radio-group v-model="form.characterType">
            <el-radio-button value="AI">AI 角色</el-radio-button>
            <el-radio-button value="USER">用户角色</el-radio-button>
          </el-radio-group>
        </div>

        <div v-if="mode === 'ai'" class="ai-panel">
          <el-input
            v-model="description"
            type="textarea"
            :rows="5"
            maxlength="2000"
            show-word-limit
            placeholder="例如：创建一位冷静理性的研究员，重视诚实与成长，喜欢宇宙和人工智能，说话简洁自然。"
          />
          <el-button
            type="primary"
            size="large"
            :loading="parsing"
            :disabled="!description.trim() || parsing"
            @click="parseDescription"
          >
            {{ parsing ? 'AI 正在理解角色…' : 'AI 生成角色草稿' }}
          </el-button>
          <el-alert v-if="parseError" :title="parseError" type="error" :closable="false" show-icon />
          <el-alert
            v-else-if="parsed"
            title="AI 草稿已生成。以下所有内容都可以修改，保存时将使用你编辑后的版本。"
            type="success"
            :closable="false"
            show-icon
          />
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="character-form">
          <div class="section-title">基础设定</div>
          <div class="form-grid two-columns">
            <el-form-item label="角色名称" prop="name">
              <el-input v-model="form.name" maxlength="80" show-word-limit />
            </el-form-item>
            <el-form-item label="年龄">
              <el-input-number v-model="form.age" :min="0" :max="150" controls-position="right" />
            </el-form-item>
            <el-form-item label="身份">
              <el-input v-model="form.identity" maxlength="200" show-word-limit />
            </el-form-item>
            <el-form-item label="与用户的关系">
              <el-input v-model="form.relationshipToUser" maxlength="300" show-word-limit />
            </el-form-item>
          </div>

          <el-form-item label="核心性格">
            <el-input v-model="form.corePersonality" type="textarea" :rows="3" maxlength="1000" show-word-limit />
          </el-form-item>
          <el-form-item label="当前目标">
            <el-input v-model="form.currentGoal" type="textarea" :rows="2" maxlength="500" show-word-limit />
          </el-form-item>
          <el-form-item label="角色背景">
            <el-input v-model="form.biography" type="textarea" :rows="4" maxlength="5000" show-word-limit />
          </el-form-item>
          <el-form-item label="说话风格">
            <el-input v-model="form.speakingStyle" type="textarea" :rows="2" maxlength="500" show-word-limit />
          </el-form-item>

          <div class="section-title">深层设定</div>
          <p class="section-help">每行填写一项，每类最多 10 项。</p>
          <div class="form-grid two-columns">
            <el-form-item v-for="field in profileFields" :key="field.key" :label="field.label">
              <el-input
                v-model="form.profile[field.key]"
                type="textarea"
                :rows="3"
                maxlength="2000"
                :placeholder="field.placeholder"
              />
            </el-form-item>
          </div>

          <div class="section-title">视觉标识</div>
          <div class="color-editor">
            <el-color-picker v-model="form.avatarColor" />
            <span>{{ form.avatarColor }}</span>
            <small>本阶段使用名字首字头像，Avatar 与 VRM 均为可选。</small>
          </div>

          <div class="form-actions">
            <el-button size="large" @click="router.push('/home')">取消</el-button>
            <el-button type="primary" size="large" :loading="saving" @click="saveCharacter">保存角色</el-button>
          </div>
        </el-form>
      </section>

      <aside class="preview-card">
        <div class="letter-avatar" :style="{ background: form.avatarColor }">{{ avatarInitial }}</div>
        <span class="type-badge">{{ form.characterType }}</span>
        <h2>{{ form.name || '未命名角色' }}</h2>
        <p>{{ form.identity || '等待填写角色身份' }}</p>
        <div class="preview-divider"></div>
        <dl>
          <div><dt>性格</dt><dd>{{ form.corePersonality || '尚未设置' }}</dd></div>
          <div><dt>目标</dt><dd>{{ form.currentGoal || '尚未设置' }}</dd></div>
          <div><dt>关系</dt><dd>{{ form.relationshipToUser || '尚未设置' }}</dd></div>
        </dl>
      </aside>
    </main>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createCharacter, parseCharacter } from '../api/character'
import { applyCharacterDraft, buildCharacterCreatePayload, createCharacterForm } from '../utils/characterDraft'

const router = useRouter()
const mode = ref('ai')
const description = ref('')
const parsing = ref(false)
const saving = ref(false)
const parsed = ref(false)
const parseError = ref('')
const formRef = ref(null)
const form = reactive(createCharacterForm())

const profileFields = [
  { key: 'values', label: '价值观', placeholder: '诚实\n成长' },
  { key: 'likes', label: '喜欢', placeholder: '阅读\n安静的夜晚' },
  { key: 'dislikes', label: '不喜欢', placeholder: '欺骗' },
  { key: 'interests', label: '兴趣', placeholder: '人工智能\n宇宙' },
  { key: 'fears', label: '恐惧', placeholder: '被遗忘' },
  { key: 'secrets', label: '秘密', placeholder: '每行一项' },
  { key: 'behaviorTendencies', label: '行为倾向', placeholder: '先分析再行动' }
]

const avatarInitial = computed(() => form.name.trim().charAt(0) || '角')
const rules = { name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }] }

async function parseDescription() {
  if (!description.value.trim() || parsing.value) return
  parsing.value = true
  parsed.value = false
  parseError.value = ''
  try {
    const response = await parseCharacter({
      characterType: form.characterType,
      description: description.value.trim()
    })
    applyCharacterDraft(form, response.data)
    parsed.value = true
  } catch (error) {
    parseError.value = error?.message || 'AI 解析失败，请检查描述后重试。'
  } finally {
    parsing.value = false
  }
}

async function saveCharacter() {
  if (saving.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = buildCharacterCreatePayload(form, description.value)
    const response = await createCharacter(payload)
    ElMessage.success('角色创建成功')
    await router.push(`/character/${response.data.id}`)
  } catch (error) {
    console.error('Create character failed:', error)
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.character-create-page { min-height: 100vh; padding: 36px; color: #fff; background: radial-gradient(circle at 12% 10%, rgba(124,92,255,.2), transparent 30%), radial-gradient(circle at 88% 80%, rgba(0,229,192,.12), transparent 30%), #060816; }
.page-header { width: min(1180px, 100%); margin: 0 auto 24px; display: flex; align-items: center; gap: 22px; h1 { margin: 4px 0; font-size: 32px; } p { margin: 0; color: #98a0bd; } }
.eyebrow { color: #00e5c0; font-size: 12px; letter-spacing: 1.6px; }
.back-button { padding: 10px 15px; border: 1px solid rgba(124,92,255,.38); border-radius: 12px; color: #fff; background: rgba(124,92,255,.12); cursor: pointer; }
.create-layout { width: min(1180px, 100%); margin: auto; display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 24px; align-items: start; }
.creator-card, .preview-card { border: 1px solid rgba(124,92,255,.24); border-radius: 24px; background: rgba(16,21,48,.9); box-shadow: 0 24px 70px rgba(0,0,0,.3); backdrop-filter: blur(18px); }
.creator-card { padding: 28px; }
.mode-tabs { display: grid; grid-template-columns: 1fr 1fr; padding: 4px; border-radius: 14px; background: rgba(255,255,255,.05); button { padding: 12px; border: 0; border-radius: 11px; color: #9299b3; background: transparent; cursor: pointer; } button.active { color: #fff; background: linear-gradient(135deg, #705cff, #516ee8); } }
.type-row { margin: 22px 0; display: flex; justify-content: space-between; align-items: center; color: #cbd0e3; }
.ai-panel { display: grid; gap: 14px; margin-bottom: 26px; padding: 20px; border: 1px solid rgba(0,229,192,.18); border-radius: 18px; background: rgba(0,229,192,.04); }
.character-form { border-top: 1px solid rgba(255,255,255,.08); padding-top: 22px; }
.section-title { margin: 8px 0 15px; font-size: 18px; font-weight: 700; }
.section-help { margin: -8px 0 16px; color: #858da9; font-size: 13px; }
.form-grid.two-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 0 18px; }
.color-editor { display: flex; align-items: center; gap: 12px; margin-bottom: 28px; color: #aeb5cc; small { margin-left: 8px; } }
.form-actions { display: flex; justify-content: flex-end; gap: 12px; }
.preview-card { position: sticky; top: 24px; padding: 30px 24px; text-align: center; }
.letter-avatar { width: 96px; height: 96px; margin: 0 auto 14px; display: grid; place-items: center; border-radius: 30px; color: #fff; font-size: 38px; font-weight: 700; box-shadow: 0 14px 35px rgba(0,0,0,.3); }
.type-badge { display: inline-block; padding: 4px 10px; border-radius: 999px; color: #a99cff; background: rgba(124,92,255,.14); font-size: 12px; }
.preview-card h2 { margin: 12px 0 5px; } .preview-card > p { color: #929ab6; }
.preview-divider { height: 1px; margin: 22px 0; background: rgba(255,255,255,.08); }
dl { margin: 0; text-align: left; div { margin-bottom: 16px; } dt { margin-bottom: 5px; color: #79819e; font-size: 12px; } dd { margin: 0; color: #dce0ef; line-height: 1.55; } }
:deep(.el-form-item__label) { color: #cbd0e3; }
:deep(.el-input__wrapper), :deep(.el-textarea__inner) { color: #fff; background: rgba(255,255,255,.055); box-shadow: 0 0 0 1px rgba(255,255,255,.12) inset; }
@media (max-width: 860px) { .character-create-page { padding: 20px; } .create-layout { grid-template-columns: 1fr; } .preview-card { position: static; grid-row: 1; } }
@media (max-width: 600px) { .page-header { align-items: flex-start; flex-direction: column; } .creator-card { padding: 20px; } .form-grid.two-columns { grid-template-columns: 1fr; } .type-row { align-items: flex-start; flex-direction: column; gap: 10px; } }
</style>

