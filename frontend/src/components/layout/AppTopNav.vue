<template>
  <header class="app-top-nav">
    <div class="app-top-nav__inner">
      <RouterLink class="app-top-nav__brand" to="/home" aria-label="AI Character World 首页">
        <span class="app-top-nav__brand-mark" aria-hidden="true">◉</span><span>AI Character World</span>
      </RouterLink>
      <nav class="app-top-nav__links" aria-label="主导航">
        <RouterLink v-for="item in navigationItems" :key="item.section" :to="item.to" class="app-top-nav__link" :class="{ 'is-active': activeSection === item.section }" :aria-current="activeSection === item.section ? 'page' : undefined">{{ item.label }}</RouterLink>
      </nav>
      <el-dropdown trigger="click" placement="bottom-end">
        <button class="app-top-nav__user" type="button" :aria-label="`${displayName}的用户菜单`"><span aria-hidden="true">{{ userInitial }}</span></button>
        <template #dropdown><el-dropdown-menu><el-dropdown-item disabled>{{ displayName }}</el-dropdown-item><el-dropdown-item divided @click="logout">退出登录</el-dropdown-item></el-dropdown-menu></template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
const route=useRoute(),router=useRouter(),userStore=useUserStore()
const navigationItems=[{label:'首页',to:'/home',section:'home'},{label:'角色',to:'/characters',section:'characters'},{label:'世界',to:'/worlds',section:'worlds'},{label:'聊天',to:'/chat',section:'chat'}]
const pathSection=computed(()=>route.path==='/home'?'home':route.path.startsWith('/character')?'characters':route.path.startsWith('/worlds')?'worlds':route.path.startsWith('/chat')?'chat':'')
const activeSection=computed(()=>route.meta.navSection||pathSection.value)
const displayName=computed(()=>userStore.nickname||'用户')
const userInitial=computed(()=>displayName.value.trim().charAt(0).toUpperCase()||'U')
async function logout(){await userStore.logout();await router.push('/login')}
</script>

<style lang="scss" scoped>
.app-top-nav{position:relative;z-index:20;flex:0 0 auto;border-bottom:1px solid var(--app-border);color:var(--app-text);background:rgba(255,255,255,.92);backdrop-filter:blur(14px)}
.app-top-nav__inner{width:min(var(--app-content-width,1180px),calc(100% - 48px));min-height:74px;margin:0 auto;display:grid;grid-template-columns:minmax(max-content,1fr) auto minmax(40px,1fr);align-items:center;gap:24px}
.app-top-nav__brand{width:max-content;display:inline-flex;align-items:center;gap:9px;border-radius:var(--app-radius-sm);color:var(--app-primary-strong);font-size:21px;font-weight:800;white-space:nowrap}.app-top-nav__brand:hover{color:var(--app-primary)}.app-top-nav__brand-mark{color:var(--app-primary);font-size:25px}
.app-top-nav__links{display:flex;align-items:stretch;gap:22px;align-self:stretch}.app-top-nav__link{position:relative;min-width:48px;min-height:44px;display:grid;place-items:center;padding:0 6px;color:var(--app-text-secondary);font-size:15px;font-weight:650;touch-action:manipulation}.app-top-nav__link:hover,.app-top-nav__link.is-active{color:var(--app-primary-strong)}.app-top-nav__link.is-active:after{content:'';position:absolute;right:6px;bottom:13px;left:6px;height:3px;border-radius:var(--app-radius-pill);background:var(--app-primary)}
.app-top-nav__user{width:42px;height:42px;justify-self:end;display:grid;place-items:center;border:1px solid var(--app-border-strong);border-radius:50%;color:var(--app-primary-strong);font-weight:800;background:linear-gradient(135deg,var(--app-mint-soft),var(--app-sky-soft));box-shadow:var(--app-shadow-sm);touch-action:manipulation}
.app-top-nav__brand:focus-visible,.app-top-nav__link:focus-visible,.app-top-nav__user:focus-visible{outline:2px solid var(--app-primary);outline-offset:3px;box-shadow:var(--app-focus-ring)}
@media(max-width:760px){.app-top-nav__inner{width:calc(100% - 28px);min-height:112px;grid-template-columns:minmax(0,1fr) auto;grid-template-rows:58px 48px;gap:0 12px}.app-top-nav__brand{min-width:0;max-width:100%;font-size:17px}.app-top-nav__brand span:last-child{overflow:hidden;text-overflow:ellipsis}.app-top-nav__links{grid-column:1/-1;grid-row:2;width:100%;justify-content:space-between;gap:4px}.app-top-nav__link{flex:1 1 0;padding:0 4px}.app-top-nav__link.is-active:after{right:10px;bottom:3px;left:10px}.app-top-nav__user{width:38px;height:38px}}
</style>
