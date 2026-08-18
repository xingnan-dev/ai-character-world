import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const chatSource = readFileSync(new URL('../src/views/Chat.vue', import.meta.url), 'utf8')
const rendererSource = readFileSync(new URL('../src/components/AvatarRenderer.vue', import.meta.url), 'utf8')
const sceneSource = readFileSync(new URL('../src/three/AvatarScene.js', import.meta.url), 'utf8')

test('chat reactions require an assistant active-to-completed transition', () => {
  assert.match(chatSource, /message\.status === CHAT_MESSAGE_STATUS\.COMPLETED/)
  assert.match(chatSource, /\[CHAT_MESSAGE_STATUS\.PENDING, CHAT_MESSAGE_STATUS\.STREAMING\]\.includes\(previousStatus\)/)
  assert.doesNotMatch(chatSource, /chatStore\.streaming[\s\S]{0,80}playReaction/)
})

test('chat deduplicates stable message reactions and isolates sessions', () => {
  assert.match(chatSource, /message\.requestId \?\? message\.id/)
  assert.match(chatSource, /reactedMessageIds\.has\(stableKey\)/)
  assert.match(chatSource, /\(\) => chatStore\.currentSessionId/)
  assert.match(chatSource, /previousAssistantStatuses\.clear\(\)/)
  assert.match(chatSource, /reactedMessageIds\.clear\(\)/)
})

test('send completion provides a deduplicated reaction fallback', () => {
  assert.match(chatSource, /await chatStore\.sendMessage\(text\)/)
  assert.match(chatSource, /playCompletedAssistantReaction\(completedAssistant\)/)
  assert.match(chatSource, /reactedMessageIds\.has\(stableKey\)/)
})

test('reaction crosses renderer and scene boundaries without direct controller access', () => {
  assert.match(chatSource, /nextTick\(\(\) =>/)
  assert.match(chatSource, /sessionId === chatStore\.currentSessionId/)
  assert.match(chatSource, /avatarRendererRef\.value\?\.playReaction\(reaction\)/)
  assert.match(rendererSource, /playReaction: \(reaction\) => sceneInstance\.value\?\.playReaction\(reaction\)/)
  assert.match(sceneSource, /return this\.behaviorController\.playReaction\(reaction\)/)
  assert.doesNotMatch(chatSource, /BehaviorController|expressionManager/)
})
