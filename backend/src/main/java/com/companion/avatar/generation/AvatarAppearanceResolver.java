package com.companion.avatar.generation;

import com.companion.entity.AvatarAsset;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Projects a requested appearance onto capabilities explicitly declared by a selected asset.
 * It does not infer visual properties from names, tags, URLs, or any other indirect metadata.
 */
@Component
@RequiredArgsConstructor
public class AvatarAppearanceResolver {

    private final ObjectMapper objectMapper;

    public AvatarAppearanceResolution resolve(AvatarAppearanceConfig requested, AvatarAsset asset) {
        AvatarAppearanceConfig requestedCopy = copy(requested);
        AvatarAppearanceConfig realized = new AvatarAppearanceConfig();
        realized.setAccessories(List.of());

        if (asset == null) {
            return new AvatarAppearanceResolution(requestedCopy, realized);
        }

        JsonNode supported = supportedAttributes(asset);
        realized.setGender(resolveGender(asset.getGender()));
        realized.setHairColor(resolveValue(supported.path("hair").path("color"), requestedCopy.getHairColor()));
        realized.setHairStyle(resolveValue(supported.path("hair").path("style"), requestedCopy.getHairStyle()));
        realized.setEyeColor(resolveValue(supported.path("eye").path("color"), requestedCopy.getEyeColor()));
        realized.setBodyType(resolveValue(supported.path("body").path("type"), requestedCopy.getBodyType()));
        realized.setOutfitStyle(resolveValue(supported.path("outfit").path("style"), requestedCopy.getOutfitStyle()));
        realized.setOutfitColor(resolveValue(supported.path("outfit").path("color"), requestedCopy.getOutfitColor()));
        realized.setEarType(resolveValue(supported.path("ear"), requestedCopy.getEarType()));
        realized.setWingType(resolveValue(supported.path("wing"), requestedCopy.getWingType()));
        realized.setAccessories(resolveAccessories(supported.path("accessories"), requestedCopy.getAccessories()));
        return new AvatarAppearanceResolution(requestedCopy, realized);
    }

    private AvatarAppearanceConfig copy(AvatarAppearanceConfig source) {
        AvatarAppearanceConfig copy = new AvatarAppearanceConfig();
        if (source == null) {
            copy.setAccessories(List.of());
            return copy;
        }
        copy.setGender(source.getGender());
        copy.setHairColor(source.getHairColor());
        copy.setHairStyle(source.getHairStyle());
        copy.setEyeColor(source.getEyeColor());
        copy.setBodyType(source.getBodyType());
        copy.setOutfitStyle(source.getOutfitStyle());
        copy.setOutfitColor(source.getOutfitColor());
        copy.setEarType(source.getEarType());
        copy.setWingType(source.getWingType());
        copy.setAccessories(source.getAccessories() == null ? List.of() : List.copyOf(source.getAccessories()));
        return copy;
    }

    private JsonNode supportedAttributes(AvatarAsset asset) {
        try {
            JsonNode node = objectMapper.readTree(asset.getSupportedAttributes());
            return node != null && node.isObject() ? node : objectMapper.createObjectNode();
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private String resolveGender(Integer gender) {
        if (gender == null) return "other";
        return switch (gender) {
            case 1 -> "male";
            case 2 -> "female";
            default -> "other";
        };
    }

    private String resolveValue(JsonNode declaredValues, String requested) {
        if (!declaredValues.isArray()) {
            return null;
        }
        for (JsonNode value : declaredValues) {
            if (value.isTextual() && requested != null && requested.equalsIgnoreCase(value.asText())) {
                return requested;
            }
        }
        if (declaredValues.size() == 1 && declaredValues.get(0).isTextual()) {
            String value = declaredValues.get(0).asText();
            return value.isBlank() ? null : value;
        }
        return null;
    }

    private List<String> resolveAccessories(JsonNode declaredValues, List<String> requested) {
        if (!declaredValues.isArray() || requested == null || requested.isEmpty()) {
            return List.of();
        }
        List<String> realized = new ArrayList<>();
        for (String requestedAccessory : requested) {
            if (requestedAccessory == null) continue;
            for (JsonNode declared : declaredValues) {
                if (declared.isTextual() && requestedAccessory.equalsIgnoreCase(declared.asText())) {
                    realized.add(requestedAccessory);
                    break;
                }
            }
        }
        return List.copyOf(realized);
    }
}
