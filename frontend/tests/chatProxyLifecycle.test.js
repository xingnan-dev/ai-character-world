import test from 'node:test'
import assert from 'node:assert/strict'
import http from 'node:http'
import { createServer } from 'vite'
import { configureChatProxy } from '../src/utils/chatProxyLifecycle.js'

test('chat proxy closes an unfinished upstream when browser disconnects before headers', async () => {
  let opened
  let closed
  const accepted = new Promise(resolve => { opened = resolve })
  const disconnected = new Promise(resolve => { closed = resolve })
  const upstream = http.createServer((req, res) => {
    res.on('close', closed)
    opened()
    // Provider has not sent its first token/response headers yet.
  })
  await new Promise(resolve => upstream.listen(0, '127.0.0.1', resolve))
  const vite = await createServer({ configFile: false, logLevel: 'silent', server: {
    host: '127.0.0.1', port: 0, proxy: { '/api': {
      target: `http://127.0.0.1:${upstream.address().port}`,
      configure: configureChatProxy
    } }
  } })
  let client
  try {
    await vite.listen()
    client = http.request({ host: '127.0.0.1', port: vite.httpServer.address().port,
      path: '/api/chat/stream', method: 'POST' })
    client.on('error', () => {})
    client.end('{}')
    await accepted
    client.destroy()
    let timer
    try {
      const result = await Promise.race([disconnected.then(() => true), new Promise(resolve => { timer = setTimeout(() => resolve(false), 3000) })])
      assert.equal(result, true, 'upstream must close without waiting for a first provider token')
    } finally { clearTimeout(timer) }
  } finally {
    client?.destroy()
    await vite.close()
    upstream.closeAllConnections()
    await new Promise(resolve => upstream.close(resolve))
  }
})
