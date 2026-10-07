<template><div class="app-shell app-v2-theme"><AppTopNav /><main class="app-shell__content"><slot /></main></div></template>
<script setup>import AppTopNav from './AppTopNav.vue'</script>
<style lang="scss" scoped>
.app-shell{--app-content-width:1180px;position:relative;isolation:isolate;min-width:0;min-height:100dvh;display:flex;flex-direction:column;overflow-x:clip;color:var(--app-text-primary);background:linear-gradient(135deg,var(--app-atmosphere-warm-ivory),rgba(247,246,241,.94) 48%,rgba(248,241,246,.92))}
.app-shell::before{content:'';position:fixed;z-index:-2;inset:-14vmax;pointer-events:none;opacity:var(--app-aura-opacity);background:radial-gradient(ellipse at 12% 15%,var(--app-atmosphere-mint),transparent 28%),radial-gradient(ellipse at 37% 4%,var(--app-atmosphere-mist-blue),transparent 27%),radial-gradient(ellipse at 72% 9%,var(--app-atmosphere-soft-pink),transparent 29%),radial-gradient(ellipse at 94% 25%,var(--app-atmosphere-pale-violet),transparent 30%),radial-gradient(ellipse at 75% 92%,rgba(92,190,230,.16),transparent 32%);filter:blur(42px);transform:translateZ(0)}
.app-shell::after{content:'';position:fixed;z-index:-1;inset:0;pointer-events:none;opacity:.35;background-image:radial-gradient(circle at 16% 21%,rgba(255,255,255,.8) 0 1px,transparent 1.8px),radial-gradient(circle at 81% 16%,rgba(255,255,255,.7) 0 1px,transparent 2px);background-size:190px 190px,260px 260px}
.app-shell__content{position:relative;z-index:1;width:100%;min-width:0;flex:1 1 auto;display:flex;flex-direction:column}.app-shell__content :deep(> *){width:100%;min-width:0;min-height:0;flex:1 0 auto}
@media(max-width:1024px){.app-shell{--app-content-width:960px}.app-shell::before{filter:blur(32px)}}
@media(max-width:768px){.app-shell::before{inset:-8vmax;opacity:.58;filter:blur(24px)}.app-shell::after{opacity:.2}}
@media(prefers-reduced-motion:reduce){.app-shell::before,.app-shell::after{animation:none!important}}
@media(forced-colors:active){.app-shell{background:Canvas}.app-shell::before,.app-shell::after{display:none}}
</style>
