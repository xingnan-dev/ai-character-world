export function configureChatProxy(proxy) {
  proxy.on('proxyReq', (upstream, request, response) => {
    if (request.url?.split('?')[0] !== '/api/chat/stream') return
    // http-proxy otherwise leaves an upstream SSE request running after refresh/abort.
    const cancel = () => { if (!response.writableEnded) upstream.destroy() }
    response.once('close', cancel)
    upstream.once('close', () => response.removeListener('close', cancel))
  })
}
