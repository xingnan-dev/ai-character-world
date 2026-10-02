package com.companion.agent.tool;
import com.companion.ai.MemoryEngine;import com.companion.service.CharacterService;import com.fasterxml.jackson.databind.JsonNode;import org.springframework.stereotype.Component;
@Component public class SearchCharacterMemoryTool implements AgentTool {private final CharacterService characters;private final MemoryEngine memory;public SearchCharacterMemoryTool(CharacterService c,MemoryEngine m){characters=c;memory=m;}
 public String name(){return "search_character_memory";}public String description(){return "Search global and character-scoped memory for one owned character.";}public String inputSchema(){return "{characterId: positive integer, query: non-empty string}";}
 public void validate(JsonNode a){ToolArguments.exact(a,"characterId","query");ToolArguments.positiveLong(a,"characterId");ToolArguments.text(a,"query",500);}public String execute(Long u,JsonNode a){validate(a);long id=ToolArguments.positiveLong(a,"characterId");String q=ToolArguments.text(a,"query",500);characters.get(u,id);String s=memory.getMemoryContext(u,id,q);return s.length()>8000?s.substring(0,8000):s;}
}
