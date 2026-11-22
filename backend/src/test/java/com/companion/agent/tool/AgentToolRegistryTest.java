package com.companion.agent.tool;
import com.companion.agent.AgentExecutionException;import com.fasterxml.jackson.databind.JsonNode;import org.junit.jupiter.api.Test;import java.util.List;import static org.assertj.core.api.Assertions.*;
class AgentToolRegistryTest {static class T implements AgentTool{private final String n;T(String n){this.n=n;}public String name(){return n;}public String description(){return n;}public String inputSchema(){return "{}";}public void validate(JsonNode n){}public String execute(Long u,JsonNode n){return "ok";}}
 @Test void allowlistRejectsUnknownAndDuplicate(){AgentToolRegistry r=new AgentToolRegistry(List.of(new T("safe")));assertThat(r.require("safe").name()).isEqualTo("safe");assertThatThrownBy(()->r.require("shell")).isInstanceOf(AgentExecutionException.class);assertThatThrownBy(()->new AgentToolRegistry(List.of(new T("same"),new T("same")))).isInstanceOf(IllegalStateException.class);}
}
