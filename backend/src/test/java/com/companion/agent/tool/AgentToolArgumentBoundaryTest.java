package com.companion.agent.tool;
import com.companion.agent.AgentExecutionException;
import com.companion.service.CharacterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class AgentToolArgumentBoundaryTest {
 @Test void fractionalResourceIdIsRejectedBeforeAnyRead() throws Exception {
  var service=mock(CharacterService.class);var mapper=new ObjectMapper();var tool=new GetCharacterContextTool(service,mapper);
  assertThatThrownBy(()->tool.validate(mapper.readTree("{\"characterId\":10.75}"))).isInstanceOf(AgentExecutionException.class);
  verifyNoInteractions(service);
 }
}
