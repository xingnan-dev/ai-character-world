import test from 'node:test'
import assert from 'node:assert/strict'
import { calculateAnchorAdjustment, classifyTimelineUpdate, createTimelineSignature, isNearBottom } from '../src/utils/worldTimelineScroll.js'

test('near bottom threshold and invalid numbers', () => {
  assert.equal(isNearBottom({ scrollTop: 700, clientHeight: 300, scrollHeight: 1000, threshold: 0 }), true)
  assert.equal(isNearBottom({ scrollTop: 699, clientHeight: 300, scrollHeight: 1000, threshold: 0 }), false)
  assert.equal(isNearBottom({ scrollTop: 'bad', clientHeight: 300, scrollHeight: 1000 }), false)
})
test('signature is lightweight and does not mutate input', () => {
  const items = [{ round: { id: 1 }, events: [{ id: 2, status: 'RUNNING', content: 'x' }] }]
  assert.equal(createTimelineSignature(items), '1[2:RUNNING:x:]')
  assert.deepEqual(items, [{ round: { id: 1 }, events: [{ id: 2, status: 'RUNNING', content: 'x' }] }])
})
test('classifies initial, history, tail append/update and unchanged', () => {
  const a = [{ round: { id: 2 }, events: [{ id: 3, status: 'RUNNING' }] }]
  assert.equal(classifyTimelineUpdate([], a), 'INITIAL')
  assert.equal(classifyTimelineUpdate(a, [{ round: { id: 1 }, events: [] }, ...a], { historyLoad: true }), 'HISTORY_PREPEND')
  assert.equal(classifyTimelineUpdate(a, [...a, { round: { id: 4 }, events: [] }]), 'TAIL_APPEND')
  assert.equal(classifyTimelineUpdate(a, [{ round: { id: 2 }, events: [{ id: 3, status: 'COMPLETED' }] }]), 'TAIL_UPDATE')
  assert.equal(classifyTimelineUpdate(a, a), 'UNCHANGED')
})
test('anchor adjustment compensates viewport movement', () => {
  assert.equal(calculateAnchorAdjustment(120, 80), 40)
  assert.equal(calculateAnchorAdjustment('bad', 80), 0)
})
