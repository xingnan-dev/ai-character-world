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
        AvatarAssetSelector selector = selector(mapper, 0);

        assertThat(selector.select(config).asset().getId()).isEqualTo(3L);
        assertThat(selector.select(config).asset().getId()).isEqualTo(3L);
    }

    @Test
    void reportsUnavailableWhenGenderAndSpecialFeaturesAreUnsupported() {
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

        AvatarAssetSelector.Selection result = selector(mapper, 10).select(config);

        assertThat(result.matchType()).isEqualTo(AvatarAssetSelector.MatchType.UNAVAILABLE);
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

        AvatarAssetSelector.Selection result = selector(mapper, 10).select(config);

        assertThat(result.matchType()).isEqualTo(AvatarAssetSelector.MatchType.MATCHED);
        assertThat(result.unmatchedAttributes()).isEmpty();
    }

    @Test
    void scoreBelowMinimumIsUnavailable() {
        AvatarAssetMapper mapper = mock(AvatarAssetMapper.class);
        AvatarAsset sky = asset(18L, 1, "/models/avatars/sky.vrm");
        sky.setGender(0);
        sky.setStyleTags("basic,modern");
        sky.setColorTags("");
        sky.setSupportedAttributes(emptyFixedCapabilities());
        when(mapper.selectList(any())).thenReturn(List.of(sky));

        AvatarAssetSelector.Selection result = selector(mapper, 10).select(complexFemale());

        assertThat(result.score()).isEqualTo(-25);
        assertThat(result.matchType()).isEqualTo(AvatarAssetSelector.MatchType.UNAVAILABLE);
    }

    @Test
    void minorDifferenceAboveMinimumIsNearest() {
        AvatarAssetMapper mapper = mock(AvatarAssetMapper.class);
        AvatarAsset asset = fullyCapableFemale(2L);
        asset.setColorTags("brown,blue,white");
        asset.setSupportedAttributes(asset.getSupportedAttributes()
                .replace("\"color\":[\"silver\"],\"style\"", "\"color\":[\"brown\"],\"style\""));
        when(mapper.selectList(any())).thenReturn(List.of(asset));

        AvatarAssetSelector.Selection result = selector(mapper, 10).select(standardFemale());

        assertThat(result.matchType()).isEqualTo(AvatarAssetSelector.MatchType.NEAREST);
        assertThat(result.unmatchedAttributes()).containsExactly("hairColor");
    }

    @Test
    void unsupportedCatEarWingOrAccessoryIsUnavailable() {
        AvatarAssetMapper mapper = mock(AvatarAssetMapper.class);
        AvatarAsset fixed = fullyCapableFemale(2L);
        when(mapper.selectList(any())).thenReturn(List.of(fixed));

        AvatarAppearanceConfig config = standardFemale();
        config.setEarType("cat");
        assertThat(selector(mapper, -100).select(config).matchType()).isEqualTo(AvatarAssetSelector.MatchType.UNAVAILABLE);
        config.setEarType("human");
        config.setWingType("mechanical");
        assertThat(selector(mapper, -100).select(config).matchType()).isEqualTo(AvatarAssetSelector.MatchType.UNAVAILABLE);
        config.setWingType("none");
        config.setAccessories(List.of("glasses"));
        assertThat(selector(mapper, -100).select(config).matchType()).isEqualTo(AvatarAssetSelector.MatchType.UNAVAILABLE);
    }

    @Test
    void oppositeGenderAndMalformedCapabilitiesAreUnavailable() {
        AvatarAssetMapper mapper = mock(AvatarAssetMapper.class);
        AvatarAsset asset = fullyCapableFemale(2L);
        asset.setGender(1);
        asset.setSupportedAttributes("{broken");
        when(mapper.selectList(any())).thenReturn(List.of(asset));

        AvatarAssetSelector.Selection result = selector(mapper, -100).select(standardFemale());

        assertThat(result.matchType()).isEqualTo(AvatarAssetSelector.MatchType.UNAVAILABLE);
        assertThat(result.unmatchedAttributes()).contains("gender", "bodyType");
    }

    @Test
    void noPersistedAssetIsUnavailable() {
        AvatarAssetMapper mapper = mock(AvatarAssetMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());

        AvatarAssetSelector.Selection result = selector(mapper, 10).select(standardFemale());

        assertThat(result.asset()).isNull();
        assertThat(result.matchType()).isEqualTo(AvatarAssetSelector.MatchType.UNAVAILABLE);
    }

    private AvatarAssetSelector selector(AvatarAssetMapper mapper, int minimumScore) {
        ObjectMapper objectMapper = new ObjectMapper();
        AvatarAssetProperties properties = new AvatarAssetProperties();
        properties.setMinimumScore(minimumScore);
        return new AvatarAssetSelector(new ModelMatcher(mapper, objectMapper), objectMapper, properties);
    }

    private AvatarAppearanceConfig standardFemale() {
        return new AvatarAppearanceConfig(
                "female", "silver", "long", "blue", "slim", "tech_future", "silver",
                "human", "none", List.of()
        );
    }

    private AvatarAppearanceConfig complexFemale() {
        AvatarAppearanceConfig config = standardFemale();
        config.setEarType("cat");
        config.setWingType("mechanical");
        return config;
    }

    private AvatarAsset fullyCapableFemale(Long id) {
        AvatarAsset asset = asset(id, 1, "/models/test.vrm");
        asset.setGender(2);
        asset.setStyleTags("basic,tech_future");
        asset.setColorTags("silver,blue");
        asset.setSupportedAttributes("""
                {"hair":{"color":["silver"],"style":["long"]},"eye":{"color":["blue"]},
                 "body":{"type":["slim"]},"outfit":{"style":["tech_future"],"color":["silver"]},
                 "ear":["human"],"wing":["none"],"accessories":[]}
                """);
        return asset;
    }

    private String emptyFixedCapabilities() {
        return """
                {"hair":{"color":[],"style":[]},"eye":{"color":[]},"body":{"type":[]},
                 "outfit":{"style":[],"color":[]},"ear":["human"],"wing":["none"],"accessories":[]}
                """;
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
