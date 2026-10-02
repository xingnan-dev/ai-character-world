package com.companion.agent.tool;
import com.companion.service.CharacterWorldService;import com.fasterxml.jackson.databind.*;import org.springframework.stereotype.Component;
@Component public class GetWorldContextTool implements AgentTool {private final CharacterWorldService service;private final ObjectMapper mapper;public GetWorldContextTool(CharacterWorldService s,ObjectMapper m){service=s;mapper=m;}
 public String name(){return "get_world_context";}public String description(){return "Read one owned world's context and frozen participant snapshots.";}public String inputSchema(){return "{worldId: positive integer}";}
 public void validate(JsonNode a){ToolArguments.exact(a,"worldId");ToolArguments.positiveLong(a,"worldId");}public String execute(Long u,JsonNode a){validate(a);try{String s=mapper.writeValueAsString(service.get(u,ToolArguments.positiveLong(a,"worldId")));return s.length()>12000?s.substring(0,12000):s;}catch(com.companion.common.exception.BusinessException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
}
