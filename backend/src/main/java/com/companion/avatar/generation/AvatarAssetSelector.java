package com.companion.avatar.generation;

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
        NEAREST
    }

    public record Selection(AvatarAsset asset, MatchType matchType, List<String> unmatchedAttributes) {
    }

    private final ModelMatcher modelMatcher;
    private final ObjectMapper objectMapper;

    public Selection select(AvatarAppearanceConfig appearanceConfig) {
        AvatarAsset asset = modelMatcher.match(appearanceConfig);
        List<String> unmatched = findUnmatched(appearanceConfig, asset);
        MatchType type = unmatched.isEmpty() ? MatchType.MATCHED : MatchType.NEAREST;
        return new Selection(asset, type, List.copyOf(unmatched));
    }

    private List<String> findUnmatched(AvatarAppearanceConfig config, AvatarAsset asset) {
        List<String> unmatched = new ArrayList<>();
        if (asset == null) {
            return List.of("asset");
        }
        int requestedGender = "male".equals(config.getGender()) ? 1 : "female".equals(config.getGender()) ? 2 : 0;
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
        for (String accessory : config.getAccessories()) {
            if (!contains(supported.path("accessories"), accessory)) {
                unmatched.add("accessories");
                break;
            }
        }
        return unmatched;
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
