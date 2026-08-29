import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/views/WorldList.vue', import.meta.url), 'utf8')
const style = source.match(/<style[\s\S]*?>([\s\S]*?)<\/style>/)?.[1] || ''

test('World list primary actions use an explicit contrast-safe class', () => {
  assert.equal((source.match(/class="world-primary-button"/g) || []).length, 2)
  assert.match(style, /\.world-primary-button\.el-button--primary/)
  assert.match(style, /--el-button-text-color:var\(--app-text-inverse\)/)
  assert.match(style, /--el-button-bg-color:var\(--app-primary-strong\)/)
})

test('primary button text stays visible in interactive and busy states', () => {
  for (const state of ['hover', 'focus-visible', 'is-loading', 'is-disabled', 'disabled']) {
    assert.match(style, new RegExp(`world-primary-button[^}]*${state}|world-primary-button[^,{]*${state}`))
  }
  assert.match(style, /--el-button-hover-text-color:var\(--app-text-inverse\)/)
  assert.match(style, /--el-button-active-text-color:var\(--app-text-inverse\)/)
  assert.match(style, /--el-button-disabled-text-color:var\(--app-text-inverse\)/)
  assert.match(style, /box-shadow:var\(--app-focus-ring\)/)
})

test('card actions use explicit classes without a page-level bare button color rule', () => {
  assert.match(source, /class="world-card-link"/)
  assert.match(source, /class="world-card-delete"/)
  assert.match(style, /\.world-card-link\{color:var\(--world-primary\)/)
  assert.match(style, /\.world-card-delete\{color:var\(--world-danger\)/)
  assert.doesNotMatch(style, /(^|[;}])\s*button\s*\{[^}]*color:/m)
})
