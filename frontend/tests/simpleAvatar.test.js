import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { getAvatarInitial, resolveAvatarColor, safeAvatarImageUrl, stableAvatarAppearance, stableAvatarColor } from '../src/utils/simpleAvatar.js'

const component = readFileSync(new URL('../src/components/ui/SimpleAvatar.vue', import.meta.url), 'utf8')

test('local and same-origin avatar images are accepted', () => {
  assert.equal(safeAvatarImageUrl('/images/avatar.png', 'https://example.test'), '/images/avatar.png')
  assert.equal(safeAvatarImageUrl('./avatar.png', 'https://example.test'), './avatar.png')
  assert.equal(safeAvatarImageUrl('https://example.test/avatar.png', 'https://example.test'), 'https://example.test/avatar.png')
})

test('unsafe and cross-origin avatar URLs are rejected', () => {
  for (const url of ['https://remote.test/avatar.png', '//remote.test/a.png', 'data:image/png;base64,x', 'blob:x', 'javascript:alert(1)', 'ftp://example.test/a']) {
    assert.equal(safeAvatarImageUrl(url, 'https://example.test'), '')
  }
})

test('initial fallback handles whitespace, unicode and empty names', () => {
  assert.equal(getAvatarInitial('  nova'), 'N')
  assert.equal(getAvatarInitial(' 小夏'), '小')
  assert.equal(getAvatarInitial(''), '角')
  assert.equal(getAvatarInitial(null), '角')
})

test('palette color is stable without random generation', () => {
  assert.equal(stableAvatarColor(42, 'Nova'), stableAvatarColor(42, 'Changed'))
  assert.equal(stableAvatarColor('', 'Nova'), stableAvatarColor('', 'Nova'))
  assert.doesNotMatch(readFileSync(new URL('../src/utils/simpleAvatar.js', import.meta.url), 'utf8'), /Math\.random/)
})

test('different role ids produce different but stable CSS person appearances', () => {
  const first = stableAvatarAppearance('character-101', '小夏')
  assert.deepEqual(first, stableAvatarAppearance('character-101', '改名后的小夏'))
  assert.notDeepEqual(first, stableAvatarAppearance('character-202', '小夏'))
  for (const key of ['hair', 'skin', 'clothes', 'hairStyle', 'eyeStyle']) assert.ok(key in first)
  assert.match(component, /simple-avatar__head/)
  assert.match(component, /simple-avatar__hair/)
  assert.match(component, /simple-avatar__eye/)
  assert.match(component, /simple-avatar__body/)
})

test('custom colors accept plain hex only and cannot load a CSS URL', () => {
  assert.equal(resolveAvatarColor('#83cdf3', 42, 'Nova'), '#83cdf3')
  assert.equal(resolveAvatarColor('url(https://remote.test/a.png)', 42, 'Nova'), stableAvatarColor(42, 'Nova'))
})

test('component falls back on image error and exposes textual type and status labels', () => {
  assert.match(component, /@error="handleImageError"/)
  assert.match(component, /failedImage\.value = safeImage\.value/)
  assert.match(component, /role="img"/)
  assert.match(component, /:aria-label="accessibleLabel"/)
  for (const label of ['AI', 'USER', '在线', '回复中', '异常', '离线']) assert.match(component, new RegExp(label))
})
