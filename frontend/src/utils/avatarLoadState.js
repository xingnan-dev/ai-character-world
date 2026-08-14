export function createAvatarLoadState() {
  let sequence = 0
  let loadedUrl = ''
  let disposed = false

  return {
    begin(url) {
      if (disposed || !url || url === loadedUrl) return null
      return Object.freeze({ sequence: ++sequence, url })
    },
    isCurrent(request) {
      return Boolean(request) && !disposed && request.sequence === sequence
    },
    complete(request) {
      if (!this.isCurrent(request)) return false
      loadedUrl = request.url
      return true
    },
    fail(request) {
      return this.isCurrent(request)
    },
    getLoadedUrl() {
      return loadedUrl
    },
    dispose() {
      disposed = true
      sequence += 1
      loadedUrl = ''
    },
    isDisposed() {
      return disposed
    }
  }
}

export async function runAvatarLoad({ state, url, load, onStart, onLoaded, onError, onSettled }) {
  const request = state.begin(url)
  if (!request) return false

  onStart?.(request)
  try {
    await load(url)
    if (!state.complete(request)) return false
    onLoaded?.(request)
    return true
  } catch (error) {
    if (state.fail(request)) onError?.(error, request)
    return false
  } finally {
    if (state.isCurrent(request)) onSettled?.(request)
  }
}
