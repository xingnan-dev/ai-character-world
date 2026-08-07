export function personalityText(personality) {
  return personality?.corePersonality?.trim() || '未设置人格'
}

export function personalityIdentity(personality) {
  return personality?.identity?.trim() || '未设置人格'
}

export function personalityInterests(personality) {
  return personality?.hobbies?.trim() || '未设置人格'
}

export function personalityTags(personality) {
  if (!personality?.corePersonality) return ['未设置人格']
  return personality.corePersonality
    .split(/[、，,]/)
    .map(tag => tag.trim())
    .filter(Boolean)
}
