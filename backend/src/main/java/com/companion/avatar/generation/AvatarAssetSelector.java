package com.companion.avatar.generation;

import com.companion.ai.AssetMatchResult;
import com.companion.ai.ModelMatcher;
import com.companion.entity.AvatarAsset;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AvatarAssetSelector {

    public enum MatchType {
        MATCHED,
        NEAREST,
        UNAVAILABLE
    }

    public record Selection(AvatarAsset asset, int score, MatchType matchType, List<String> unmatchedAttributes) {
    }

    private final ModelMatcher modelMatcher;
    private final ObjectMapper objectMapper;
    private final AvatarAssetProperties properties;

    public Selection select(AvatarAppearanceConfig appearanceConfig) {
        AssetMatchResult match = modelMatcher.match(appearanceConfig);
        if (match.isEmpty()) {
            return new Selection(null, match.score(), MatchType.UNAVAILABLE, List.of("asset"));
        }
        AvatarAsset asset = match.asset();
        List<String> unmatched = findUnmatched(appearanceConfig, asset);
        MatchType type;
        if (match.score() < properties.getMinimumScore() || hasCriticalMismatch(appearanceConfig, asset)) {
            type = MatchType.UNAVAILABLE;
        } else {
            type = unmatched.isEmpty() ? MatchType.MATCHED : MatchType.NEAREST;
        }
        return new Selection(asset, match.score(), type, List.copyOf(unmatched));
    }

    private boolean hasCriticalMismatch(AvatarAppearanceConfig config, AvatarAsset asset) {
        int requestedGender = gender(config.getGender());
        Integer assetGender = asset.getGender();
        if (requestedGender != 0 && assetGender != null && assetGender != 0 && assetGender != requestedGender) {
            return true;
        }

        JsonNode supported = supported(asset);
        if (config.getEarType() != null && !"human".equalsIgnoreCase(config.getEarType())
                && !contains(supported.path("ear"), config.getEarType())) {
            return true;
        }
        if (config.getWingType() != null && !"none".equalsIgnoreCase(config.getWingType())
                && !contains(supported.path("wing"), config.getWingType())) {
            return true;
        }
        if (config.getAccessories() != null) {
            for (String accessory : config.getAccessories()) {
                if (!contains(supported.path("accessories"), accessory)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> findUnmatched(AvatarAppearanceConfig config, AvatarAsset asset) {
        List<String> unmatched = new ArrayList<>();
        if (asset == null) {
            return List.of("asset");
        }
        int requestedGender = gender(config.getGender());
        if (requestedGender != 0 && (asset.getGender() == null || asset.getGender() == 0 || asset.getGender() != requestedGender)) {
            unmatched.add("gender");
        }

        JsonNode supported = supported(asset);
        check(unmatched, supported.path("hair").path("color"), config.getHairColor(), "hairColor");
        check(unmatched, supported.path("hair").path("style"), config.getHairStyle(), "hairStyle");
        check(unmatched, supported.path("eye").path("color"), config.getEyeColor(), "eyeColor");
        check(unmatched, supported.path("body").path("type"), config.getBodyType(), "bodyType");
        check(unmatched, supported.path("outfit").path("style"), config.getOutfitStyle(), "outfitStyle");
        check(unmatched, supported.path("outfit").path("color"), config.getOutfitColor(), "outfitColor");
        check(unmatched, supported.path("ear"), config.getEarType(), "earType");
        check(unmatched, supported.path("wing"), config.getWingType(), "wingType");
        if (config.getAccessories() != null) {
            for (String accessory : config.getAccessories()) {
                if (!contains(supported.path("accessories"), accessory)) {
                    unmatched.add("accessories");
                    break;
                }
            }
        }
        return unmatched;
    }

    private int gender(String value) {
        return "male".equalsIgnoreCase(value) ? 1 : "female".equalsIgnoreCase(value) ? 2 : 0;
    }

    private JsonNode supported(AvatarAsset asset) {
        try {
            return objectMapper.readTree(asset.getSupportedAttributes() == null ? "{}" : asset.getSupportedAttributes());
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private void check(List<String> unmatched, JsonNode values, String requested, String field) {
        if (requested != null && !contains(values, requested)) {
            unmatched.add(field);
        }
    }

    private boolean contains(JsonNode values, String requested) {
        if (!values.isArray()) return false;
        for (JsonNode value : values) {
            if (requested.equalsIgnoreCase(value.asText())) return true;
        }
        return false;
    }
}
