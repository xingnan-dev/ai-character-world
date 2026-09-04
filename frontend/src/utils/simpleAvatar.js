export const AVATAR_PALETTE = Object.freeze([
  '#8bc9e8', '#8eddbd', '#f4b293', '#b8a6e8', '#e7a8bc', '#a8d4a2', '#e8c67d', '#91b8df'
])

const DEFAULT_INITIAL = '角'

export function getAvatarInitial(name) {
  const normalized = typeof name === 'string' ? name.trim() : ''
  return Array.from(normalized)[0]?.toUpperCase() || DEFAULT_INITIAL
}

export function stableAvatarColor(entityKey, name, palette = AVATAR_PALETTE) {
  const source = String(entityKey ?? '').trim() || String(name ?? '').trim() || DEFAULT_INITIAL
  let hash = 2166136261
  for (const character of source) {
    hash ^= character.codePointAt(0)
    hash = Math.imul(hash, 16777619)
  }
  return palette[(hash >>> 0) % palette.length]
}

export function stableAvatarHash(entityKey, name) {
  const source = String(entityKey ?? '').trim() || String(name ?? '').trim() || DEFAULT_INITIAL
  let hash = 2166136261
  for (const character of source) {
    hash ^= character.codePointAt(0)
    hash = Math.imul(hash, 16777619)
  }
  return hash >>> 0
}

const HAIR_COLORS = Object.freeze(['#31475b', '#6b4936', '#755c96', '#263f3e', '#a9604e', '#4d526f'])
const SKIN_COLORS = Object.freeze(['#f6c9a8', '#efb78e', '#d99c72', '#b97855', '#8d5d45'])
const CLOTHES_COLORS = Object.freeze(['#168ac0', '#6d65c2', '#3b9b79', '#dc775a', '#557cae', '#b06993'])

export function stableAvatarAppearance(entityKey, name) {
  const hash = stableAvatarHash(entityKey, name)
  return {
    hash,
    hair: HAIR_COLORS[hash % HAIR_COLORS.length],
    skin: SKIN_COLORS[(hash >>> 5) % SKIN_COLORS.length],
    clothes: CLOTHES_COLORS[(hash >>> 10) % CLOTHES_COLORS.length],
    hairStyle: (hash >>> 15) % 3,
    eyeStyle: (hash >>> 18) % 2
  }
}

export function resolveAvatarColor(avatarColor, entityKey, name) {
  return typeof avatarColor === 'string' && /^#[0-9a-f]{3,8}$/i.test(avatarColor.trim())
    ? avatarColor.trim()
    : stableAvatarColor(entityKey, name)
}

export function safeAvatarImageUrl(imageUrl, origin) {
  if (typeof imageUrl !== 'string' || !imageUrl.trim()) return ''
  const candidate = imageUrl.trim()
  if (/^[a-z][a-z0-9+.-]*:/i.test(candidate) && !/^https?:\/\//i.test(candidate)) return ''
  if (candidate.startsWith('//') || candidate.startsWith('\\')) return ''

  const currentOrigin = origin || (typeof window !== 'undefined' ? window.location.origin : '')
  if (/^https?:\/\//i.test(candidate)) {
    if (!currentOrigin) return ''
    try {
      const parsed = new URL(candidate)
      return parsed.origin === currentOrigin ? parsed.href : ''
    } catch {
      return ''
    }
  }

  return /^[./]|^[a-zA-Z0-9_-]/.test(candidate) ? candidate : ''
}
