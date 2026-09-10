import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/components/home/SideMenu.vue', import.meta.url), 'utf8')

test('all side menu items pass the click event and support keyboard navigation', () => {
  assert.match(source, /v-for="\(item, index\) in items"[\s\S]*@click="handleClick\(item, \$event\)"/)
  assert.match(source, /@keydown\.enter\.prevent="handleClick\(item\)"/)
  assert.match(source, /@keydown\.space\.prevent="handleClick\(item\)"/)
})

test('navigation does not depend on a DOM event while ripple animation remains guarded', () => {
  assert.match(source, /const target = e\?\.currentTarget/)
  assert.match(source, /target && typeof target\.getBoundingClientRect === 'function'/)
  assert.match(source, /emit\('navigate', item\)[\s\S]*router\.push\(item\.path\)/)
  assert.doesNotMatch(source, /e\.currentTarget\.getBoundingClientRect/)
})

test('the default menu exposes only the four route-backed product destinations', () => {
  const paths = ['/home', '/characters', '/worlds', '/chat']
  for (const path of paths) assert.match(source, new RegExp(`path: '${path}'`))
  for (const path of ['/avatar/create', '/memory', '/avatars', '/settings']) {
    assert.doesNotMatch(source, new RegExp(`path: '${path}'`))
  }
  assert.equal((source.match(/path: '\//g) || []).length, paths.length)
  assert.match(source, /label: '世界', icon: '🌍', path: '\/worlds'/)
})
