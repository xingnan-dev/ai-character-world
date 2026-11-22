package com.companion.ai.worldmemory;

import com.companion.ai.LlmClient;
import com.companion.ai.json.LlmJsonObjectExtractor;
import com.companion.ai.prompt.*;
import com.companion.entity.enums.WorldMemoryType;
import com.fasterxml.jackson.databind.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Pattern;

@Component @RequiredArgsConstructor
public class LlmWorldMemoryExtractor {
 private final LlmClient client;
 private final ObjectMapper mapper;
 private final PromptTemplateLoader loader;
 private final PromptTemplateRenderer renderer;
 // Conservative guard, not semantic entailment. Inspect the entire source Event to
 // prevent a short quote from hiding explicit uncertainty elsewhere in the Event.
 private static final Pattern UNCERTAIN = Pattern.compile("(?iu)[?？]|如果|假如|假设|也许|可能|猜测|猜想|传闻|据说|听说|未证实|未经证实|尚未确认|\\b(if|maybe|perhaps|might|rumou?r|suspect|unconfirmed|suppose)\\b");
 private static final Pattern PLAN = Pattern.compile("(?iu)计划|打算|准备|提议|承诺|将要|尚未执行|\\b(plan|intend|propose|promise|will)\\b");
 public List<WorldMemoryCandidate> extract(WorldMemoryExtractionContext c) {
  try {
   String system=renderer.render(loader.load(PromptTemplateKey.WORLD_MEMORY_SYSTEM),Map.of());
   String transcript=bound(c.transcript());
   String user=renderer.render(loader.load(PromptTemplateKey.WORLD_MEMORY_USER),Map.of("worldSnapshot",bound(c.worldSnapshot()),"participantSnapshots",bound(c.participantSnapshots()),"transcript",transcript,"existingMemory",bound(c.existingMemory())));
   JsonNode array=mapper.readTree(LlmJsonObjectExtractor.extract(client.complete(system,user,1600).content())).path("memories");
   if(!array.isArray())throw new IllegalArgumentException("Invalid world memory envelope");
   List<WorldMemoryCandidate> out=new ArrayList<>();
   for(JsonNode n:array) {
    if(out.size()==5)break;
    try {
     WorldMemoryType type=WorldMemoryType.valueOf(n.path("type").asText());
     String key=n.path("key").isTextual()?n.path("key").asText().trim():null;
     String content=n.path("content").asText().trim();
     int importance=n.path("importance").asInt(50);
     long id=n.path("sourceEventId").asLong(-1);
     String evidence=n.path("evidence").asText().trim();
     String source=c.sourceEvents().get(id);
     if(content.isBlank())continue;
     // Only eligible completed Events can support new memories. The summary can
     // paraphrase: only its evidence quote, not the summary, must match the source.
     if(source==null||evidence.isBlank()||evidence.length()>700||!source.contains(evidence)||!transcript.contains("Event "+id+" [")||!transcript.contains(evidence))continue;
     String stance=n.path("stance").asText();
     if(!Set.of("OBSERVED","PLAN","UNCERTAIN").contains(stance))continue;
     if(UNCERTAIN.matcher(source).find())stance="UNCERTAIN";
     else if(PLAN.matcher(source).find())stance="PLAN";
     if(stance.equals("UNCERTAIN")) {
      if(type!=WorldMemoryType.WORLD_STATE)continue;
      content="[未证实的说法，不代表已发生] "+content;
     } else if(stance.equals("PLAN")) {
      if(type!=WorldMemoryType.PROMISE)continue;
      content="[计划或承诺，尚不代表已完成] "+content;
     }
     if(content.isBlank()||content.length()>1200||(key!=null&&key.length()>255)||importance<1||importance>100)continue;
     content=content+"\n[来源 Event "+id+" 原文] "+evidence;
     if(content.length()>2000)continue;
     out.add(new WorldMemoryCandidate(type,key,content,importance,id));
    } catch(IllegalArgumentException ignored) { }
   }
   return out;
  } catch(Exception error) {
   // Distinguish failure from successful empty extraction. The Round caller isolates it.
   throw new IllegalStateException("WORLD_MEMORY_EXTRACTION_FAILED",error);
  }
 }
 private String bound(String s){return s==null?"":s.substring(0,Math.min(12000,s.length()));}
}
