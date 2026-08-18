import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const chatSource = readFileSync(new URL('../src/views/Chat.vue', import.meta.url), 'utf8')

test('chat resolves the real avatar from current ChatSession avatarId', () => {
  assert.match(chatSource, /chatStore\.currentSession\?\.avatarId/)
  assert.match(chatSource, /getAvatarById\(avatarId\)/)
})

test('chat model URL comes only from the Avatar API response', () => {
  assert.match(chatSource, /sessionAvatar\.value\?\.modelUrl\?\.trim\(\)\s*\|\|\s*''/)
  assert.match(chatSource, /:model-url="currentAvatarModelUrl"/)
  assert.doesNotMatch(chatSource, /getModelUrlByName|avatarModels|DEFAULT_MODEL|nova\.vrm/i)
})

test('chat does not render AvatarRenderer without a real model URL', () => {
  assert.match(chatSource, /v-if="currentAvatarModelUrl"/)
  assert.match(chatSource, /当前会话没有可加载的3D模型/)
  assert.match(chatSource, /会话3D形象加载失败/)
})

test('avatar binding caches identical avatarIds and rejects stale responses', () => {
  assert.match(chatSource, /sessionAvatarCache = new Map\(\)/)
  assert.match(chatSource, /sessionAvatarCache\.get\(cacheKey\)/)
  assert.match(chatSource, /requestVersion !== sessionAvatarRequestVersion/)
  assert.match(chatSource, /String\(chatStore\.currentSession\?\.avatarId/)
})
