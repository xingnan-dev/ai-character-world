import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const workspace = source('../src/views/AgentWorkspace.vue')
const timeline = source('../src/components/agent/AgentStepTimeline.vue')
const detail = source('../src/components/agent/AgentStepDetail.vue')
const payload = source('../src/components/agent/AgentPayloadBlock.vue')
const runList = source('../src/components/agent/AgentRunList.vue')

test('V2 theme is scoped to the Agent page instead of global tokens', () => {
  assert.match(workspace, /\.agent-page\{--agent-bg:/)
  assert.doesNotMatch(workspace, /:root\s*\{[^}]*--agent-/)
})
test('Agent atmosphere uses layered diffuse aura without a heavy rendering dependency', () => {
  assert.match(workspace, /radial-gradient/)
  assert.match(workspace, /filter:blur\(26px\)/)
  assert.doesNotMatch(workspace, /canvas|three|particle/i)
})
test('major Agent surfaces use scoped soft glass tokens', () => {
  assert.match(workspace, /--agent-blur/)
  assert.match(workspace, /backdrop-filter:blur/)
  assert.match(runList, /backdrop-filter:blur/)
})
test('timeline has semantic endpoint, duplicate echo and tool aura identities', () => {
  assert.match(timeline, /data-decision/)
  assert.match(timeline, /DUPLICATE_TOOL_CALL/)
  assert.match(timeline, /get_character_context/)
  assert.match(timeline, /search_character_memory/)
  assert.match(timeline, /get_world_context/)
})
test('stable payload surfaces keep JSON and text sharp', () => {
  assert.match(payload, /color:#27343c/)
  assert.match(payload, /white-space:pre-wrap/)
  assert.match(payload, /overflow:auto/)
})
test('prototype retains responsive drawers and lowers mobile effects', () => {
  assert.match(workspace, /agent-v2-drawer/)
  assert.match(workspace, /@media\(max-width:760px\)[\s\S]*--agent-blur:12px/)
})
test('reduced motion disables aura, status and interaction motion', () => {
  assert.match(workspace, /@media\(prefers-reduced-motion:reduce\)/)
  assert.match(timeline, /@media\(prefers-reduced-motion:reduce\)/)
  assert.match(workspace, /animation:none/)
})
test('visual presentation retains selection and alert accessibility semantics', () => {
  assert.match(timeline, /aria-selected/)
  assert.match(timeline, /aria-current/)
  assert.match(workspace, /role="alert"/)
  assert.match(workspace, /role="status"/)
})
test('Agent UI contains no Thinking or hidden reasoning labels', () => {
  assert.doesNotMatch(`${workspace}\n${timeline}\n${detail}`, />\s*Thinking\s*</i)
  assert.doesNotMatch(`${workspace}\n${timeline}\n${detail}`, /chain.of.thought|hidden reasoning|思维链|思考过程/i)
})
