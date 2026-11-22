import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import * as Vue from 'vue'
import { parse, compileScript } from '@vue/compiler-sfc'

// Compile the actual component, replacing only the avatar dependency. The native
// buttons, props, state branches and event handlers under test remain unchanged.
const moduleUrl = code => 'data:text/javascript,' + encodeURIComponent(code).replaceAll("'", '%27')
const avatarUrl = moduleUrl(`import { h } from '${import.meta.resolve('vue')}';export default {render(){return h('span')}}`)
const { descriptor } = parse(readFileSync(new URL('../src/components/chat/ChatSessionList.vue', import.meta.url), 'utf8'))
const code = compileScript(descriptor, { id: 'chat-list-test', inlineTemplate: true }).content
  .replace(/from (["'])vue\1/g, `from '${import.meta.resolve('vue')}'`)
  .replace("from '@element-plus/icons-vue'", `from '${import.meta.resolve('@element-plus/icons-vue')}'`)
  .replace("from './ChatSessionAvatar.vue'", `from '${avatarUrl}'`)
  .replace("from '../../utils/chatSessionPresentation'", `from '${new URL('../src/utils/chatSessionPresentation.js', import.meta.url).href}'`)
const { default: SessionList } = await import(moduleUrl(code))

const node = (tag, text = '') => ({ tag, text, props: {}, children: [], parent: null })
const renderer = Vue.createRenderer({
  createElement: tag => node(tag), createText: text => node('#text', text), createComment: text => node('#comment', text),
  setText: (n, text) => { n.text = text }, setElementText: (n, text) => { n.text = text; n.children = [] },
  patchProp: (n, key, _old, value) => { n.props[key] = value },
  insert(n, parent, anchor) {
    if (n.parent) n.parent.children.splice(n.parent.children.indexOf(n), 1)
    const i = anchor ? parent.children.indexOf(anchor) : -1
    parent.children.splice(i < 0 ? parent.children.length : i, 0, n); n.parent = parent
  },
  remove(n) { if (n.parent) n.parent.children.splice(n.parent.children.indexOf(n), 1); n.parent = null },
  parentNode: n => n.parent,
  nextSibling: n => n.parent?.children[n.parent.children.indexOf(n) + 1] || null
})
const all = n => [n, ...n.children.flatMap(all)]
const text = n => n.text + n.children.map(text).join('')
const session = (id, name) => ({ id, characterName: name, title: name, characterId: id })
function mount(overrides = {}) {
  const state = Vue.reactive({ sessions: [], currentSessionId: null, loading: false, error: '', user: {}, ...overrides })
  const events = [], root = node('root')
  const app = renderer.createApp({ render: () => Vue.h(SessionList, {
    ...state, onSelect: value => events.push(['select', value.id]), onDelete: value => events.push(['delete', value.id]),
    onCreate: () => events.push(['create']), onReload: () => events.push(['reload'])
  }) })
  app.component('el-button', { props: ['icon', 'type'], setup: (_props, { slots, attrs }) => () => Vue.h('button', { ...attrs, type: 'button' }, slots.default?.()) })
  app.component('el-icon', { setup: (_props, { slots }) => () => Vue.h('span', {}, slots.default?.()) })
  app.component('RouterLink', { props: ['to'], setup: (props, { slots }) => () => Vue.h('a', { href: props.to }, slots.default?.()) })
  app.mount(root)
  return { state, root, events, close: () => app.unmount() }
}

test('rendered session selection and sibling delete buttons emit the intended resource independently', () => {
  const view = mount({ sessions: [session(11, 'Frozen A'), session(12, 'Frozen B')] })
  try {
    const buttons = all(view.root).filter(n => n.tag === 'button' && n.props.class === 'chat-item')
    assert.equal(buttons.length, 2)
    assert.equal(buttons[1].props.type, 'button')
    buttons[1].props.onClick()
    const remove = all(view.root).find(n => n.props['aria-label'] === '删除与 Frozen A 的对话')
    remove.props.onClick()
    assert.deepEqual(view.events, [['select', 12], ['delete', 11]])
    assert.match(text(buttons[0]), /Frozen A/)
  } finally { view.close() }
})

test('selected session updates the accessible pressed state without losing the frozen names', async () => {
  const view = mount({ sessions: [session(11, 'Frozen A'), session(12, 'Frozen B')], currentSessionId: 11 })
  try {
    const buttons = () => all(view.root).filter(n => n.tag === 'button' && n.props.class === 'chat-item')
    assert.equal(buttons()[0].props['aria-pressed'], true)
    view.state.currentSessionId = 12; await Vue.nextTick()
    assert.equal(buttons()[0].props['aria-pressed'], false)
    assert.equal(buttons()[1].props['aria-pressed'], true)
    assert.match(text(view.root), /Frozen A/)
    assert.match(text(view.root), /Frozen B/)
  } finally { view.close() }
})

test('loading, failed and empty list states render actionable and accessible feedback', async () => {
  const view = mount({ loading: true })
  try {
    assert.ok(all(view.root).some(n => n.props.role === 'status'))
    view.state.loading = false; view.state.error = '受控加载失败'; await Vue.nextTick()
    assert.ok(all(view.root).some(n => n.props.role === 'alert'))
    all(view.root).find(n => n.tag === 'button' && text(n) === '重新加载').props.onClick()
    assert.deepEqual(view.events, [['reload']])
    view.state.error = ''; await Vue.nextTick()
    assert.match(text(view.root), /还没有对话/)
    all(view.root).find(n => n.tag === 'button' && text(n) === '创建第一段对话').props.onClick()
    assert.deepEqual(view.events, [['reload'], ['create']])
  } finally { view.close() }
})
