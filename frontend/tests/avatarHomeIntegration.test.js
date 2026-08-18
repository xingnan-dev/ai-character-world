import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const homeSource = readFileSync(new URL('../src/views/Home.vue', import.meta.url), 'utf8')
const showSource = readFileSync(new URL('../src/components/home/AIAvatarShow.vue', import.meta.url), 'utf8')
const rendererSource = readFileSync(new URL('../src/components/AvatarRenderer.vue', import.meta.url), 'utf8')
const sceneSource = readFileSync(new URL('../src/three/AvatarScene.js', import.meta.url), 'utf8')

test('home presentation model preserves backend modelUrl and baseModel', () => {
  assert.match(homeSource, /modelUrl:\s*avatar\.modelUrl/)
  assert.match(homeSource, /baseModel:\s*avatar\.baseModel/)
})

test('home renderer receives only the selected avatar real modelUrl', () => {
  assert.match(showSource, /:model-url="avatar\.modelUrl"/)
  assert.match(showSource, /presentation="home"/)
  assert.doesNotMatch(showSource, /avatarModels|getModelUrlByName|DEFAULT_MODEL/)
  assert.doesNotMatch(showSource, /requestAnimationFrame/)
})

test('AvatarRenderer does not infer or default a model URL from avatar name', () => {
  assert.match(rendererSource, /props\.modelUrl\?\.trim\(\)\s*\|\|\s*''/)
  assert.doesNotMatch(rendererSource, /avatarModels|getModelUrlByName|DEFAULT_MODEL/)
})

test('AvatarScene drives and disposes behavior in its existing animation loop', () => {
  assert.match(sceneSource, /new AvatarBehaviorController\(vrm\)/)
  assert.match(sceneSource, /behaviorController\?\.update\(delta, elapsed\)/)
  assert.match(sceneSource, /this\._disposeBehaviorController\(\)/)
  assert.equal((sceneSource.match(/requestAnimationFrame\(/g) || []).length, 1)
})

test('home presentation is transparent, floorless and does not auto rotate', () => {
  assert.match(sceneSource, /this\.scene\.background = this\.isHomePresentation \? null/)
  assert.match(sceneSource, /if \(!this\.isHomePresentation\) \{[\s\S]*new THREE\.PlaneGeometry/)
  assert.match(sceneSource, /this\.controls\.autoRotate = !this\.isHomePresentation/)
})
