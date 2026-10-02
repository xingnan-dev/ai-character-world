package com.companion.agent.tool;
import com.companion.agent.AgentExecutionException;import org.springframework.stereotype.Component;import java.util.*;import java.util.function.Function;import java.util.stream.Collectors;
@Component public class AgentToolRegistry {private final Map<String,AgentTool> tools;
 public AgentToolRegistry(List<AgentTool> values){try{tools=values.stream().collect(Collectors.toUnmodifiableMap(AgentTool::name,Function.identity()));}catch(IllegalStateException e){throw new IllegalStateException("Duplicate Agent Tool registration",e);}}
 public AgentTool require(String name){AgentTool t=tools.get(name);if(t==null)throw new AgentExecutionException("UNKNOWN_TOOL","Unknown tool: "+name,false);return t;}
 public List<AgentTool> all(){return tools.values().stream().sorted(Comparator.comparing(AgentTool::name)).toList();}
}
