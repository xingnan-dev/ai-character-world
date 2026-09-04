<template>
  <div class="character-create-page">
    <header class="page-header">
      <button class="back-button" type="button" @click="router.push('/characters')">← 返回角色列表</button>
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
            <small>未选择图片时使用稳定的 2D 首字头像。</small>
          </div>

          <div class="form-actions">
            <el-button size="large" @click="router.push('/characters')">取消</el-button>
            <el-button type="primary" size="large" :loading="saving" @click="saveCharacter">保存角色</el-button>
          </div>
        </el-form>
      </section>

      <aside class="preview-card">
        <SimpleAvatar :name="form.name" :avatar-color="form.avatarColor" :entity-key="form.name" :type="form.characterType" size="xl" />
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
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createCharacter, parseCharacter } from '../api/character'
import { applyCharacterDraft, buildCharacterCreatePayload, createCharacterForm } from '../utils/characterDraft'
import SimpleAvatar from '../components/ui/SimpleAvatar.vue'

const router = useRouter()
const route = useRoute()
const mode = ref('ai')
const description = ref('')
const parsing = ref(false)
const saving = ref(false)
const parsed = ref(false)
const parseError = ref('')
const formRef = ref(null)
const form = reactive(createCharacterForm())
if (route.query.type === 'USER') form.characterType = 'USER'

const profileFields = [
  { key: 'values', label: '价值观', placeholder: '诚实\n成长' },
  { key: 'likes', label: '喜欢', placeholder: '阅读\n安静的夜晚' },
  { key: 'dislikes', label: '不喜欢', placeholder: '欺骗' },
  { key: 'interests', label: '兴趣', placeholder: '人工智能\n宇宙' },
  { key: 'fears', label: '恐惧', placeholder: '被遗忘' },
  { key: 'secrets', label: '秘密', placeholder: '每行一项' },
  { key: 'behaviorTendencies', label: '行为倾向', placeholder: '先分析再行动' }
]

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
    parseError.value = error?.message || 'AI 解析失败，原始描述已保留，请手动填写或稍后重试。'
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
.character-create-page{min-height:100%;padding:clamp(22px,4vw,48px) 24px 56px;color:var(--app-text)}
.page-header{width:min(1180px,100%);margin:0 auto 24px;display:flex;align-items:center;gap:22px;h1{margin:4px 0;font-size:clamp(27px,4vw,36px)}p{margin:0;color:var(--app-text-secondary)}}
.eyebrow{color:var(--app-primary-strong);font-size:12px;font-weight:800;letter-spacing:1.6px}
.back-button{min-height:44px;padding:10px 15px;border:1px solid var(--app-border-strong);border-radius:var(--app-radius-sm);color:var(--app-primary-strong);background:var(--app-surface);box-shadow:var(--app-shadow-sm)}
.create-layout { width: min(1180px, 100%); margin: auto; display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 24px; align-items: start; }
.creator-card,.preview-card{min-width:0;border:1px solid var(--app-border);border-radius:var(--app-radius-lg);background:var(--app-surface);box-shadow:var(--app-shadow-lg)}
.creator-card { padding: 28px; }
.mode-tabs{display:grid;grid-template-columns:1fr 1fr;padding:4px;border:1px solid var(--app-border);border-radius:var(--app-radius-md);background:var(--app-surface-subtle);button{min-height:44px;padding:12px;border-radius:var(--app-radius-sm);color:var(--app-text-secondary)}button.active{color:var(--app-primary-strong);font-weight:750;background:var(--app-primary-soft);box-shadow:inset 0 0 0 1px var(--app-sky)}}
.type-row{margin:22px 0;display:flex;justify-content:space-between;align-items:center;color:var(--app-text)}
.ai-panel{display:grid;gap:14px;margin-bottom:26px;padding:20px;border:1px solid var(--app-mint);border-radius:var(--app-radius-md);background:var(--app-mint-soft)}
.character-form{border-top:1px solid var(--app-border);padding-top:22px}
.section-title { margin: 8px 0 15px; font-size: 18px; font-weight: 700; }
.section-help{margin:-8px 0 16px;color:var(--app-text-secondary);font-size:13px}
.form-grid.two-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 0 18px; }
.color-editor{display:flex;align-items:center;gap:12px;margin-bottom:28px;color:var(--app-text-secondary);small{margin-left:8px}}
.form-actions { display: flex; justify-content: flex-end; gap: 12px; }
.preview-card{position:sticky;top:24px;padding:30px 24px;text-align:center}.preview-card :deep(.simple-avatar){margin:0 auto 18px}
.preview-card h2{margin:12px 0 5px}.preview-card>p{color:var(--app-text-secondary)}
.preview-divider{height:1px;margin:22px 0;background:var(--app-border)}
dl{margin:0;text-align:left;div{margin-bottom:16px;padding:12px;border-radius:var(--app-radius-sm);background:var(--app-peach-soft)}dt{margin-bottom:5px;color:var(--app-text-muted);font-size:12px}dd{margin:0;color:var(--app-text);line-height:1.55}}
:deep(.el-form-item__label){color:var(--app-text)}
:deep(.el-input__wrapper),:deep(.el-textarea__inner){color:var(--app-text);background:#fff;box-shadow:0 0 0 1px var(--app-border-strong) inset}
@media(max-width:860px){.character-create-page{padding:24px 20px 44px}.create-layout{grid-template-columns:1fr}.preview-card{position:static;grid-row:1}}
@media(max-width:600px){.character-create-page{padding-inline:16px}.page-header{align-items:flex-start;flex-direction:column}.creator-card{padding:20px 16px}.form-grid.two-columns{grid-template-columns:1fr}.type-row{align-items:flex-start;flex-direction:column;gap:10px}.color-editor{align-items:flex-start;flex-wrap:wrap}.color-editor small{width:100%;margin-left:0}.form-actions{flex-direction:column-reverse}.form-actions :deep(.el-button){width:100%;margin-left:0}}
</style>
