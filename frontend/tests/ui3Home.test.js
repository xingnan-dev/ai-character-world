import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const home = readFileSync(new URL('../src/views/Home.vue', import.meta.url), 'utf8')

test('Home uses the bound USER Character and lists AI Characters with a safe fallback', () => {
  assert.doesNotMatch(home, /avatarStore|fetchAvatarList/)
  assert.match(home, /getCharacterList\(\)/)
  assert.match(home, /<SimpleAvatar/)
  assert.match(home, /default-\$\{userStore\.userInfo\.id\|\|'user'\}/)
  assert.match(home, /currentUserCharacter/)
  assert.match(home, /characterType==='AI'/)
  assert.match(home, /尚未设置用户身份/)
})

test('Home stops loading the Three VRM presentation without deleting its dependencies', () => {
  assert.doesNotMatch(home, /AIAvatarShow|AvatarRenderer|SpaceBackground|modelUrl|\.vrm|three/i)
})

test('Home keeps welcome, core shortcuts and logout', () => {
  for (const text of ['欢迎回来', '/character/create', '/characters', '/worlds', '/chat', '退出登录']) assert.match(home, new RegExp(text))
  assert.match(home, /userStore\.logout\(\)/)
  assert.match(home, /var\(--app-/)
})
