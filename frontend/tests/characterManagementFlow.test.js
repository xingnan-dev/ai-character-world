import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const apiSource = readFileSync(new URL('../src/api/character.js', import.meta.url), 'utf8')
const routerSource = readFileSync(new URL('../src/router/index.js', import.meta.url), 'utf8')
const listSource = readFileSync(new URL('../src/views/CharacterList.vue', import.meta.url), 'utf8')
const detailSource = readFileSync(new URL('../src/views/CharacterDetail.vue', import.meta.url), 'utf8')
const homeSource = readFileSync(new URL('../src/views/Home.vue', import.meta.url), 'utf8')

test('Character API supports list filtering, update and delete', () => {
  assert.match(apiSource, /getCharacterList\s*=\s*\(type\)/)
  assert.match(apiSource, /params:\s*type\s*\?\s*\{\s*type\s*\}/)
  assert.match(apiSource, /method:\s*'put'/)
  assert.match(apiSource, /method:\s*'delete'/)
})

test('authenticated Character list route and Home navigation entry exist', () => {
  assert.match(routerSource, /path:\s*'\/characters'/)
  assert.match(routerSource, /CharacterList\.vue/)
  assert.match(homeSource, /label:\s*'角色空间'[\s\S]*path:\s*'\/characters'/)
})

test('list loads current user Characters and sends AI or USER filters', () => {
  assert.match(listSource, /getCharacterList\(selectedType\.value\s*\|\|\s*undefined\)/)
  assert.match(listSource, /value:\s*'AI'/)
  assert.match(listSource, /value:\s*'USER'/)
  assert.match(listSource, /selectType\(option\.value\)/)
})

test('list has empty state and required card summaries', () => {
  assert.match(listSource, /characters\.length\s*===\s*0/)
  assert.match(listSource, /character\.avatarColor/)
  assert.match(listSource, /character\.identity/)
  assert.match(listSource, /character\.corePersonality/)
  assert.match(listSource, /character\.currentGoal/)
})

test('detail loads Character and displays the complete profile', () => {
  assert.match(detailSource, /getCharacterById\(id\)/)
  for (const field of ['values', 'likes', 'dislikes', 'interests', 'fears', 'secrets', 'behaviorTendencies']) {
    assert.match(detailSource, new RegExp(`key: '${field}'`))
  }
  assert.match(detailSource, /character\.biography/)
  assert.match(detailSource, /character\.relationshipToUser/)
  assert.match(detailSource, /character\.speakingStyle/)
})

test('detail edits through the update API and refreshes after save', () => {
  assert.match(detailSource, /updateCharacter\(characterId\(\), buildCharacterUpdatePayload\(form\)\)/)
  assert.match(detailSource, /await loadCharacter\(\)/)
  assert.match(detailSource, /editing\.value\s*=\s*false/)
})

test('delete requires confirmation and returns to Character list', () => {
  assert.match(detailSource, /ElMessageBox\.confirm/)
  assert.match(detailSource, /deleteCharacter\(characterId\(\)\)/)
  assert.match(detailSource, /router\.push\('\/characters'\)/)
})

test('management pages use letter avatars and never load a VRM renderer', () => {
  assert.match(listSource, /letter-avatar/)
  assert.match(detailSource, /letter-avatar/)
  assert.doesNotMatch(listSource, /AvatarRenderer|modelUrl|getModelUrlByName/)
  assert.doesNotMatch(detailSource, /AvatarRenderer|modelUrl|getModelUrlByName/)
})
