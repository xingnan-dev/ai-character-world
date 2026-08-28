import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const api = source('../src/api/world.js')
const store = source('../src/stores/world.js')
const router = source('../src/router/index.js')
const list = source('../src/views/WorldList.vue')
const create = source('../src/views/WorldCreate.vue')
const detail = source('../src/views/WorldDetail.vue')
const home = source('../src/views/Home.vue')
const nav = source('../src/components/world/WorldTopNav.vue')

test('World API covers parse, CRUD and roster replacement', () => {
  for (const path of ['/worlds/parse', '/worlds', '/participants']) assert.match(api, new RegExp(path.replace('/', '\\/')))
  for (const method of ["method: 'get'", "method: 'post'", "method: 'put'", "method: 'delete'"]) assert.match(api, new RegExp(method))
})

test('World store is independent and clears transient page state', () => {
  assert.match(store, /defineStore\('world'/)
  assert.match(store, /list:\s*\[\]/)
  assert.match(store, /currentWorld:\s*null/)
  assert.match(store, /draft:\s*null/)
  assert.match(store, /clearTransient/)
  assert.doesNotMatch(store, /useChatStore|ChatSession|ChatMessage/)
})

test('authenticated World routes and Home navigation exist', () => {
  for (const path of ["'/worlds'", "'/worlds/create'", "'/worlds/:worldId'"]) assert.match(router, new RegExp(path))
  assert.match(router, /WorldDetail\.vue[\s\S]*requiresAuth:\s*true/)
  assert.match(home, /label:\s*'我的世界'[\s\S]*path:\s*'\/worlds'/)
  assert.match(nav, /AI Character World/)
  assert.match(nav, /router\.push\('\/characters'\)/)
})

test('create flow supports AI fallback, selection, sorting and duplicate-submit guard', () => {
  assert.match(create, /AI草稿暂时不可用/)
  assert.match(create, /getCharacterList\('AI'\)/)
  assert.match(create, /buildParticipantPayload/)
  assert.match(create, /submitting\.value/)
  assert.doesNotMatch(create, /v-html|AvatarRenderer|\.vrm/)
})

test('list provides empty, retry, detail and confirmed deletion flows', () => {
  assert.match(list, /worldStore\.list\.length\s*===\s*0/)
  assert.match(list, /重新加载/)
  assert.match(list, /ElMessageBox\.confirm/)
  assert.match(list, /worldStore\.remove/)
})

test('detail reloads by route id and enforces locked roster UX', () => {
  assert.match(detail, /route\.params\.worldId/)
  assert.match(detail, /worldStore\.loadDetail\(id\)/)
  assert.match(detail, /world\.participantsLocked/)
  assert.match(detail, /角色阵容已冻结/)
  assert.match(detail, /互动页面将在下一阶段开放/)
  assert.doesNotMatch(detail, /execute|Timeline|EventSource|v-html/)
})
