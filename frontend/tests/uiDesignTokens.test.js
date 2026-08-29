import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const styles = readFileSync(new URL('../src/styles/index.scss', import.meta.url), 'utf8')

test('shared light design tokens cover the UI-0 foundations', () => {
  const required = [
    'bg', 'bg-cool', 'surface', 'surface-subtle', 'text', 'text-secondary', 'text-muted',
    'text-inverse', 'primary', 'primary-strong', 'primary-soft', 'sky', 'sky-soft', 'mint',
    'mint-soft', 'peach', 'peach-soft', 'lavender', 'lavender-soft', 'border',
    'border-strong', 'danger', 'danger-soft', 'warning', 'warning-soft', 'success',
    'success-soft', 'radius-sm', 'radius-md', 'radius-lg', 'radius-pill', 'shadow-sm',
    'shadow-lg', 'space-1', 'space-2', 'space-3', 'space-4', 'space-5', 'space-6',
    'space-8', 'space-10', 'space-12', 'space-16', 'focus-ring'
  ]
  for (const token of required) assert.match(styles, new RegExp(`--app-${token}:\\s*[^;]+;`))
})

test('World tokens are aliases of shared app tokens', () => {
  const aliases = {
    bg: 'bg', surface: 'surface', 'surface-blue': 'surface-subtle', ink: 'text',
    muted: 'text-secondary', primary: 'primary', 'primary-strong': 'primary-strong',
    'primary-soft': 'primary-soft', sky: 'sky', 'sky-soft': 'sky-soft', mint: 'mint',
    'mint-soft': 'mint-soft', peach: 'peach', 'peach-soft': 'peach-soft',
    'lavender-soft': 'lavender-soft', border: 'border', danger: 'danger',
    'radius-sm': 'radius-sm', 'radius-md': 'radius-md', 'radius-lg': 'radius-lg',
    'shadow-sm': 'shadow-sm', 'shadow-lg': 'shadow-lg'
  }
  for (const [world, app] of Object.entries(aliases)) {
    assert.match(styles, new RegExp(`--world-${world}:\\s*var\\(--app-${app}\\);`))
  }
})

test('legacy dark tokens remain unchanged during UI-0', () => {
  assert.match(styles, /--bg-page:\s*#060816;/i)
  assert.match(styles, /--text-primary:\s*#FFFFFF;/i)
  assert.match(styles, /--space-purple:\s*#7C5CFF;/i)
})
