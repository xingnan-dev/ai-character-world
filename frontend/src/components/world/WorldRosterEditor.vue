<template>
  <section class="roster-editor">
    <div class="section-heading">
      <div><span class="eyebrow">CAST</span><h2>选择参与角色</h2></div>
      <span class="counter" :class="{ valid: validCount }">{{ modelValue.length }} / 2～4</span>
    </div>
    <el-alert v-if="locked" title="该世界已有互动记录，角色阵容已冻结" type="warning" :closable="false" show-icon />
    <template v-else>
      <p class="hint">只展示当前可用的AI Character。无需VRM模型也可以加入世界。</p>
      <div v-if="characters.length" class="character-options">
        <button v-for="character in characters" :key="character.id" type="button" class="character-option"
          :class="{ selected: isSelected(character.id) }" :disabled="!isSelected(character.id) && modelValue.length >= 4"
          @click="toggle(character)">
          <span class="letter-avatar" :style="{ background: character.avatarColor || '#7c5cff' }">{{ character.name?.charAt(0) || '角' }}</span>
          <span><strong>{{ character.name }}</strong><small>{{ character.identity || character.corePersonality || 'AI角色' }}</small></span>
          <span class="check">{{ isSelected(character.id) ? '✓' : '+' }}</span>
        </button>
      </div>
      <el-empty v-else description="还没有可用的AI角色" :image-size="70" />
      <div v-if="modelValue.length" class="ordered-roster">
        <h3>出场顺序</h3>
        <div v-for="(character, index) in modelValue" :key="character.id" class="roster-row">
          <span class="order">{{ index + 1 }}</span>
          <span class="letter-avatar small" :style="{ background: character.avatarColor || '#7c5cff' }">{{ character.name?.charAt(0) || '角' }}</span>
          <strong>{{ character.name }}</strong>
          <div class="move-actions">
            <el-button circle size="small" :disabled="index === 0" aria-label="上移" @click="move(index, -1)">↑</el-button>
            <el-button circle size="small" :disabled="index === modelValue.length - 1" aria-label="下移" @click="move(index, 1)">↓</el-button>
            <el-button circle size="small" type="danger" plain aria-label="移除" @click="remove(index)">×</el-button>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { moveRosterItem } from '../../utils/worldBuilder'

const props = defineProps({
  characters: { type: Array, default: () => [] },
  modelValue: { type: Array, default: () => [] },
  locked: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])
const validCount = computed(() => props.modelValue.length >= 2 && props.modelValue.length <= 4)
const isSelected = id => props.modelValue.some(character => character.id === id)
function toggle(character) {
  if (isSelected(character.id)) emit('update:modelValue', props.modelValue.filter(item => item.id !== character.id))
  else if (props.modelValue.length < 4) emit('update:modelValue', [...props.modelValue, character])
}
function move(index, direction) { emit('update:modelValue', moveRosterItem(props.modelValue, index, direction)) }
function remove(index) { emit('update:modelValue', props.modelValue.filter((_, itemIndex) => itemIndex !== index)) }
</script>

<style lang="scss" scoped>
.roster-editor { padding: 28px; border: 1px solid var(--world-border); border-top: 4px solid var(--world-peach); border-radius: var(--world-radius-lg); color:var(--world-ink); background: var(--world-surface); box-shadow:var(--world-shadow-sm); }
.section-heading { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; h2{margin:4px 0 0;} }
.eyebrow { color:var(--world-primary); font-size:11px; font-weight:800; letter-spacing:2px; }
.counter { padding:6px 11px; border-radius:999px; color:var(--world-danger); background:#fff1ef; &.valid{color:#007054;background:var(--world-mint-soft);} }
.hint { margin:0 0 16px; color:var(--world-muted); }
.character-options { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; }
.character-option { display:flex; align-items:center; gap:12px; padding:13px; border:1px solid var(--world-border); border-radius:var(--world-radius-md); color:var(--world-ink); text-align:left; background:#fbfdfe; span:nth-child(2){display:flex;min-width:0;flex:1;flex-direction:column;} small{margin-top:3px;color:var(--world-muted);overflow:hidden;text-overflow:ellipsis;white-space:nowrap;} &.selected{border-color:var(--world-primary);background:var(--world-sky-soft);box-shadow:0 0 0 3px rgba(22,138,192,.1);} &:disabled{opacity:.45;cursor:not-allowed;filter:grayscale(.4);} }
.letter-avatar { width:42px;height:42px;display:grid;place-items:center;flex:0 0 auto;border-radius:13px;font-weight:700;color:#fff; &.small{width:34px;height:34px;border-radius:10px;} }
.check { color:var(--world-primary);font-size:20px; }
.ordered-roster { margin-top:22px; h3{margin-bottom:10px;} }
.roster-row { display:flex;align-items:center;gap:10px;padding:11px 13px;margin-top:8px;border:1px solid #dbe5e9;border-radius:var(--world-radius-md);background:var(--world-surface-blue); .order{width:22px;color:var(--world-primary);font-weight:800;} strong{flex:1;} }
.move-actions { display:flex;gap:5px; }
@media(max-width:680px){.character-options{grid-template-columns:1fr}.roster-editor{padding:18px}.roster-row{flex-wrap:wrap}.move-actions{margin-left:auto}}
</style>
