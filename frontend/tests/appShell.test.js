import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const app = source('../src/App.vue')
const router = source('../src/router/index.js')
const shell = source('../src/components/layout/AppShell.vue')
const nav = source('../src/components/layout/AppTopNav.vue')
const worldPages = ['WorldList', 'WorldCreate', 'WorldDetail', 'WorldInteraction'].map(name => source(`../src/views/${name}.vue`))

test('App renders each route component once through the meta-controlled shell branch', () => {
  assert.match(app, /RouterView v-slot="\{ Component, route \}"/)
  assert.match(app, /AppShell v-if="route\.meta\.appShell"/)
  assert.match(app, /component :is="Component" v-else/)
  assert.equal((app.match(/<RouterView/g) || []).length, 1)
})

test('World and UI-2 Character routes enable AppShell without changing unrelated routes', () => {
  for (const path of ['/worlds', '/worlds/create', '/worlds/:worldId', '/worlds/:worldId/interaction']) {
    assert.match(router, new RegExp(`path: '${path.replaceAll('/', '\\/')}'[\\s\\S]*?appShell: true[\\s\\S]*?navSection: 'worlds'`))
  }
  for (const path of ['/characters', '/character/create', '/character/:id', '/personality/edit/:avatarId']) {
    assert.match(router, new RegExp(`path: '${path.replaceAll('/', '\\/')}'[\\s\\S]*?appShell: true[\\s\\S]*?navSection: 'characters'`))
  }
  assert.equal((router.match(/appShell:\s*true/g) || []).length, 8)
  for (const view of ['Login.vue', 'Register.vue', 'Home.vue', 'Chat.vue', 'AvatarCreate.vue']) {
    assert.doesNotMatch(router, new RegExp(`${view.replace('.', '\\.')}[^\\n]*\\n[^\\n]*appShell: true`))
  }
})

test('shell uses dynamic viewport height and leaves content flow flexible', () => {
  assert.match(shell, /min-height:100dvh/)
  assert.match(shell, /app-shell__content/)
  assert.doesNotMatch(shell, /height:100vh|overflow:(?:hidden|auto|scroll)/)
})

test('top nav has four real destinations, active metadata fallback and no dead links', () => {
  for (const path of ['/home', '/characters', '/worlds', '/chat']) assert.match(nav, new RegExp(`to:'${path.replaceAll('/', '\\/')}'`))
  assert.match(nav, /route\.meta\.navSection\|\|pathSection\.value/)
  assert.match(nav, /aria-current/)
  assert.doesNotMatch(nav, /\/memory|\/avatars|\/settings/)
})

test('top nav exposes accessible navigation, user action and two-row mobile structure', () => {
  assert.match(nav, /<nav[^>]*aria-label="主导航"/)
  assert.match(nav, /<button[^>]*type="button"[^>]*:aria-label=/)
  assert.match(nav, /退出登录/)
  assert.match(nav, /:focus-visible/)
  assert.match(nav, /min-height:44px/)
  assert.match(nav, /@media\(max-width:760px\)[\s\S]*grid-template-rows:58px 48px[\s\S]*grid-row:2/)
})

test('World pages rely on exactly one shell navigation and keep correct back paths', () => {
  for (const page of worldPages) assert.doesNotMatch(page, /WorldTopNav|app-top-nav/)
  assert.match(worldPages[1], /router\.push\('\/worlds'\)/)
  assert.match(worldPages[2], /router\.push\('\/worlds'\)/)
  assert.match(worldPages[3], /router\.push\(`\/worlds\/\$\{routeWorldId\.value\}`\)/)
})

test('Interaction route cleanup remains attached to route unmount', () => {
  const interaction = worldPages[3]
  assert.match(interaction, /watch\(\(\) => route\.params\.worldId, initialize\)/)
  assert.match(interaction, /onUnmounted\(\(\) => \{[\s\S]*removeEventListener\('visibilitychange', handleVisibility\)[\s\S]*store\.dispose\(\)/)
})
