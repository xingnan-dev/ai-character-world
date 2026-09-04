<template>
  <div class="builder-page">
    <header><button @click="router.push('/worlds')">← 返回世界列表</button><span>WORLD BUILDER</span><h1>创造一个世界</h1><p>先描述想象，再亲手确认每一条设定。</p><div class="flow"><b class="active">1</b><span>世界设定</span><i></i><b>2</b><span>角色阵容</span><i></i><b>3</b><span>完成</span></div></header>
    <main>
      <section class="panel prompt-panel">
        <div class="step"><b>1</b><div><h2>描述世界</h2><p>AI只生成可编辑的纯文本语义草稿。</p></div></div>
        <el-input v-model="description" type="textarea" :rows="5" maxlength="2000" show-word-limit placeholder="例如：一座永远下雨的赛博朋克城市，角色们在午夜侦探事务所相遇……" />
        <div class="prompt-actions"><el-button type="primary" :loading="parsing" :disabled="!description.trim()" @click="generateDraft">AI生成草稿</el-button><el-button @click="manualStart">跳过AI，手动填写</el-button></div>
        <el-alert v-if="parseFallback" :title="parseError || 'AI 解析失败，原始描述已保留；请手动填写或稍后重试。'" type="warning" :closable="false" show-icon />
      </section>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="panel semantic-panel">
        <div class="step"><b>2</b><div><h2>编辑世界设定</h2><p>这些字段会作为运行时语义，与未来视觉主题保持分离。</p></div></div>
        <el-form-item label="世界名称" prop="name"><el-input v-model="form.name" maxlength="100" show-word-limit /></el-form-item>
        <div class="two-columns"><el-form-item label="氛围"><el-input v-model="form.atmosphere" maxlength="500" /></el-form-item><el-form-item label="当前场景"><el-input v-model="form.scene" maxlength="1000" /></el-form-item></div>
        <el-form-item label="世界背景"><el-input v-model="form.background" type="textarea" :rows="4" maxlength="2000" show-word-limit /></el-form-item>
        <el-form-item label="世界规则"><el-input v-model="form.rules" type="textarea" :rows="4" maxlength="2000" show-word-limit /></el-form-item>
        <p v-if="form.sourceDescription" class="source-note">原始描述：{{ form.sourceDescription }}</p>
      </el-form>

      <WorldRosterEditor v-model="selectedCharacters" :characters="characters" />
      <div v-if="characterError" class="retry-row"><span>{{ characterError }}</span><el-button @click="loadCharacters">重试加载角色</el-button></div>
      <footer><el-button size="large" @click="router.push('/worlds')">取消</el-button><el-button type="primary" size="large" :loading="submitting" :disabled="!canSubmit" @click="submit">创建世界</el-button></footer>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCharacterList } from '../api/character'
import { useWorldStore } from '../stores/world'
import WorldRosterEditor from '../components/world/WorldRosterEditor.vue'
import { applyWorldDraft, buildParticipantPayload, buildWorldSemanticPayload, createWorldForm, eligibleAiCharacters } from '../utils/worldBuilder'

const router=useRouter(); const worldStore=useWorldStore(); const formRef=ref(null)
const description=ref(''); const form=reactive(createWorldForm()); const characters=ref([]); const selectedCharacters=ref([])
const parsing=ref(false); const submitting=ref(false); const parseFallback=ref(false); const parseError=ref(''); const characterError=ref('')
const rules={name:[{required:true,message:'请输入世界名称',trigger:'blur'}]}
const canSubmit=computed(()=>form.name.trim()&&selectedCharacters.value.length>=2&&selectedCharacters.value.length<=4&&!submitting.value)
async function loadCharacters(){characterError.value='';try{const response=await getCharacterList('AI');characters.value=eligibleAiCharacters(response.data||response)}catch(error){characters.value=[];characterError.value=error.message||'角色加载失败'}}
async function generateDraft(){if(parsing.value||!description.value.trim())return;parsing.value=true;parseFallback.value=false;parseError.value='';try{const draft=await worldStore.parse(description.value.trim());applyWorldDraft(form,draft,description.value)}catch(error){parseFallback.value=true;parseError.value=error?.message||'AI 解析失败，原始描述已保留；请手动填写或稍后重试。';form.sourceDescription=description.value.trim()}finally{parsing.value=false}}
function manualStart(){parseFallback.value=false;parseError.value='';form.sourceDescription=description.value.trim();document.querySelector('.semantic-panel')?.scrollIntoView({behavior:'smooth'})}
async function submit(){if(!canSubmit.value)return;const valid=await formRef.value?.validate().catch(()=>false);if(!valid)return;submitting.value=true;try{const world=await worldStore.create({...buildWorldSemanticPayload(form),participants:buildParticipantPayload(selectedCharacters.value)});ElMessage.success('世界创建成功');await router.push(`/worlds/${world.id}`)}catch(error){console.error('Create world failed:',error)}finally{submitting.value=false}}
onMounted(()=>{worldStore.clearTransient();loadCharacters()}); onUnmounted(()=>{worldStore.error='';worldStore.draft=null})
</script>

<style lang="scss" scoped>
.builder-page{min-height:100vh;color:var(--world-ink);background:radial-gradient(circle at 4% 15%,rgba(131,205,243,.26),transparent 28%),radial-gradient(circle at 95% 28%,rgba(255,171,135,.18),transparent 28%),var(--world-bg)}header,main{width:min(1040px,calc(100% - 48px));margin:auto}header{position:relative;padding:48px 0 30px}header button{margin-bottom:22px;color:var(--world-muted);font-weight:700}header>span{display:block;color:var(--world-primary);font-size:11px;font-weight:800;letter-spacing:2px}h1{margin:7px 0;font-size:42px;letter-spacing:-.03em}header p,.step p{color:var(--world-muted)}.flow{position:absolute;right:0;top:66px;display:flex;align-items:center;gap:9px}.flow b{width:29px;height:29px;display:grid;place-items:center;border-radius:50%;color:#7d8990;background:#e3edf2}.flow b.active{color:#fff;background:var(--world-primary)}.flow span{color:var(--world-muted);font-size:12px;font-weight:700}.flow i{width:34px;height:2px;background:#dce8ed}.panel{margin-bottom:22px;padding:30px;border:1px solid var(--world-border);border-radius:var(--world-radius-lg);background:var(--world-surface);box-shadow:var(--world-shadow-sm)}.prompt-panel{border-top:4px solid var(--world-mint)}.semantic-panel{border-top:4px solid var(--world-sky)}.step{display:flex;gap:14px;margin-bottom:20px}.step b{width:34px;height:34px;display:grid;place-items:center;border-radius:11px;color:#fff;background:var(--world-primary)}.step h2{margin:0 0 4px}.step p{margin:0}.prompt-actions,footer{display:flex;justify-content:flex-end;gap:10px;margin-top:15px}.two-columns{display:grid;grid-template-columns:1fr 1fr;gap:18px}.source-note{padding:13px;border-radius:var(--world-radius-sm);color:var(--world-muted);background:var(--world-sky-soft);white-space:pre-wrap}.retry-row{display:flex;justify-content:space-between;margin-top:12px;padding:13px;color:var(--world-danger)}footer{padding:12px 0 40px}:deep(.el-form-item__label){color:var(--world-ink);font-weight:700}:deep(.el-input__wrapper),:deep(.el-textarea__inner){color:var(--world-ink);background:#fbfdfe;box-shadow:0 0 0 1px var(--world-border) inset}:deep(.el-input__wrapper.is-focus),:deep(.el-textarea__inner:focus){box-shadow:0 0 0 1px var(--world-primary) inset,0 0 0 4px rgba(22,138,192,.12)}:deep(.el-button--primary){--el-button-bg-color:var(--world-primary);--el-button-border-color:var(--world-primary);--el-button-hover-bg-color:var(--world-primary-strong);--el-button-hover-border-color:var(--world-primary-strong);font-weight:700}@media(max-width:760px){header,main{width:calc(100% - 32px)}.flow{position:static;margin-top:20px}.flow span,.flow i{display:none}.panel{padding:20px}.two-columns{grid-template-columns:1fr}h1{font-size:34px}.prompt-actions,footer{flex-wrap:wrap}}
</style>
