<template>
  <SimpleAvatar
    :name="presentation.name"
    :image-url="presentation.imageUrl"
    :avatar-color="presentation.avatarColor"
    :entity-key="presentation.entityKey"
    :type="showType ? presentation.type : ''"
    :size="size"
    shape="circle"
    fallback="initial"
  />
</template>

<script setup>
import { computed } from 'vue'
import SimpleAvatar from '../ui/SimpleAvatar.vue'
import { resolveMessagePresentation } from '../../utils/chatSessionPresentation'

const props = defineProps({
  session: { type: Object, default: null },
  role: { type: String, default: 'assistant' },
  user: { type: Object, default: () => ({}) },
  size: { type: [String, Number], default: 'sm' },
  showType: { type: Boolean, default: false }
})

const presentation = computed(() => resolveMessagePresentation(props.session, props.role, props.user))
</script>
