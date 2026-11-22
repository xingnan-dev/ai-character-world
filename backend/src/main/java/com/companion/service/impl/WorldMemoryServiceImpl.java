package com.companion.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;import com.companion.ai.worldmemory.*;import com.companion.dto.response.WorldResponse;import com.companion.entity.*;import com.companion.entity.enums.WorldRoundStatus;import com.companion.mapper.*;import com.companion.service.*;import lombok.RequiredArgsConstructor;import org.springframework.dao.DuplicateKeyException;import org.springframework.stereotype.Service;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.time.LocalDateTime;import java.util.*;import java.util.stream.Collectors;
@Service @RequiredArgsConstructor public class WorldMemoryServiceImpl implements WorldMemoryService{
 private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(WorldMemoryServiceImpl.class);
 static final int MAX_MEMORIES=10,MAX_CHARS=6000,MAX_ATTEMPTS=2;private final WorldMemoryMapper memories;private final CharacterWorldService worlds;private final WorldRoundMapper rounds;private final WorldEventMapper events;private final WorldParticipantMapper participants;private final LlmWorldMemoryExtractor extractor;
 public String getRelevantMemory(Long u,Long w,String q){worlds.get(u,w);String terms=q==null?"":q.toLowerCase();StringBuilder out=new StringBuilder();memories.selectList(new QueryWrapper<WorldMemory>().eq("user_id",u).eq("world_id",w)).stream().sorted(Comparator.<WorldMemory>comparingInt(m->score(m,terms)).reversed().thenComparing(WorldMemory::getUpdateTime,Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(WorldMemory::getId,Comparator.nullsLast(Comparator.reverseOrder()))).limit(MAX_MEMORIES).map(this::renderMemory).forEach(s->{if(out.length()+s.length()+1<=MAX_CHARS)out.append(s).append('\n');});return out.toString();}
 public void extractAndStore(Long u,Long w,Long r) {
  WorldResponse current=worlds.get(u,w);
  WorldRound round=rounds.selectOne(new QueryWrapper<WorldRound>().eq("id",r).eq("world_id",w));
  if(round==null||!WorldRoundStatus.COMPLETED.name().equals(round.getStatus()))return;
  long started=System.nanoTime();
  LOG.info("WORLD_MEMORY_STARTED userId={} worldId={} roundId={}",u,w,r);
  try {
   String world=round.getWorldSnapshot()!=null?round.getWorldSnapshot():"LEGACY_FALLBACK:"+current.getName()+"\n"+current.getBackground()+"\n"+current.getRules()+"\n"+current.getAtmosphere()+"\n"+current.getScene();
   Map<Long,String> sources=new LinkedHashMap<>();
   String transcript=events.selectList(new QueryWrapper<WorldEvent>().eq("round_id",r).orderByAsc("sequence_no").orderByAsc("id")).stream()
    .filter(e->e.getId()!=null&&"COMPLETED".equals(e.getStatus())&&Set.of("USER_MESSAGE","AI_MESSAGE").contains(e.getEventType())&&e.getContent()!=null&&!e.getContent().isBlank())
    .map(e->{sources.put(e.getId(),e.getContent());return "Event "+e.getId()+" ["+e.getEventType()+"]: "+e.getContent();}).collect(Collectors.joining("\n"));
   String roster=participants.selectList(new QueryWrapper<WorldParticipant>().eq("world_id",w).orderByAsc("display_order")).stream().map(WorldParticipant::getCharacterSnapshot).filter(Objects::nonNull).collect(Collectors.joining("\n"));
   var candidates=extractor.extract(new WorldMemoryExtractionContext(world,roster,transcript,getRelevantMemory(u,w,round.getUserInput()),Map.copyOf(sources)));
   for(WorldMemoryCandidate c:candidates)consolidate(u,w,r,c);
   LOG.info("WORLD_MEMORY_COMPLETED userId={} worldId={} roundId={} acceptedCount={} elapsedMs={}",u,w,r,candidates.size(),(System.nanoTime()-started)/1_000_000);
  } catch(RuntimeException error) {
   // IDs and exception class only: never log prompts, content or provider error messages.
   LOG.warn("WORLD_MEMORY_FAILED userId={} worldId={} roundId={} errorCategory={} elapsedMs={}",u,w,r,error.getClass().getSimpleName(),(System.nanoTime()-started)/1_000_000);
   throw error;
  }
 }

 void consolidate(Long u,Long w,Long r,WorldMemoryCandidate c){String hash=hash(c.type().name()+"\n"+normalize(summaryForHash(c.content())));WorldMemory exact=findHash(u,w,c.type().name(),hash);if(exact!=null)return;for(int attempt=0;attempt<MAX_ATTEMPTS;attempt++){WorldMemory keyed=c.key()==null?null:findKey(u,w,c.type().name(),c.key());if(keyed!=null){if(keyed.getSourceRoundId()!=null&&keyed.getSourceRoundId()>r)return;int v=keyed.getVersion()==null?0:keyed.getVersion();keyed.setContent(c.content());keyed.setDedupeHash(hash);keyed.setImportance(c.importance());keyed.setSourceRoundId(r);keyed.setSourceEventId(c.sourceEventId());keyed.setUpdateTime(LocalDateTime.now());try{if(memories.updateIfVersionMatches(keyed,v)==1)return;}catch(DuplicateKeyException duplicate){if(findHash(u,w,c.type().name(),hash)!=null)return;}continue;}WorldMemory n=new WorldMemory();n.setUserId(u);n.setWorldId(w);n.setMemoryType(c.type().name());n.setMemoryKey(c.key());n.setContent(c.content());n.setDedupeHash(hash);n.setSourceRoundId(r);n.setSourceEventId(c.sourceEventId());n.setImportance(c.importance());n.setVersion(0);n.setDeleted(0);n.setCreateTime(LocalDateTime.now());n.setUpdateTime(n.getCreateTime());try{memories.insert(n);return;}catch(DuplicateKeyException duplicate){if(findHash(u,w,c.type().name(),hash)!=null)return;}}throw new IllegalStateException("WORLD_MEMORY_CONSOLIDATION_CONFLICT");}
 // A matching quote establishes provenance, not semantic entailment of the model's
 // free-form summary. Keep that summary for ranking/consolidation, but inject only
 // the source quote with the server-enforced uncertainty/plan marker.
 private String renderMemory(WorldMemory m){
  String content=m.getContent();
  if(m.getSourceEventId()!=null){
   String marker="\n[来源 Event "+m.getSourceEventId()+" 原文] ";int at=content.lastIndexOf(marker);
   if(at>=0){
    String stance=content.startsWith("[未证实的说法，不代表已发生]")?"[未证实的说法，不代表已发生] ":content.startsWith("[计划或承诺，尚不代表已完成]")?"[计划或承诺，尚不代表已完成] ":"";
    return "["+m.getMemoryType()+"] "+stance+"[Event "+m.getSourceEventId()+" 原文] "+content.substring(at+marker.length());
   }
  }
  return "["+m.getMemoryType()+"] "+content;
 }
 // Provenance changes must not defeat exact-summary deduplication across Rounds.
 private String summaryForHash(String content){int marker=content.indexOf("\n[来源 Event ");return marker<0?content:content.substring(0,marker);}
 private WorldMemory findHash(Long u,Long w,String t,String h){return memories.selectOne(new QueryWrapper<WorldMemory>().eq("user_id",u).eq("world_id",w).eq("memory_type",t).eq("dedupe_hash",h));}private WorldMemory findKey(Long u,Long w,String t,String k){return memories.selectOne(new QueryWrapper<WorldMemory>().eq("user_id",u).eq("world_id",w).eq("memory_type",t).eq("memory_key",k).last("LIMIT 1"));}private String normalize(String s){return s.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);}private String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}private int score(WorldMemory m,String q){int s=m.getImportance()==null?50:m.getImportance();for(String t:q.split("\\s+"))if(t.length()>1&&m.getContent().toLowerCase().contains(t))s+=20;return s;}
}
