package com.companion.character.snapshot;

import com.companion.character.model.CharacterProfile;
import com.companion.dto.response.CharacterResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CharacterSnapshotTest {

    private final CharacterSnapshotJsonMapper jsonMapper =
            new CharacterSnapshotJsonMapper(new ObjectMapper());

    @Test
    void createsCompleteImmutableSnapshotAndDefensivelyCopiesProfile() {
        CharacterResponse source = completeCharacter();
        List<String> values = new ArrayList<>(List.of("诚实"));
        source.getProfile().setValues(values);

        CharacterSnapshot snapshot = CharacterSnapshot.from(source);
        source.setName("修改后的名字");
        values.add("后来添加");
        source.getProfile().getInterests().add("后来兴趣");

        assertThat(snapshot.name()).isEqualTo("林澈");
        assertThat(snapshot.profile().values()).containsExactly("诚实");
        assertThat(snapshot.profile().interests()).containsExactly("宇宙");
        assertThatThrownBy(() -> snapshot.profile().values().add("非法修改"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void supportsOptionalFieldsAndEmptyProfile() {
        CharacterResponse source = completeCharacter();
        source.setAge(null);
        source.setIdentity(null);
        source.setProfile(null);

        CharacterSnapshot snapshot = CharacterSnapshot.from(source);

        assertThat(snapshot.age()).isNull();
        assertThat(snapshot.identity()).isNull();
        assertThat(snapshot.profile().values()).isEmpty();
    }

    @Test
    void jsonRoundTripPreservesSnapshotAndOmitsOwnershipAndAuditFields() {
        CharacterSnapshot snapshot = CharacterSnapshot.from(completeCharacter());

        String json = jsonMapper.write(snapshot);
        CharacterSnapshot restored = jsonMapper.read(json);

        assertThat(restored).isEqualTo(snapshot);
        assertThat(json).doesNotContain("userId", "deleted", "createTime", "updateTime");
    }

    @Test
    void rejectsInvalidJsonMissingRequiredFieldsAndUnsupportedVersion() {
        assertThatThrownBy(() -> jsonMapper.read("not-json"))
                .isInstanceOf(CharacterSnapshotException.class);
        assertThatThrownBy(() -> jsonMapper.read("""
                {"snapshotVersion":1,"sourceCharacterId":1,"characterType":"AI","visualType":"INITIAL"}
                """))
                .isInstanceOf(CharacterSnapshotException.class);
        assertThatThrownBy(() -> jsonMapper.read("""
                {"snapshotVersion":2,"sourceCharacterId":1,"characterType":"AI","name":"角色","visualType":"INITIAL"}
                """))
                .isInstanceOf(CharacterSnapshotException.class);
        assertThatThrownBy(() -> jsonMapper.read("""
                {"snapshotVersion":1,"sourceCharacterId":1,"characterType":"AI","name":"角色",\
                "visualType":"INITIAL","userId":99}
                """))
                .isInstanceOf(CharacterSnapshotException.class);
    }

    @Test
    void rejectsUnknownTypesAndInvalidProfileItems() {
        CharacterSnapshot valid = CharacterSnapshot.from(completeCharacter());

        assertThatThrownBy(() -> new CharacterSnapshot(
                valid.snapshotVersion(), valid.sourceCharacterId(), "UNKNOWN", valid.name(), valid.age(),
                valid.identity(), valid.corePersonality(), valid.currentGoal(), valid.biography(),
                valid.relationshipToUser(), valid.speakingStyle(), valid.profile(), valid.visualType(),
                valid.avatarId(), valid.imageUrl(), valid.avatarColor()))
                .isInstanceOf(CharacterSnapshotException.class);

        assertThatThrownBy(() -> new CharacterSnapshot(
                valid.snapshotVersion(), valid.sourceCharacterId(), valid.characterType(), valid.name(), valid.age(),
                valid.identity(), valid.corePersonality(), valid.currentGoal(), valid.biography(),
                valid.relationshipToUser(), valid.speakingStyle(), valid.profile(), "UNKNOWN",
                valid.avatarId(), valid.imageUrl(), valid.avatarColor()))
                .isInstanceOf(CharacterSnapshotException.class);

        assertThatThrownBy(() -> new CharacterSnapshot.Profile(
                java.util.Collections.singletonList(null), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()))
                .isInstanceOf(CharacterSnapshotException.class);
    }

    private CharacterResponse completeCharacter() {
        CharacterProfile profile = new CharacterProfile();
        profile.setValues(new ArrayList<>(List.of("诚实")));
        profile.setLikes(new ArrayList<>(List.of("阅读")));
        profile.setDislikes(new ArrayList<>());
        profile.setInterests(new ArrayList<>(List.of("宇宙")));
        profile.setFears(new ArrayList<>());
        profile.setSecrets(new ArrayList<>());
        profile.setBehaviorTendencies(new ArrayList<>(List.of("先分析")));

        CharacterResponse response = new CharacterResponse();
        response.setId(42L);
        response.setCharacterType("AI");
        response.setName("林澈");
        response.setAge(28);
        response.setIdentity("研究员");
        response.setCorePersonality("冷静、好奇");
        response.setCurrentGoal("探索未知");
        response.setBiography("长期研究人工智能。");
        response.setRelationshipToUser("伙伴");
        response.setSpeakingStyle("简洁自然");
        response.setProfile(profile);
        response.setVisualType("INITIAL");
        response.setAvatarColor("#667eea");
        return response;
    }
}
