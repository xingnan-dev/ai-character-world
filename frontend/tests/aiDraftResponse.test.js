import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { AI_PARSE_TIMEOUT_MS } from '../src/utils/aiDraftResponse.js'

const characterApi = readFileSync(new URL('../src/api/character.js', import.meta.url), 'utf8')
const worldApi = readFileSync(new URL('../src/api/world.js', import.meta.url), 'utf8')
const characterPage = readFileSync(new URL('../src/views/CharacterCreate.vue', import.meta.url), 'utf8')
const worldPage = readFileSync(new URL('../src/views/WorldCreate.vue', import.meta.url), 'utf8')

test('AI parse calls have a dedicated timeout longer than 15 seconds', () => {
  assert.ok(AI_PARSE_TIMEOUT_MS > 15000)
  assert.match(characterApi, /timeout:\s*AI_PARSE_TIMEOUT_MS/)
  assert.match(worldApi, /timeout:\s*AI_PARSE_TIMEOUT_MS/)
})

test('frontend consumes the backend DTO and does not reparse raw LLM output', () => {
  assert.match(characterPage, /applyCharacterDraft\(form, response\.data\)/)
  assert.doesNotMatch(characterPage + worldPage, /normalizeAIDraftResponse|parseJsonObjectFromAI/)
})

test('parse failures retain the description and expose clear manual fallback text', () => {
  assert.doesNotMatch(characterPage + worldPage, /description\.value\s*=\s*''/)
  assert.match(characterPage, /原始描述已保留/)
  assert.match(worldPage, /form\.sourceDescription=description\.value\.trim\(\)/)
  assert.match(worldPage, /手动填写/)
})
