export const AVATAR_GENERATE_TIMEOUT_MS = 60000

export function createAvatarGenerationGuard() {
  let active = false

  return {
    tryAcquire() {
      if (active) return false
      active = true
      return true
    },
    release() {
      active = false
    },
    isActive() {
      return active
    }
  }
}
