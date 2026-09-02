<template><div class="app-shell"><AppTopNav /><main class="app-shell__content"><slot /></main></div></template>
<script setup>import AppTopNav from './AppTopNav.vue'</script>
<style lang="scss" scoped>
.app-shell{--app-content-width:1180px;min-height:100dvh;display:flex;flex-direction:column;color:var(--app-text);background:radial-gradient(circle at 8% 18%,rgba(144,231,197,.2),transparent 28%),radial-gradient(circle at 92% 12%,rgba(131,205,243,.2),transparent 25%),var(--app-bg)}
.app-shell__content{width:100%;min-width:0;flex:1 1 auto;display:flex;flex-direction:column}.app-shell__content :deep(> *){width:100%;min-width:0;min-height:0;flex:1 0 auto}
</style>
