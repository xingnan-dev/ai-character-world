import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const home = source('../src/views/Home.vue')
const router = source('../src/router/index.js')
const nav = source('../src/components/layout/AppTopNav.vue')
const shell = source('../src/components/layout/AppShell.vue')

test('Home route uses the shared AppShell and preserves authentication metadata', () => {
  assert.match(router, /path: '\/home'[\s\S]*?requiresAuth: true[\s\S]*?appShell: true[\s\S]*?navSection: 'home'/)
  assert.match(shell, /<AppTopNav/)
  assert.doesNotMatch(home, /<nav|home-nav|AppTopNav|退出登录|userStore\.logout/)
})

test('shared navigation remains the single five-route accessible navigation', () => {
  for (const path of ['/home','/characters','/worlds','/chat','/agent']) assert.match(nav, new RegExp(`to:'${path}'`))
  assert.match(nav, /aria-current/)
  assert.match(nav, /pathSection\.value/)
})

test('Home preserves welcome, nickname and USER Character identity fallbacks', () => {
  assert.match(home, /欢迎回来，\{\{ userStore\.nickname \|\| '创造者' \}\}/)
  assert.match(home, /currentUserCharacter/)
  assert.match(home, /尚未设置用户身份/)
  assert.match(home, /创建用户身份/)
  assert.match(home, /\/character\/create\?type=USER/)
  assert.match(home, /type="USER" status="online"/)
})

test('Home retains exactly four real quick actions and their routes', () => {
  for (const [title, path] of [['创建角色','/character/create'],['角色空间','/characters'],['我的世界','/worlds'],['普通聊天','/chat']]) {
    assert.match(home, new RegExp(`title:'${title}'[^}]*path:'${path.replaceAll('/', '\\/')}'`))
  }
  assert.match(home, /v-for="item in quickActions"/)
  assert.match(home, /:aria-label="`\$\{item\.title\}/)
})

test('Home keeps real Character loading, AI filtering, preview count and empty state', () => {
  assert.match(home, /getCharacterList\(\)/)
  assert.match(home, /userStore\.getUserInfoAction\(\)/)
  assert.match(home, /characterType==='AI'/)
  assert.match(home, /\.slice\(0,5\)/)
  assert.match(home, /你已经创建了 \$\{characterItems\.value\.filter/)
  assert.match(home, /还没有人物，先创建一个吧/)
  assert.match(home, /管理角色/)
  assert.match(home, /<SimpleAvatar/)
})

test('Home consumes shared V2 surfaces and does not duplicate full-page atmosphere', () => {
  for (const name of ['app-surface--glass','app-surface','app-surface--elevated','app-empty-state']) assert.match(home, new RegExp(name))
  for (const token of ['--app-content-width','--app-text-primary','--app-type-display','--app-section-gap','--app-card-padding','--app-motion-normal']) assert.match(home, new RegExp(token))
  assert.doesNotMatch(home, /\.home-page\{[^}]*radial-gradient|position:fixed|filter:blur/i)
})

test('Home has desktop, tablet and mobile overflow guards', () => {
  assert.match(home, /grid-template-columns:minmax\(0,1\.35fr\)/)
  assert.match(home, /@media\(max-width:1024px\)/)
  assert.match(home, /@media\(max-width:768px\)/)
  assert.match(home, /@media\(max-width:480px\)/)
  assert.match(home, /min-width:0/)
  assert.match(home, /overflow-wrap:anywhere/)
})

test('Home accessibility and motion fallbacks remain explicit', () => {
  assert.equal((home.match(/<h1/g) || []).length, 1)
  assert.match(home, /aria-labelledby="home-title"/)
  assert.match(home, /aria-hidden="true"/)
  assert.match(home, /@media\(prefers-reduced-motion:reduce\)/)
  assert.match(home, /@media\(forced-colors:active\)/)
  assert.match(home, /min-height:44px/)
})

test('Home migration adds no remote asset, 3D renderer or hidden reasoning label', () => {
  assert.doesNotMatch(home, /https?:\/\/|@font-face|url\(|AvatarRenderer|\.vrm|new THREE|WebGL|<canvas/i)
  assert.doesNotMatch(home, /chain.of.thought|hidden reasoning|思维链|思考过程/i)
})
