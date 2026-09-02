<template>
  <div class="world-page">
    <header class="page-header">
      <div><span class="eyebrow">MY UNIVERSES</span><h1>我的世界</h1><p>你的所有创作宇宙都汇聚于此。每个世界，等待你继续书写。</p></div>
      <el-button class="world-primary-button" type="primary" size="large" @click="router.push('/worlds/create')">＋ 创建世界</el-button>
    </header>
    <main v-loading="worldStore.loading">
      <div v-if="worldStore.error && !worldStore.loading" class="retry-card"><p>{{ worldStore.error }}</p><el-button @click="load">重新加载</el-button></div>
      <el-empty v-else-if="!worldStore.loading && worldStore.list.length === 0" description="还没有创建世界">
        <el-button class="world-primary-button" type="primary" @click="router.push('/worlds/create')">描述第一个世界</el-button>
      </el-empty>
      <div v-else class="world-grid">
        <article v-for="world in worldStore.list" :key="world.id" class="world-card" :class="accentForWorld(world.id)" @click="open(world.id)">
          <div class="world-cover" aria-hidden="true"><span></span><i></i></div>
          <div class="card-body"><div class="card-top"><span class="scene">{{ world.scene || '未命名场景' }}</span><span>{{ visibleWorldParticipants(world).length }} / 4 位角色</span></div>
          <h2>{{ world.name }}</h2><p>{{ world.atmosphere || world.background || '等待你补充世界氛围' }}</p>
          <div class="card-actions"><button type="button" class="world-card-link" @click.stop="open(world.id)">查看详情 →</button><button type="button" class="world-card-delete" @click.stop="remove(world)">删除</button></div>
          </div>
        </article>
      </div>
    </main>
  </div>
</template>
<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useWorldStore } from '../stores/world'
import { accentForWorld, visibleWorldParticipants } from '../utils/worldBuilder'
const router=useRouter(); const worldStore=useWorldStore()
const load=()=>worldStore.loadList().catch(error=>console.error('Load worlds failed:',error))
const open=id=>router.push(`/worlds/${id}`)
async function remove(world){try{await ElMessageBox.confirm(`确定删除世界“${world.name}”吗？`,'删除世界',{type:'warning',confirmButtonText:'删除',cancelButtonText:'取消'})}catch{return}try{await worldStore.remove(world.id);ElMessage.success('世界已删除');await worldStore.loadList()}catch(error){console.error('Delete world failed:',error)}}
onMounted(()=>{worldStore.clearTransient();load()}); onUnmounted(()=>{worldStore.error=''})
</script>
<style lang="scss" scoped>
.world-page{min-height:100vh;color:var(--world-ink);background:radial-gradient(circle at 8% 32%,rgba(144,231,197,.2),transparent 25%),radial-gradient(circle at 92% 24%,rgba(255,171,135,.2),transparent 28%),var(--world-bg)}.page-header,main{width:min(var(--world-content),calc(100% - 48px));margin:auto}.page-header{display:flex;justify-content:space-between;align-items:flex-end;padding:64px 0 38px}.eyebrow{color:var(--world-primary);letter-spacing:2px;font-size:11px;font-weight:800}h1{margin:7px 0;font-size:44px;letter-spacing:-.03em}.page-header p,.world-card p{color:var(--world-muted)}.world-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:24px;padding-bottom:70px}.world-card{overflow:hidden;min-height:350px;border:1px solid var(--world-border);border-radius:var(--world-radius-lg);background:var(--world-surface);box-shadow:var(--world-shadow-sm);cursor:pointer;transition:.25s;&:hover{transform:translateY(-5px);box-shadow:var(--world-shadow-lg)}h2{margin:18px 0 9px;font-size:24px}p{min-height:50px;line-height:1.65}}.world-cover{position:relative;height:118px;overflow:hidden;background:var(--accent-soft)}.world-cover:before,.world-cover:after,.world-cover span,.world-cover i{content:'';position:absolute;border-radius:45% 55% 62% 38%}.world-cover:before{width:160px;height:130px;left:-20px;top:28px;background:var(--accent);opacity:.7;transform:rotate(18deg)}.world-cover:after{width:180px;height:150px;right:-30px;top:-55px;background:var(--world-sky);opacity:.3}.world-cover span{width:80px;height:80px;left:45%;top:20px;border:18px solid rgba(255,255,255,.55)}.world-cover i{width:34px;height:34px;right:22px;bottom:14px;background:rgba(255,255,255,.75)}.accent-sky{--accent:var(--world-sky);--accent-soft:var(--world-sky-soft)}.accent-mint{--accent:var(--world-mint);--accent-soft:var(--world-mint-soft)}.accent-peach{--accent:var(--world-peach);--accent-soft:var(--world-peach-soft)}.card-body{padding:22px}.card-top,.card-actions{display:flex;justify-content:space-between;align-items:center}.card-top{font-size:12px;color:var(--world-muted)}.scene{max-width:65%;overflow:hidden;padding:5px 9px;border-radius:999px;color:var(--world-primary-strong);text-overflow:ellipsis;white-space:nowrap;background:var(--world-primary-soft)}.card-actions{margin-top:24px;padding-top:16px;border-top:1px solid var(--world-border)}.world-card-link{color:var(--world-primary);font-weight:700}.world-card-delete{color:var(--world-danger);font-weight:700}.retry-card{text-align:center;padding:30px;border:1px solid #f3ceca;border-radius:var(--world-radius-md);color:var(--world-danger);background:#fff5f3}:deep(.world-primary-button.el-button--primary){--el-button-text-color:var(--app-text-inverse);--el-button-bg-color:var(--app-primary-strong);--el-button-border-color:var(--app-primary-strong);--el-button-hover-text-color:var(--app-text-inverse);--el-button-hover-bg-color:var(--app-primary);--el-button-hover-border-color:var(--app-primary);--el-button-active-text-color:var(--app-text-inverse);--el-button-active-bg-color:var(--app-primary-strong);--el-button-active-border-color:var(--app-primary-strong);--el-button-disabled-text-color:var(--app-text-inverse);--el-button-disabled-bg-color:#6f8f9b;--el-button-disabled-border-color:#6f8f9b;color:var(--app-text-inverse);font-weight:700}:deep(.world-primary-button.el-button--primary:hover),:deep(.world-primary-button.el-button--primary:focus-visible),:deep(.world-primary-button.el-button--primary.is-loading){color:var(--app-text-inverse)}:deep(.world-primary-button.el-button--primary:focus-visible){box-shadow:var(--app-focus-ring)}:deep(.world-primary-button.el-button--primary.is-disabled),:deep(.world-primary-button.el-button--primary:disabled){color:var(--app-text-inverse);opacity:1}@media(max-width:850px){.world-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:600px){.page-header,main{width:calc(100% - 32px)}.page-header{align-items:flex-start;gap:20px;padding:38px 0 28px;flex-direction:column}.world-grid{grid-template-columns:1fr}h1{font-size:34px}}
</style>
