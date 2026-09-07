import { defineStore } from 'pinia'
import { getWorldById } from '../api/world'
import {
  createWorldRound,
  executeWorldRound,
  getActiveWorldRound,
  getWorldRound,
  getWorldRoundEvents,
  getWorldTimeline
} from '../api/worldInteraction'
import { createWorldInteractionStoreDefinition } from './worldInteractionStoreFactory.js'

export const useWorldInteractionStore = defineStore('worldInteraction', createWorldInteractionStoreDefinition({
  createWorldRound,
  executeWorldRound,
  getActiveWorldRound,
  getWorldById,
  getWorldRound,
  getWorldRoundEvents,
  getWorldTimeline
}))
