import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const apiSource = readFileSync(new URL('../src/api/character.js', import.meta.url), 'utf8')
const routerSource = readFileSync(new URL('../src/router/index.js', import.meta.url), 'utf8')
const createSource = readFileSync(new URL('../src/views/CharacterCreate.vue', import.meta.url), 'utf8')

test('Character API exposes parse, create and detail operations', () => {
  assert.match(apiSource, /url:\s*'\/characters\/parse'/)
  assert.match(apiSource, /url:\s*'\/characters'/)
  assert.match(apiSource, /url:\s*`\/characters\/\$\{id}`/)
})

test('router exposes authenticated create and detail pages', () => {
  assert.match(routerSource, /path:\s*'\/character\/create'/)
  assert.match(routerSource, /path:\s*'\/character\/:id'/)
  assert.match(routerSource, /CharacterCreate\.vue/)
  assert.match(routerSource, /CharacterDetail\.vue/)
})

test('create flow preserves description on parse failure and navigates after save', () => {
  assert.match(createSource, /parseError\.value\s*=\s*error\?\.message/)
  assert.doesNotMatch(createSource, /description\.value\s*=\s*''/)
  assert.match(createSource, /buildCharacterCreatePayload\(form, description\.value\)/)
  assert.match(createSource, /router\.push\(`\/character\/\$\{response\.data\.id}`\)/)
})

test('Character create page does not load AvatarRenderer or infer a VRM', () => {
  assert.doesNotMatch(createSource, /AvatarRenderer|avatarModels|getModelUrlByName|modelUrl/)
  assert.match(createSource, /<SimpleAvatar/)
})
