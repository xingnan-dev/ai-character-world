package com.companion.avatar.generation;

import com.companion.ai.ModelMatcher;
import com.companion.entity.AvatarAsset;
import com.companion.mapper.AvatarAssetMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvatarAssetSelectorTest {

    @Test
    void usesOfficialThenLowestIdAsStableTieBreaker() {
        AvatarAssetMapper mapper = mock(AvatarAssetMapper.class);
        AvatarAsset unofficial = asset(1L, 0, "/models/one.vrm");
        AvatarAsset officialHighId = asset(9L, 1, "/models/nine.vrm");
        AvatarAsset officialLowId = asset(3L, 1, "/models/three.vrm");
        when(mapper.selectList(any())).thenReturn(List.of(unofficial, officialHighId, officialLowId));

        AvatarAppearanceConfig config = new AvatarAppearanceNormalizer().normalize(new AvatarAppearanceConfig());
        AvatarAssetSelector selector = new AvatarAssetSelector(new ModelMatcher(mapper), new ObjectMapper());

        assertThat(selector.select(config).asset().getId()).isEqualTo(3L);
        assertThat(selector.select(config).asset().getId()).isEqualTo(3L);
    }

    @Test
    void reportsNearestWhenGenderAndSpecialFeaturesAreUnsupported() {
        AvatarAssetMapper mapper = mock(AvatarAssetMapper.class);
        AvatarAsset nova = asset(1L, 1, "/models/avatars/nova.vrm");
        nova.setGender(1);
        nova.setSupportedAttributes("""
                {"hair":{"color":["brown"],"style":["short"]},"eye":{"color":["blue"]},
                 "body":{"type":["slim"]},"outfit":{"style":["casual"],"color":["white"]},
                 "ear":["human"],"wing":["none"],"accessories":[]}
                """);
        when(mapper.selectList(any())).thenReturn(List.of(nova));
        AvatarAppearanceConfig config = new AvatarAppearanceConfig(
                "female", "silver", "long", "blue", "slim", "casual", "white",
                "cat", "mechanical", List.of("glasses")
        );

        AvatarAssetSelector.Selection result = new AvatarAssetSelector(new ModelMatcher(mapper), new ObjectMapper()).select(config);

        assertThat(result.matchType()).isEqualTo(AvatarAssetSelector.MatchType.NEAREST);
        assertThat(result.unmatchedAttributes()).contains(
                "gender", "hairColor", "hairStyle", "earType", "wingType", "accessories"
        );
    }

    @Test
    void reportsMatchedOnlyWhenFixedAssetCapabilitiesReallyMatch() {
        AvatarAssetMapper mapper = mock(AvatarAssetMapper.class);
        AvatarAsset nova = asset(1L, 1, "/models/avatars/nova.vrm");
        nova.setGender(1);
        nova.setSupportedAttributes("""
                {"hair":{"color":["brown"],"style":["short"]},"eye":{"color":["blue"]},
                 "body":{"type":["slim"]},"outfit":{"style":["casual"],"color":["white"]},
                 "ear":["human"],"wing":["none"],"accessories":[]}
                """);
        when(mapper.selectList(any())).thenReturn(List.of(nova));
        AvatarAppearanceConfig config = new AvatarAppearanceConfig(
                "male", "brown", "short", "blue", "slim", "casual", "white",
                "human", "none", List.of()
        );

        AvatarAssetSelector.Selection result = new AvatarAssetSelector(new ModelMatcher(mapper), new ObjectMapper()).select(config);

        assertThat(result.matchType()).isEqualTo(AvatarAssetSelector.MatchType.MATCHED);
        assertThat(result.unmatchedAttributes()).isEmpty();
    }

    private AvatarAsset asset(Long id, int official, String url) {
        AvatarAsset asset = new AvatarAsset();
        asset.setId(id);
        asset.setIsOfficial(official);
        asset.setFileUrl(url);
        asset.setGender(2);
        asset.setStatus(1);
        asset.setDownloadCount(0);
        return asset;
    }
}
