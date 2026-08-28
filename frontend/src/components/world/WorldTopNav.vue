<template>
  <nav class="world-nav">
    <div class="nav-inner">
      <button class="brand" type="button" @click="router.push('/home')"><span>◉</span> AI Character World</button>
      <div class="nav-links">
        <button type="button" @click="router.push('/home')">首页</button>
        <button type="button" @click="router.push('/characters')">角色</button>
        <button type="button" class="active" @click="router.push('/worlds')">世界</button>
        <button type="button" @click="router.push('/chat')">聊天</button>
      </div>
      <div class="nav-actions">
        <el-button v-if="route.path !== '/worlds/create'" type="primary" round @click="router.push('/worlds/create')">＋ 创建新世界</el-button>
        <span class="user-dot">{{ userStore.nickname?.charAt(0)?.toUpperCase() || 'U' }}</span>
      </div>
    </div>
  </nav>
</template>
<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
const router=useRouter();const route=useRoute();const userStore=useUserStore()
</script>
<style lang="scss" scoped>
.world-nav{position:relative;z-index:5;border-bottom:1px solid var(--world-border);background:rgba(255,255,255,.92);backdrop-filter:blur(14px)}.nav-inner{width:min(var(--world-content),calc(100% - 48px));height:74px;margin:auto;display:flex;align-items:center;justify-content:space-between;gap:24px}.brand{display:flex;align-items:center;gap:9px;color:var(--world-primary);font-size:21px;font-weight:800;white-space:nowrap}.brand span{font-size:25px}.nav-links{display:flex;align-items:center;gap:30px}.nav-links button{position:relative;padding:25px 2px;color:var(--world-ink);font-size:15px}.nav-links .active{color:var(--world-primary);font-weight:700}.nav-links .active:after{content:'';position:absolute;bottom:13px;left:50%;width:6px;height:6px;border-radius:50%;background:var(--world-primary);transform:translateX(-50%)}.nav-actions{display:flex;align-items:center;gap:14px}.user-dot{width:40px;height:40px;display:grid;place-items:center;border:4px solid #fff;border-radius:50%;color:var(--world-primary-strong);font-weight:800;background:var(--world-mint-soft);box-shadow:0 0 0 1px #e6ddd4}:deep(.el-button--primary){--el-button-bg-color:var(--world-primary);--el-button-border-color:var(--world-primary);--el-button-hover-bg-color:var(--world-primary-strong);--el-button-hover-border-color:var(--world-primary-strong);font-weight:700}@media(max-width:760px){.nav-inner{width:calc(100% - 28px);height:auto;min-height:66px;flex-wrap:wrap;padding:11px 0}.brand{font-size:17px}.nav-links{order:3;width:100%;justify-content:space-around;gap:8px}.nav-links button{padding:8px}.nav-links .active:after{bottom:0}.nav-actions .el-button{display:none}.user-dot{width:34px;height:34px}}
</style>
