export function createChatSendGuard() {
  const activeSessions = new Set()

  return {
    tryAcquire(sessionId) {
      if (sessionId == null || activeSessions.has(sessionId)) return false
      activeSessions.add(sessionId)
      return true
    },
    release(sessionId) {
      activeSessions.delete(sessionId)
    },
    isActive(sessionId) {
      return activeSessions.has(sessionId)
    },
    clear() {
      activeSessions.clear()
    }
  }
}
