import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const home = read('../src/views/Home.vue')
const chat = read('../src/views/Chat.vue')
const world = read('../src/views/WorldDetail.vue')

test('Home separates the bound USER identity from AI Characters', () => {
  assert.match(home, /userInfo\.currentUserCharacter/)
  assert.match(home, /filter\(item=>item\.characterType==='AI'\)/)
  assert.doesNotMatch(home, /useAvatarStore|fetchAvatarList/)
})

test('ordinary chats select AI Characters while legacy sessions no longer require VRM rendering', () => {
  assert.match(chat, /getCharacterList\('AI'\)/)
  assert.match(chat, /aiCharacters/)
  assert.match(chat, /<ChatSessionAvatar/)
  assert.doesNotMatch(chat, /AvatarRenderer|getAvatarById|useAvatarStore/)
})

test('World entry binds the current USER Character and keeps interaction routing', () => {
  assert.match(world, /setWorldUserCharacter/)
  assert.match(world, /currentUserCharacterId/)
  assert.match(world, /worldInteractionLocation/)
  assert.doesNotMatch(world, /router\.push\(['"]\/chat/)
})
