import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = path => readFileSync(new URL(path, import.meta.url), 'utf8')

test('browser and authentication pages use the AIworld product identity', () => {
  const index = source('../index.html')
  const login = source('../src/views/Login.vue')
  const register = source('../src/views/Register.vue')
  const positioning = 'AI 角色世界与多角色互动聊天平台'

  assert.match(index, new RegExp(`<title>AIworld——${positioning}</title>`))
  for (const page of [login, register]) {
    assert.match(page, />AIworld</)
    assert.match(page, new RegExp(positioning))
  }
})

test('visible navigation brands use AIworld and keep the four primary destinations', () => {
  const topNav = source('../src/components/layout/AppTopNav.vue')
  const sideMenu = source('../src/components/home/SideMenu.vue')
  const home = source('../src/views/Home.vue')
  const chat = source('../src/views/Chat.vue')
  const avatarCompatibility = source('../src/views/AvatarCreate.vue')

  for (const view of [topNav, sideMenu, home, chat, avatarCompatibility]) assert.match(view, /> AIworld<|>AIworld</)
  for (const oldBrand of ['AI Character World', 'AI Companion', 'AI Space']) {
    for (const view of [topNav, sideMenu, home, chat, avatarCompatibility]) {
      assert.doesNotMatch(view, new RegExp(oldBrand))
    }
  }
})
