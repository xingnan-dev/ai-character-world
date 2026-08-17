package com.companion.ai;

import com.companion.avatar.generation.AvatarAppearanceConfig;
import com.companion.entity.AvatarAsset;
import com.companion.mapper.AvatarAssetMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModelMatcherTest {

    @Test
    void returnsHighestNegativeCandidateAndActualSkyScore() {
        AvatarAssetMapper mapper = mock(AvatarAssetMapper.class);
        AvatarAsset nova = nova();
        AvatarAsset sky = sky();
        when(mapper.selectList(any())).thenReturn(List.of(nova, sky));

        AssetMatchResult result = new ModelMatcher(mapper, new ObjectMapper()).match(complexFemale());

        assertThat(result.asset().getId()).isEqualTo(18L);
        assertThat(result.score()).isEqualTo(-25);
    }

    @Test
    void usesExactStructuredTagsInsteadOfSubstringContains() {
        AvatarAssetMapper mapper = mock(AvatarAssetMapper.class);
        AvatarAsset asset = sky();
        asset.setColorTags("silversmith");
        when(mapper.selectList(any())).thenReturn(List.of(asset));

        AssetMatchResult result = new ModelMatcher(mapper, new ObjectMapper()).match(complexFemale());

        assertThat(result.score()).isEqualTo(-25);
    }

    @Test
    void noAssetsReturnsEmptyWithoutSyntheticDefault() {
        AvatarAssetMapper mapper = mock(AvatarAssetMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());

        AssetMatchResult result = new ModelMatcher(mapper, new ObjectMapper()).match(complexFemale());

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.asset()).isNull();
    }

    private AvatarAppearanceConfig complexFemale() {
        return new AvatarAppearanceConfig(
                "female", "silver", "long", "blue", "slim", "tech_future", "silver",
                "cat", "mechanical", List.of()
        );
    }

    private AvatarAsset sky() {
        AvatarAsset asset = new AvatarAsset();
        asset.setId(18L);
        asset.setGender(0);
        asset.setStyleTags("basic,modern");
        asset.setColorTags("");
        asset.setSupportedAttributes("""
                {"hair":{"color":[],"style":[]},"eye":{"color":[]},"body":{"type":[]},
                 "outfit":{"style":[],"color":[]},"ear":["human"],"wing":["none"],"accessories":[]}
                """);
        asset.setDownloadCount(0);
        return asset;
    }

    private AvatarAsset nova() {
        AvatarAsset asset = new AvatarAsset();
        asset.setId(17L);
        asset.setGender(1);
        asset.setStyleTags("basic,modern,casual");
        asset.setColorTags("brown,blue,teal,black,white");
        asset.setSupportedAttributes("""
                {"hair":{"color":["brown"],"style":["short"]},"eye":{"color":["blue"]},
                 "body":{"type":["slim"]},"outfit":{"style":["casual"],"color":["teal","black","white"]},
                 "ear":["human"],"wing":["none"],"accessories":[]}
                """);
        asset.setDownloadCount(0);
        return asset;
    }
}
