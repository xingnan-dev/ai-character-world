import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import * as Vue from 'vue'
import { compile } from '@vue/compiler-dom'
import { parse } from '@vue/compiler-sfc'
import { renderToString } from '@vue/server-renderer'

test('rendered Agent drawers and Goal dialog escape the isolated page stacking context', async () => {
  const { descriptor } = parse(readFileSync(new URL('../src/views/AgentWorkspace.vue', import.meta.url), 'utf8'))
  const render = new Function('Vue', compile(descriptor.template.content, { mode: 'function', prefixIdentifiers: true }).code)(Vue)
  const modals = []
  const modal = name => Vue.defineComponent({
    props: { appendToBody: Boolean },
    setup(props) { modals.push({ name, appendToBody: props.appendToBody }); return () => null }
  })
  const app = Vue.createSSRApp({
    render,
    data: () => ({
      agentStore: { listLoading: false, listError: '', runs: [], createLoading: false, createError: '' },
      AGENT_EMPTY_STATE: { title: 'Empty', description: 'No Runs' },
      runsDrawerOpen: true, detailDrawerOpen: true, composerOpen: true,
      visibleRun: null, selectedStep: null, selectedStepId: null,
      loadRuns() {}, openComposer() {}, chooseRun() {}
    })
  })
  app.component('el-button', { render: () => null })
  for (const name of ['AgentRunList', 'AgentPayloadBlock', 'AgentStepTimeline', 'AgentStepDetail', 'el-input']) app.component(name, { render: () => null })
  app.component('el-drawer', modal('drawer'))
  app.component('el-dialog', modal('dialog'))
  app.directive('loading', { getSSRProps: () => ({}) })
  await renderToString(app)
  assert.equal(modals.length, 3)
  assert.ok(modals.every(x => x.appendToBody === true))
  assert.equal(modals.filter(x => x.name === 'drawer').length, 2)
})
