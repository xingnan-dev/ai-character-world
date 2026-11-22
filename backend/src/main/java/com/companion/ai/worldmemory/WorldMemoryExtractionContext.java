package com.companion.ai.worldmemory;
public record WorldMemoryExtractionContext(String worldSnapshot,String participantSnapshots,String transcript,String existingMemory, java.util.Map<Long,String> sourceEvents) {
 public WorldMemoryExtractionContext(String world,String participants,String transcript,String memory) { this(world,participants,transcript,memory,java.util.Map.of()); }
}
