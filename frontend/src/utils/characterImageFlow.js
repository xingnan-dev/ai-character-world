export function createCharacterImageFlow({ generateImage, confirmImage, createCharacter, createRequestId }) {
  let generating = false
  let generationId = null
  let candidateImageUrl = ''
  let createdCharacterId = null

  return {
    get generating() { return generating },
    get generationId() { return generationId },
    get candidateImageUrl() { return candidateImageUrl },
    get createdCharacterId() { return createdCharacterId },

    async generate(prompt) {
      if (generating || !String(prompt || '').trim()) return null
      generating = true
      try {
        const response = await generateImage({ requestId: createRequestId(), prompt: prompt.trim() })
        const data = response.data || response
        if (data.status !== 'SUCCEEDED') throw new Error(data.error || '图片生成失败，请稍后重试')
        candidateImageUrl = data.imageUrl
        generationId = data.id
        return data
      } finally {
        generating = false
      }
    },

    async createAndConfirm(payload) {
      if (createdCharacterId == null) {
        const response = await createCharacter(payload)
        createdCharacterId = (response.data || response).id
      }
      if (generationId != null) {
        await confirmImage({ generationId, characterId: createdCharacterId })
      }
      return createdCharacterId
    }
  }
}
