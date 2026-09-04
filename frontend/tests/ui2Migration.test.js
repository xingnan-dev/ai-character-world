import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const router = source('../src/router/index.js')
const nav = source('../src/components/layout/AppTopNav.vue')
const pages = ['CharacterList', 'CharacterCreate', 'CharacterDetail', 'PersonalityEdit'].map(name => source(`../src/views/${name}.vue`))

test('all Character-domain routes use one AppShell and highlight Character navigation', () => {
  for (const path of ['/characters', '/character/create', '/character/:id', '/personality/edit/:avatarId']) {
    assert.match(router, new RegExp(`path: '${path.replaceAll('/', '\\/')}'[\\s\\S]*?appShell: true[\\s\\S]*?navSection: 'characters'`))
  }
  assert.match(nav, /activeSection === item\.section/)
  for (const page of pages) assert.doesNotMatch(page, /AppTopNav|app-top-nav|主导航/)
})

test('migrated pages use light app tokens, SimpleAvatar, and no legacy full-page navigation', () => {
  for (const page of pages) {
    assert.match(page, /var\(--app-/)
    assert.match(page, /<SimpleAvatar/)
    assert.doesNotMatch(page, /#060816|返回首页/)
  }
})

test('UI-2 pages have responsive single-column/mobile guards without horizontal overflow sources', () => {
  for (const page of pages) {
    assert.match(page, /@media\s*\(max-width:/)
    assert.match(page, /min-width:\s*0/)
    assert.match(page, /(?:width:\s*min\([^;]*100%|max-width:\s*100%|minmax\(0,\s*1fr\)|grid-template-columns:\s*1fr)/)
  }
})

test('UI-2 scope contains no remote image, v-html, or new 3D reference', () => {
  for (const page of [...pages, source('../src/components/ui/SimpleAvatar.vue')]) {
    assert.doesNotMatch(page, /v-html|https?:\/\/|AvatarRenderer|three|\.vrm/i)
  }
})

test('Personality keeps load/save guards and does not fabricate missing resources', () => {
  const personality = pages[3]
  assert.match(personality, /getPersonalityByAvatarId\(avatarId\)/)
  assert.match(personality, /if \(!personality\.value \|\| saving\.value\) return/)
  assert.match(personality, /updatePersonality\(payload\)/)
  assert.doesNotMatch(personality, /personality\.value\s*=\s*\{\}/)
})
