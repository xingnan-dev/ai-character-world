import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const styles = source('../src/styles/index.scss')
const shell = source('../src/components/layout/AppShell.vue')
const nav = source('../src/components/layout/AppTopNav.vue')
const agent = source('../src/views/AgentWorkspace.vue')
const router = source('../src/router/index.js')
const agentLogic = [source('../src/stores/agent.js'), source('../src/api/agent.js'), source('../src/utils/agentPollingController.js')].join('\n')

test('V2 semantic color, atmosphere, shape, shadow, spacing, motion and type tokens exist', () => {
  for (const token of ['--app-page-background','--app-surface-elevated','--app-surface-glass','--app-text-primary','--app-accent-primary','--app-atmosphere-mist-blue','--app-atmosphere-pale-violet','--app-atmosphere-soft-pink','--app-atmosphere-warm-ivory','--app-shadow-card','--app-shadow-floating','--app-page-gutter','--app-motion-normal','--app-type-page-title']) assert.match(styles, new RegExp(token))
})

test('legacy dark tokens and World aliases remain available', () => {
  for (const token of ['--space-bg:','--space-purple:','--space-glass:','--world-bg: var\\(--app-bg\\)','--world-content: 1180px']) assert.match(styles, new RegExp(token))
})

test('surface system is opt-in under the V2 theme scope', () => {
  for (const name of ['app-surface','app-surface--elevated','app-surface--glass','app-surface--inset','app-surface--selected','app-status-chip','app-metadata','app-empty-state']) assert.match(styles, new RegExp(name))
  assert.doesNotMatch(styles, /^\.el-[^{]+\{/m)
})

test('App Shell owns a bounded low-cost CSS atmosphere without remote assets', () => {
  assert.match(shell, /app-shell app-v2-theme/)
  assert.match(shell, /--app-content-width:1180px/)
  assert.match(shell, /radial-gradient/)
  assert.match(shell, /overflow-x:clip/)
  assert.doesNotMatch(`${styles}\n${shell}`, /https?:\/\/|@font-face|url\(/i)
  assert.doesNotMatch(shell, /<canvas|WebGLRenderer|new THREE|requestAnimationFrame/i)
})

test('desktop, tablet and mobile shell guards reduce expensive effects', () => {
  assert.match(shell, /@media\(max-width:1024px\)/)
  assert.match(shell, /@media\(max-width:768px\)[\s\S]*filter:blur\(24px\)/)
  assert.match(styles, /@media \(max-width: 768px\)[\s\S]*--app-page-gutter: 16px/)
})

test('motion and high contrast fallbacks preserve usable boundaries', () => {
  assert.match(styles, /@media \(prefers-reduced-motion: reduce\)/)
  assert.match(shell, /@media\(prefers-reduced-motion:reduce\)/)
  assert.match(shell, /@media\(forced-colors:active\)/)
  assert.match(nav, /@media\(forced-colors:active\)/)
})

test('navigation keeps five real routes, route semantics and keyboard focus', () => {
  for (const path of ['/home','/characters','/worlds','/chat','/agent']) {
    assert.match(nav, new RegExp(`to:'${path}'`))
    assert.match(router, new RegExp(`path: '${path.replaceAll('/', '\\/')}'`))
  }
  assert.match(nav, /aria-current/)
  assert.match(nav, /:focus-visible/)
  assert.match(nav, /min-height:44px/)
  assert.match(nav, /is-active\{font-weight:800;background:/)
})

test('mobile navigation remains visible at narrow widths with no duplicate drawer', () => {
  assert.match(nav, /@media\(max-width:760px\)[\s\S]*grid-template-rows:58px 48px/)
  assert.match(nav, /@media\(max-width:420px\)/)
  assert.doesNotMatch(nav, /drawer|display:none/)
})

test('Agent consumes shared atmosphere tokens while its visual identity stays scoped', () => {
  for (const token of ['--app-page-background','--app-shadow-floating','--app-atmosphere-pale-violet','--app-atmosphere-mist-blue','--app-atmosphere-soft-pink','--app-atmosphere-mint']) assert.match(agent, new RegExp(token))
  assert.match(agent, /<style lang="scss" scoped>/)
  assert.match(agent, /\.agent-page\{--agent-bg:/)
  assert.match(agent, /agent-aura-drift/)
})

test('Agent behavior files remain free of visual-foundation concerns', () => {
  assert.doesNotMatch(agentLogic, /app-v2-theme|app-atmosphere|app-surface--glass|Visual System Foundation/i)
})

test('foundation introduces no hidden reasoning labels or new rendering engine usage', () => {
  const combined = `${styles}\n${shell}\n${nav}\n${agent}`
  assert.doesNotMatch(combined, /chain.of.thought|hidden reasoning|思维链|思考过程/i)
  assert.doesNotMatch(combined, /new THREE|WebGLRenderer|<canvas/i)
})
