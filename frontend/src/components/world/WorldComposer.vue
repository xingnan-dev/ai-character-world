<template>
  <section class="composer">
    <div v-if="notice" class="notice" :class="{ busy: queueBusy }"><span>{{ notice }}</span><el-button v-if="canRecover" size="small" @click="$emit('recover')">恢复执行</el-button></div>
    <el-input
      :model-value="modelValue"
      type="textarea"
      :rows="3"
      maxlength="4000"
      show-word-limit
      resize="none"
      placeholder="你想在这个世界里说些什么？"
      :disabled="busy"
      @update:model-value="$emit('update:modelValue', $event)"
      @keydown.ctrl.enter.prevent="$emit('send')"
      @keydown.meta.enter.prevent="$emit('send')"
    />
    <div class="composer-actions"><span>{{ busy ? '请等待本轮角色依次回应' : 'Ctrl / ⌘ + Enter 发送' }}</span><el-button type="primary" :loading="sending" :disabled="busy || !modelValue.trim() || modelValue.trim().length > 4000" @click="$emit('send')">发送本轮</el-button></div>
  </section>
</template>

<script setup>
defineProps({ modelValue: { type: String, default: '' }, busy: Boolean, sending: Boolean, notice: String, queueBusy: Boolean, canRecover: Boolean })
defineEmits(['update:modelValue', 'send', 'recover'])
</script>

<style lang="scss" scoped>
.composer{position:sticky;bottom:14px;z-index:3;margin-top:18px;padding:16px;border:1px solid var(--world-border);border-radius:var(--world-radius-lg);background:rgba(255,255,255,.96);box-shadow:var(--world-shadow-lg);backdrop-filter:blur(12px)}.notice{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:11px;padding:9px 12px;border-radius:11px;color:var(--world-primary-strong);background:var(--world-primary-soft)}.notice.busy{color:#855315;background:#fff5dc}.composer-actions{display:flex;align-items:center;justify-content:space-between;margin-top:11px}.composer-actions span{color:var(--world-muted);font-size:12px}:deep(.el-textarea__inner){color:var(--world-ink);background:#fbfdfe;box-shadow:0 0 0 1px var(--world-border) inset}:deep(.el-button--primary){--el-button-bg-color:var(--world-primary);--el-button-border-color:var(--world-primary);--el-button-hover-bg-color:var(--world-primary-strong);--el-button-hover-border-color:var(--world-primary-strong)}@media(max-width:620px){.composer{bottom:8px;padding:12px}.notice{align-items:flex-start;flex-direction:column}.composer-actions span{max-width:55%}}
</style>
