<template>
  <div class="home-page">
    <header class="home-nav">
      <RouterLink class="brand" to="/home"><span aria-hidden="true">◉</span> AI Character World</RouterLink>
      <nav aria-label="首页快捷导航"><RouterLink to="/characters">角色</RouterLink><RouterLink to="/worlds">世界</RouterLink><RouterLink to="/chat">聊天</RouterLink></nav>
      <button type="button" @click="logout">退出登录</button>
    </header>
    <main>
      <section class="welcome-card">
        <div class="welcome-copy"><span class="eyebrow">YOUR STORY SPACE</span><h1>欢迎回来，{{ userStore.nickname || '创造者' }}</h1><p>和你的角色继续聊天，或带他们进入一个全新的世界。</p><div class="welcome-actions"><el-button type="primary" size="large" @click="router.push('/chat')">开始聊天</el-button><el-button size="large" @click="router.push('/worlds/create')">创建世界</el-button></div></div>
        <div class="avatar-stage"><SimpleAvatar :name="homeAvatar.name" :image-url="homeAvatar.imageUrl" :avatar-color="homeAvatar.avatarColor" :entity-key="homeAvatar.key" :type="homeAvatar.type" status="online" :size="160" shape="rounded" /><strong>{{ homeAvatar.name }}</strong><span>{{ homeAvatar.caption }}</span></div>
      </section>
      <section class="quick-section">
        <div class="section-heading"><div><span class="eyebrow">QUICK START</span><h2>从这里开始</h2></div><span>选择一个空间，继续你的故事</span></div>
        <div class="quick-grid"><button v-for="item in quickActions" :key="item.path" type="button" class="quick-card" @click="router.push(item.path)"><span class="quick-icon" :class="item.accent" aria-hidden="true">{{ item.icon }}</span><span><strong>{{ item.title }}</strong><small>{{ item.description }}</small></span><b aria-hidden="true">→</b></button></div>
      </section>
      <section class="collection-card"><div><span class="eyebrow">MY CHARACTERS</span><h2>我的人物</h2><p>{{ collectionText }}</p></div><div class="avatar-row" aria-label="已创建的人物"><SimpleAvatar v-for="item in previewCharacters" :key="`${item.kind}-${item.id}`" :name="item.name" :image-url="item.imageUrl" :avatar-color="item.avatarColor" :entity-key="`${item.kind}-${item.id}`" :type="item.type" size="sm" shape="circle" /><span v-if="!previewCharacters.length" class="empty-note">还没有人物，先创建一个吧</span></div><el-button @click="router.push('/characters')">管理角色</el-button></section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCharacterList } from '../api/character'
import { useAvatarStore } from '../stores/avatar'
import { useUserStore } from '../stores/user'
import SimpleAvatar from '../components/ui/SimpleAvatar.vue'
const router=useRouter(),userStore=useUserStore(),avatarStore=useAvatarStore();const characters=ref([])
const quickActions=[{title:'创建角色',description:'用描述生成可编辑的人物设定',icon:'✦',accent:'mint',path:'/character/create'},{title:'角色空间',description:'查看和管理你创建的角色',icon:'☺',accent:'sky',path:'/characters'},{title:'我的世界',description:'创建世界并安排互动阵容',icon:'◎',accent:'peach',path:'/worlds'},{title:'普通聊天',description:'与当前 AI 伙伴单独对话',icon:'◇',accent:'lavender',path:'/chat'}]
const avatarItems=computed(()=>avatarStore.avatarList.map(item=>({...item,kind:'avatar',type:'AI',imageUrl:item.imageUrl||item.thumbnailUrl||'',avatarColor:item.avatarColor||''})))
const characterItems=computed(()=>characters.value.map(item=>({...item,kind:'character',type:item.characterType})))
const createdPeople=computed(()=>[...avatarItems.value,...characterItems.value]);const previewCharacters=computed(()=>createdPeople.value.slice(0,5))
const homeAvatar=computed(()=>{const selected=avatarItems.value[avatarStore.currentAvatarIndex]||characterItems.value[0];if(selected)return{...selected,key:`${selected.kind}-${selected.id}`,caption:selected.identity||'你的专属人物'};return{key:`default-${userStore.userInfo.id||'user'}`,name:userStore.nickname||'我的人物',imageUrl:userStore.avatarUrl||'',avatarColor:'',type:'USER',caption:'创建角色后，这里会展示你的专属形象'}})
const collectionText=computed(()=>createdPeople.value.length?`你已经创建了 ${createdPeople.value.length} 个人物。`:'你的角色与 Avatar 会出现在这里。')
async function logout(){await userStore.logout();await router.push('/login')}
onMounted(async()=>{const results=await Promise.allSettled([avatarStore.fetchAvatarList(),getCharacterList()]);if(results[1].status==='fulfilled')characters.value=results[1].value?.data||results[1].value||[]})
</script>

<style lang="scss" scoped>
.home-page{min-height:100vh;color:var(--app-text);background:radial-gradient(circle at 10% 8%,rgba(144,231,197,.34),transparent 26%),radial-gradient(circle at 92% 16%,rgba(131,205,243,.34),transparent 27%),radial-gradient(circle at 75% 90%,rgba(255,171,135,.2),transparent 27%),var(--app-bg)}
.home-nav{height:72px;display:grid;grid-template-columns:1fr auto 1fr;align-items:center;gap:24px;padding:0 max(24px,calc((100vw - 1180px)/2));border-bottom:1px solid rgba(204,216,220,.78);background:rgba(255,255,255,.84);backdrop-filter:blur(16px)}.brand{display:flex;align-items:center;gap:9px;color:var(--app-text);font-size:17px;font-weight:800}.brand span{color:var(--app-primary)}.home-nav nav{display:flex;gap:30px}.home-nav nav a{color:var(--app-text-secondary);font-weight:700}.home-nav nav a:hover{color:var(--app-primary-strong)}.home-nav>button{justify-self:end;min-height:42px;padding:0 16px;border:1px solid var(--app-border);border-radius:var(--app-radius-sm);color:var(--app-text-secondary);background:#fff}
main{width:min(1180px,calc(100% - 48px));margin:auto;padding:42px 0 64px}.welcome-card{min-height:360px;display:grid;grid-template-columns:1.35fr .65fr;align-items:center;gap:40px;padding:48px;border:1px solid var(--app-border);border-radius:var(--app-radius-lg);background:rgba(255,255,255,.92);box-shadow:var(--app-shadow-lg);overflow:hidden}.eyebrow{color:var(--app-primary-strong);font-size:11px;font-weight:850;letter-spacing:1.8px}.welcome-copy h1{max-width:680px;margin:10px 0 14px;font-size:clamp(34px,5vw,54px);line-height:1.08;letter-spacing:-.04em}.welcome-copy p,.section-heading>span,.collection-card p{color:var(--app-text-secondary);font-size:16px;line-height:1.7}.welcome-actions{display:flex;gap:12px;margin-top:28px}:deep(.el-button){min-height:44px;border-radius:var(--app-radius-sm);font-weight:750}:deep(.el-button--primary){--el-button-bg-color:var(--app-primary);--el-button-border-color:var(--app-primary);--el-button-hover-bg-color:var(--app-primary-strong);--el-button-hover-border-color:var(--app-primary-strong)}
.avatar-stage{position:relative;min-height:280px;display:flex;flex-direction:column;align-items:center;justify-content:center;border-radius:44% 56% 52% 48%;background:linear-gradient(145deg,var(--app-sky-soft),var(--app-mint-soft));box-shadow:inset 0 0 0 1px rgba(131,205,243,.42)}.avatar-stage::before,.avatar-stage::after{content:'';position:absolute;border-radius:50%}.avatar-stage::before{width:48px;height:48px;right:8%;top:6%;background:var(--app-peach-soft)}.avatar-stage::after{width:28px;height:28px;left:10%;bottom:13%;background:var(--app-lavender-soft)}.avatar-stage strong{margin-top:18px;font-size:20px}.avatar-stage>span{margin-top:5px;color:var(--app-text-secondary);text-align:center}
.quick-section{margin-top:38px}.section-heading{display:flex;align-items:end;justify-content:space-between;margin-bottom:17px}.section-heading h2,.collection-card h2{margin:5px 0 0;font-size:27px}.quick-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:15px}.quick-card{min-height:150px;display:flex;align-items:flex-start;gap:14px;padding:22px;text-align:left;border:1px solid var(--app-border);border-radius:var(--app-radius-md);background:#fff;box-shadow:var(--app-shadow-sm)}.quick-card:hover{transform:translateY(-3px);border-color:var(--app-sky);box-shadow:var(--app-shadow-lg)}.quick-card>span:nth-child(2){display:flex;flex:1;flex-direction:column}.quick-card strong{font-size:17px}.quick-card small{margin-top:8px;color:var(--app-text-secondary);line-height:1.5}.quick-card>b{color:var(--app-primary);font-size:18px}.quick-icon{width:42px;height:42px;display:grid;place-items:center;flex:0 0 auto;border-radius:13px;font-size:21px}.quick-icon.mint{background:var(--app-mint-soft)}.quick-icon.sky{background:var(--app-sky-soft)}.quick-icon.peach{background:var(--app-peach-soft)}.quick-icon.lavender{background:var(--app-lavender-soft)}
.collection-card{margin-top:24px;display:grid;grid-template-columns:1fr auto auto;align-items:center;gap:28px;padding:25px 28px;border:1px solid var(--app-border);border-radius:var(--app-radius-md);background:#fff;box-shadow:var(--app-shadow-sm)}.avatar-row{display:flex;align-items:center}.avatar-row :deep(.simple-avatar){margin-left:-8px}.avatar-row :deep(.simple-avatar:first-child){margin-left:0}.empty-note{color:var(--app-text-muted)}
@media(max-width:900px){.quick-grid{grid-template-columns:1fr 1fr}.welcome-card{padding:34px}.collection-card{grid-template-columns:1fr auto}.avatar-row{grid-column:1/-1;grid-row:2}}@media(max-width:650px){.home-nav{height:auto;grid-template-columns:1fr auto;padding:15px 18px}.home-nav nav{grid-row:2;grid-column:1/-1;justify-content:space-between}.home-nav>button{grid-column:2;grid-row:1}main{width:calc(100% - 32px);padding-top:24px}.welcome-card{grid-template-columns:1fr;padding:27px 21px}.avatar-stage{grid-row:1;min-height:245px}.quick-grid{grid-template-columns:1fr}.section-heading{align-items:flex-start;flex-direction:column;gap:5px}.collection-card{grid-template-columns:1fr}.collection-card :deep(.el-button){justify-self:start}.avatar-row{grid-row:auto}.welcome-actions{flex-wrap:wrap}}
</style>
