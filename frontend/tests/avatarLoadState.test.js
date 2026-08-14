import test from 'node:test'
import assert from 'node:assert/strict'
import { createAvatarLoadState, runAvatarLoad } from '../src/utils/avatarLoadState.js'

function deferred() {
  let resolve
  let reject
  const promise = new Promise((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

test('same model url does not load again after success', async () => {
  const state = createAvatarLoadState()
  let calls = 0
  await runAvatarLoad({ state, url: 'nova.vrm', load: async () => { calls += 1 } })
  await runAvatarLoad({ state, url: 'nova.vrm', load: async () => { calls += 1 } })
  assert.equal(calls, 1)
})

test('avatar name changes do not reload when the resolved url stays unchanged', async () => {
  const state = createAvatarLoadState()
  let calls = 0
  const loadResolvedUrl = () => runAvatarLoad({
    state,
    url: 'nova.vrm',
    load: async () => { calls += 1 }
  })
  await loadResolvedUrl('first name')
  await loadResolvedUrl('second name')
  assert.equal(calls, 1)
})

test('a newer url wins and an older completion cannot overwrite it', async () => {
  const state = createAvatarLoadState()
  const nova = deferred()
  const sky = deferred()
  const loaded = []

  const first = runAvatarLoad({
    state,
    url: 'nova.vrm',
    load: () => nova.promise,
    onLoaded: ({ url }) => loaded.push(url)
  })
  const second = runAvatarLoad({
    state,
    url: 'sky.vrm',
    load: () => sky.promise,
    onLoaded: ({ url }) => loaded.push(url)
  })

  sky.resolve()
  await second
  nova.resolve()
  await first

  assert.deepEqual(loaded, ['sky.vrm'])
  assert.equal(state.getLoadedUrl(), 'sky.vrm')
})

test('a failed load reports error and never reports loaded', async () => {
  const state = createAvatarLoadState()
  let loaded = 0
  let errors = 0
  await runAvatarLoad({
    state,
    url: 'broken.vrm',
    load: async () => { throw new Error('broken') },
    onLoaded: () => { loaded += 1 },
    onError: () => { errors += 1 }
  })
  assert.equal(loaded, 0)
  assert.equal(errors, 1)
})

test('dispose invalidates a late result and prevents state updates', async () => {
  const state = createAvatarLoadState()
  const pending = deferred()
  let loaded = 0
  const result = runAvatarLoad({
    state,
    url: 'nova.vrm',
    load: () => pending.promise,
    onLoaded: () => { loaded += 1 }
  })
  state.dispose()
  pending.resolve()
  await result
  assert.equal(loaded, 0)
  assert.equal(state.getLoadedUrl(), '')
})
