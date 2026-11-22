package com.companion.growth;

import com.companion.ai.growth.*;
import com.companion.ai.prompt.ChatPromptComposer;
import com.companion.character.snapshot.CharacterSnapshot;
import com.companion.entity.AiCharacter;
import com.companion.mapper.CharacterMapper;
import com.companion.service.CharacterGrowthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest @ActiveProfiles("soft-delete-test") @Transactional
class GrowthAdaptationPromptIntegrationTest {
    @Autowired CharacterMapper characters;
    @Autowired CharacterGrowthService growth;
    @Autowired ChatPromptComposer prompts;
    @MockBean LlmGrowthEvaluator evaluator;

    @Test void nonDefaultEvaluationPersistsThenEntersPromptWithoutChangingCoreIdentity() {
        var character=new AiCharacter();character.setUserId(95171L);character.setCharacterType(1);
        character.setName("Fixed Conservator");character.setIdentity("Optical instrument conservator");
        character.setCorePersonality("Careful and honest");character.setSpeakingStyle("Polite Chinese");
        character.setGenerateType(0);character.setVisualType(0);character.setStatus(1);character.setDeleted(0);
        character.setCreateTime(LocalDateTime.now());character.setUpdateTime(character.getCreateTime());characters.insert(character);
        var initial=growth.getOrCreate(95171L,character.getId());assertThat(initial.getVersion()).isZero();
        when(evaluator.evaluate(any(),any())).thenReturn(new GrowthEvaluation(true,
                "Recognizes a recurring need for plain language", "Give a conclusion followed by two numbered steps",
                "Prefers practical demonstrations", "Keep explanations accessible"));
        growth.evaluateAndUpdate(95171L,character.getId(),new GrowthEvaluationContext("immutable identity","state","memory","recurring feedback","reply"));
        var persisted=growth.get(95171L,character.getId()).orElseThrow();assertThat(persisted.getVersion()).isEqualTo(1);
        var snapshot=new CharacterSnapshot(1,character.getId(),"AI",character.getName(),null,character.getIdentity(),
                character.getCorePersonality(),null,null,null,character.getSpeakingStyle(),CharacterSnapshot.Profile.empty(),"INITIAL",null,null,"purple");
        var prompt=prompts.composeCharacter(95171L,1L,snapshot,null,persisted,"",List.of(),"How should the eyepiece be stored?");
        String system=prompt.messages().stream().filter(m->m.role().name().equals("SYSTEM")).map(m->m.content()).collect(java.util.stream.Collectors.joining("\n"));
        assertThat(system).contains(persisted.getBehaviorAdaptation(),"Optical instrument conservator","Careful and honest","Polite Chinese","成长不得覆盖");
        assertThat(characters.selectById(character.getId()).getIdentity()).isEqualTo(character.getIdentity());
        assertThat(characters.selectById(character.getId()).getCorePersonality()).isEqualTo(character.getCorePersonality());
    }
}
