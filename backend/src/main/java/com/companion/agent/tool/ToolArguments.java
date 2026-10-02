package com.companion.agent.tool;
import com.companion.agent.AgentExecutionException;import com.fasterxml.jackson.databind.JsonNode;import java.util.Set;
final class ToolArguments {private ToolArguments(){}
 static void exact(JsonNode n,String...names){if(n==null||!n.isObject())bad();Set<String> allowed=Set.of(names);n.fieldNames().forEachRemaining(x->{if(!allowed.contains(x))bad();});for(String x:names)if(!n.has(x)||n.get(x).isNull())bad();}
 static long positiveLong(JsonNode n,String key){if(!n.get(key).canConvertToLong()||n.get(key).asLong()<=0)bad();return n.get(key).asLong();}
 static String text(JsonNode n,String key,int max){String s=n.get(key).isTextual()?n.get(key).asText().trim():"";if(s.isEmpty()||s.length()>max)bad();return s;}
 private static void bad(){throw new AgentExecutionException("INVALID_TOOL_ARGUMENTS","Invalid tool arguments",false);}
}
