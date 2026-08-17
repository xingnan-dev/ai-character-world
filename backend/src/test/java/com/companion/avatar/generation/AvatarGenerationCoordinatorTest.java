package com.companion.avatar.generation;

import com.companion.ai.AiService;
import com.companion.ai.dto.AvatarGenerateResult;
import com.companion.chat.ChatMessageLifecycleService;
import com.companion.chat.DefaultSessionPersonalityResolver;
import com.companion.chat.PersonalitySnapshotCodec;
import com.companion.dto.request.AvatarGenerateRequest;
import com.companion.dto.request.ChatSessionCreateRequest;
import com.companion.dto.response.AvatarGenerateResponse;
import com.companion.entity.Avatar;
import com.companion.entity.AvatarAsset;
import com.companion.entity.AvatarAttribute;
import com.companion.entity.ChatSession;
import com.companion.entity.Personality;
import com.companion.mapper.AvatarAttributeMapper;
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.ChatMessageMapper;
import com.companion.mapper.ChatSessionMapper;
import com.companion.mapper.PersonalityMapper;
import com.companion.service.impl.ChatServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvatarGenerationCoordinatorTest {

    @Mock private AvatarDescriptionParser parser;
    @Mock private AvatarAssetSelector assetSelector;
    @Mock private AvatarMapper avatarMapper;
    @Mock private PersonalityMapper personalityMapper;
    @Mock private AvatarAttributeMapper attributeMapper;

    private ObjectMapper objectMapper;
    private AvatarGenerationCoordinator coordinator;
    private AvatarDescriptionParser.ParseResult parsed;
    private AvatarAsset selectedAsset;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        coordinator = new AvatarGenerationCoordinator(
                parser, assetSelector, avatarMapper, personalityMapper, attributeMapper, objectMapper
        );
        AvatarAppearanceConfig appearance = new AvatarAppearanceConfig(
                "female", "purple", "long", "blue", "slim",
                "gothic", "black", "human", "none", List.of("glasses", "headset")
        );
        AvatarGenerateResult.PersonalityConfig personality = new AvatarGenerateResult.PersonalityConfig(
                "cool", List.of("沉稳", "理性"), "简洁", "很高兴认识你"
        );
        parsed = new AvatarDescriptionParser.ParseResult(
                "月璃", appearance, personality, List.of("gothic"), AvatarDescriptionParser.ParseSource.LLM
        );
        selectedAsset = new AvatarAsset();
        selectedAsset.setId(2L);
        selectedAsset.setFileUrl("/models/avatars/sky.vrm");
        selectedAsset.setThumbnailUrl("/images/sky.png");
    }

    @Test
    void persistsOneCanonicalAppearanceAndBothSidesOfPersonalityBinding() throws Exception {
        AtomicReference<Avatar> storedAvatar = new AtomicReference<>();
        AtomicReference<Personality> storedPersonality = new AtomicReference<>();
        when(parser.parse(any())).thenReturn(parsed);
        when(assetSelector.select(parsed.getAppearanceConfig())).thenReturn(selection());
        when(avatarMapper.insert(any())).thenAnswer(invocation -> {
            Avatar avatar = invocation.getArgument(0);
            avatar.setId(101L);
            storedAvatar.set(avatar);
            return 1;
        });
        when(personalityMapper.insert(any())).thenAnswer(invocation -> {
            Personality personality = invocation.getArgument(0);
            personality.setId(201L);
            storedPersonality.set(personality);
            return 1;
        });
        when(avatarMapper.updateById(any())).thenReturn(1);
        when(attributeMapper.insert(any())).thenReturn(1);

        AvatarGenerateResponse response = coordinator.generate(7L, request("紫发哥特女孩，戴眼镜和耳机"));

        assertThat(storedAvatar.get().getModelUrl()).isEqualTo("/models/avatars/sky.vrm");
        assertThat(storedAvatar.get().getBaseModel()).isEqualTo("sky");
        assertThat(storedAvatar.get().getSourceDescription()).isEqualTo("紫发哥特女孩，戴眼镜和耳机");
        assertThat(storedAvatar.get().getPersonalityId()).isEqualTo(201L);
        assertThat(storedPersonality.get().getAvatarId()).isEqualTo(101L);
        assertThat(response.getParseSource()).isEqualTo("LLM");
        assertThat(response.getAssetMatchType()).isEqualTo("NEAREST");
        assertThat(response.getUnmatchedAttributes()).containsExactly("gender");
        assertThat(response.getAvatar().getPersonalityId()).isEqualTo(201L);
        assertThat(response.getPersonality().getAvatarId()).isEqualTo(101L);

        JsonNode appearance = objectMapper.readTree(storedAvatar.get().getAppearanceConfig());
        JsonNode generation = objectMapper.readTree(storedAvatar.get().getGenerateResult());
        assertThat(generation.get("appearanceConfig")).isEqualTo(appearance);
        assertThat(generation.get("parseSource").asText()).isEqualTo("LLM");

        ArgumentCaptor<AvatarAttribute> attributeCaptor = ArgumentCaptor.forClass(AvatarAttribute.class);
        verify(attributeMapper, org.mockito.Mockito.times(9)).insert(attributeCaptor.capture());
        List<AvatarAttribute> accessories = attributeCaptor.getAllValues().stream()
                .filter(value -> "accessory".equals(value.getCategory())).toList();
        assertThat(accessories).hasSize(1);
        assertThat(accessories.get(0).getAttrKey()).isEqualTo("types");
        assertThat(objectMapper.readTree(accessories.get(0).getAttrValue()))
                .isEqualTo(objectMapper.readTree("[\"glasses\",\"headset\"]"));
    }

    @Test
    void generatedAvatarCanCreateSessionAndPersonalitySnapshot() {
        AtomicReference<Avatar> storedAvatar = new AtomicReference<>();
        AtomicReference<Personality> storedPersonality = new AtomicReference<>();
        when(parser.parse(any())).thenReturn(parsed);
        when(assetSelector.select(any())).thenReturn(selection());
        when(avatarMapper.insert(any())).thenAnswer(invocation -> {
            Avatar avatar = invocation.getArgument(0);
            avatar.setId(101L);
            storedAvatar.set(avatar);
            return 1;
        });
        when(personalityMapper.insert(any())).thenAnswer(invocation -> {
            Personality personality = invocation.getArgument(0);
            personality.setId(201L);
            storedPersonality.set(personality);
            return 1;
        });
        when(avatarMapper.updateById(any())).thenReturn(1);
        when(attributeMapper.insert(any())).thenReturn(1);
        coordinator.generate(7L, request("紫发哥特女孩"));

        when(avatarMapper.selectOne(any())).thenReturn(storedAvatar.get());
        when(personalityMapper.selectOne(any())).thenReturn(storedPersonality.get());
        ChatSessionMapper chatSessionMapper = mock(ChatSessionMapper.class);
        when(chatSessionMapper.insert(any())).thenAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            session.setId(301L);
            return 1;
        });
        DefaultSessionPersonalityResolver resolver = new DefaultSessionPersonalityResolver(
                personalityMapper, new PersonalitySnapshotCodec(objectMapper)
        );
        ChatServiceImpl chatService = new ChatServiceImpl(
                chatSessionMapper, mock(ChatMessageMapper.class), avatarMapper, resolver,
                mock(ChatMessageLifecycleService.class), mock(AiService.class)
        );

        chatService.createSession(7L, new ChatSessionCreateRequest(101L, null));

        ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
        verify(chatSessionMapper).insert(captor.capture());
        assertThat(captor.getValue().getPersonalityId()).isEqualTo(201L);
        assertThat(captor.getValue().getPersonalitySnapshot()).contains("沉稳");
    }

    @Test
    void transactionRollsBackWhenPersistenceFails() {
        when(parser.parse(any())).thenReturn(parsed);
        when(assetSelector.select(any())).thenReturn(selection());
        when(avatarMapper.insert(any())).thenAnswer(invocation -> {
            ((Avatar) invocation.getArgument(0)).setId(101L);
            return 1;
        });
        when(personalityMapper.insert(any())).thenReturn(0);

        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        ProxyFactory proxyFactory = new ProxyFactory(coordinator);
        proxyFactory.addAdvice(new TransactionInterceptor(
                transactionManager, new AnnotationTransactionAttributeSource()
        ));
        AvatarGenerationCoordinator transactional = (AvatarGenerationCoordinator) proxyFactory.getProxy();

        assertThatThrownBy(() -> transactional.generate(7L, request("描述")))
                .isInstanceOf(RuntimeException.class);

        verify(transactionManager).rollback(transactionStatus);
        verify(avatarMapper, never()).updateById(any());
        verify(attributeMapper, never()).insert(any());
    }

    @Test
    void unavailableAssetFailsBeforeAnyPersistence() {
        when(parser.parse(any())).thenReturn(parsed);
        when(assetSelector.select(any())).thenReturn(new AvatarAssetSelector.Selection(
                selectedAsset, -25, AvatarAssetSelector.MatchType.UNAVAILABLE,
                List.of("hairColor", "earType", "wingType")
        ));

        assertThatThrownBy(() -> coordinator.generate(7L, request("银色长发猫耳机械翅膀女孩")))
                .isInstanceOfSatisfying(com.companion.common.exception.BusinessException.class, exception ->
                        assertThat(exception.getMessage()).isEqualTo(
                                "当前资产库没有匹配该外观的3D模型，请调整描述或稍后重试。"
                        ));

        verify(avatarMapper, never()).insert(any());
        verify(personalityMapper, never()).insert(any());
        verify(attributeMapper, never()).insert(any());
        verify(avatarMapper, never()).updateById(any());
    }

    @Test
    void matchedAssetCreatesNormally() {
        when(parser.parse(any())).thenReturn(parsed);
        when(assetSelector.select(any())).thenReturn(new AvatarAssetSelector.Selection(
                selectedAsset, 40, AvatarAssetSelector.MatchType.MATCHED, List.of()
        ));
        when(avatarMapper.insert(any())).thenAnswer(invocation -> {
            ((Avatar) invocation.getArgument(0)).setId(101L);
            return 1;
        });
        when(personalityMapper.insert(any())).thenAnswer(invocation -> {
            ((Personality) invocation.getArgument(0)).setId(201L);
            return 1;
        });
        when(avatarMapper.updateById(any())).thenReturn(1);
        when(attributeMapper.insert(any())).thenReturn(1);

        AvatarGenerateResponse response = coordinator.generate(7L, request("匹配描述"));

        assertThat(response.getAssetMatchType()).isEqualTo("MATCHED");
        verify(avatarMapper).insert(any());
        verify(personalityMapper).insert(any());
    }

    private AvatarGenerateRequest request(String description) {
        AvatarGenerateRequest request = new AvatarGenerateRequest();
        request.setDescription(description);
        request.setCreatePersonality(false);
        return request;
    }

    private AvatarAssetSelector.Selection selection() {
        return new AvatarAssetSelector.Selection(
                selectedAsset, 20, AvatarAssetSelector.MatchType.NEAREST, List.of("gender")
        );
    }
}
