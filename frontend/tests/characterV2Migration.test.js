import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const router = source('../src/router/index.js')
const list = source('../src/views/CharacterList.vue')
const create = source('../src/views/CharacterCreate.vue')
const detail = source('../src/views/CharacterDetail.vue')
const personality = source('../src/views/PersonalityEdit.vue')
const pages = [list, create, detail, personality]

test('all four Character routes retain AppShell and Character navigation metadata', () => {
  for (const path of ['/characters','/character/create','/character/:id','/personality/edit/:avatarId']) assert.match(router, new RegExp(`path: '${path.replaceAll('/', '\\/')}'[\\s\\S]*?appShell: true[\\s\\S]*?navSection: 'characters'`))
  for (const page of pages) assert.doesNotMatch(page, /<nav|AppTopNav|home-nav/)
})

test('Character List preserves accessible filters and keyboard cards', () => {
  for (const label of ['全部','AI 角色','用户角色']) assert.match(list, new RegExp(label))
  assert.match(list, /role="group" aria-label="角色类型筛选"/)
  assert.match(list, /:aria-pressed=/)
  assert.match(list, /role="link"/)
  assert.match(list, /@keydown\.enter\.prevent/)
  assert.match(list, /@keydown\.space\.prevent/)
})

test('Character List preserves loading, retry, empty, create and identity behavior', () => {
  assert.match(list, /v-loading="loading"/)
  assert.match(list, /loadError/)
  assert.match(list, /重新加载/)
  assert.match(list, /characters\.length === 0/)
  assert.match(list, /创建第一个角色/)
  assert.match(list, /设为当前身份/)
  assert.match(list, /ElMessageBox|ElMessage/)
})

test('Character Create preserves parse, editable form and image lifecycle', () => {
  assert.match(create, /v-model="description"/)
  assert.match(create, /parseCharacter\(/)
  assert.match(create, /applyCharacterDraft\(form/)
  assert.match(create, /AI 解析失败，原始描述已保留/)
  assert.match(create, /buildCharacterCreatePayload\(form, description\.value\)/)
  assert.match(create, /createCharacterImageFlow/)
  assert.match(create, /imageFlow\.createAndConfirm/)
})

test('Character Create retains real editable semantic fields', () => {
  for (const field of ['characterType','name','age','identity','relationshipToUser','corePersonality','currentGoal','biography','speakingStyle','avatarColor']) assert.match(create, new RegExp(`form\\.${field}`))
  for (const field of ['values','likes','dislikes','interests','fears','secrets','behaviorTendencies']) assert.match(create, new RegExp(`key: '${field}'`))
})

test('Character Detail preserves full profile, edit, image confirm and delete confirmation', () => {
  for (const field of ['corePersonality','currentGoal','biography','speakingStyle','relationshipToUser','age']) assert.match(detail, new RegExp(`character\\.${field}`))
  assert.match(detail, /buildCharacterUpdatePayload\(form\)/)
  assert.match(detail, /confirmCharacterImage/)
  assert.match(detail, /ElMessageBox\.confirm/)
  assert.match(detail, /deleteCharacter\(characterId\(\)\)/)
})

test('Personality Edit retains exactly the five form bindings and guards', () => {
  for (const field of ['corePersonality','identity','languageStyle','hobbies','relationship']) assert.match(personality, new RegExp(`form\\.${field}`))
  assert.equal((personality.match(/<el-form-item/g) || []).length, 5)
  assert.match(personality, /getPersonalityByAvatarId\(avatarId\)/)
  assert.match(personality, /if \(!personality\.value \|\| saving\.value\) return/)
  assert.match(personality, /buildPersonalityUpdatePayload\(avatarId, personality\.value, form\)/)
})

test('Character pages consume shared V2 surfaces and tokens', () => {
  for (const page of pages) {
    assert.match(page, /app-surface--/)
    assert.match(page, /var\(--app-(?:text-primary|accent-primary|card-padding|surface-elevated)/)
    assert.doesNotMatch(page, /position:fixed|\.\w+-page\{[^}]*radial-gradient/i)
  }
})

test('Character pages retain desktop, tablet and mobile overflow guards', () => {
  for (const page of pages) {
    assert.match(page, /min-width:0/)
    assert.match(page, /@media\(max-width:1024px\)|@media \(max-width: 1024px\)/)
    assert.match(page, /@media\s*\(max-width:\s*(?:860|650|640|620|600)px\)/)
  }
})

test('Character pages expose reduced-motion and forced-colors fallbacks', () => {
  for (const page of pages) {
    assert.match(page, /prefers-reduced-motion:reduce/)
    assert.match(page, /forced-colors:active/)
  }
})

test('Character pages keep SimpleAvatar without remote or 3D presentation', () => {
  for (const page of pages) assert.match(page, /<SimpleAvatar/)
  const combined = pages.join('\n')
  assert.doesNotMatch(combined, /https?:\/\/|@font-face|url\(|AvatarRenderer|\.vrm|new THREE|WebGL|<canvas/i)
})

test('Character presentation introduces no raw stack or hidden reasoning labels', () => {
  const combined = pages.join('\n')
  assert.doesNotMatch(combined, /stackTrace|\.printStackTrace|at java\.|chain.of.thought|hidden reasoning|思维链|思考过程/i)
})
