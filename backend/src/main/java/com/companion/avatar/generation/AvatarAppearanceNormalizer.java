package com.companion.avatar.generation;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class AvatarAppearanceNormalizer {

    private static final int MAX_ACCESSORIES = 5;
    private static final int MAX_ACCESSORY_LENGTH = 24;

    private static final Set<String> GENDERS = Set.of("male", "female", "other");
    private static final Set<String> HAIR_COLORS = Set.of("silver", "black", "brown", "blonde", "pink", "blue", "purple", "red", "white");
    private static final Set<String> HAIR_STYLES = Set.of("long", "short", "twin_tail", "ponytail", "bob", "wavy", "straight");
    private static final Set<String> EYE_COLORS = Set.of("blue", "red", "green", "amber", "purple", "black", "heterochromia");
    private static final Set<String> BODY_TYPES = Set.of("petite", "slim", "athletic", "curvy", "tall");
    private static final Set<String> OUTFIT_STYLES = Set.of("tech_future", "gothic", "elegant", "casual", "uniform", "kimono", "armor");
    private static final Set<String> OUTFIT_COLORS = Set.of("black", "white", "red", "blue", "silver", "gold", "purple");
    private static final Set<String> EAR_TYPES = Set.of("human", "cat", "elf", "demon", "none");
    private static final Set<String> WING_TYPES = Set.of("angel", "demon", "mechanical", "energy", "none");
    private static final Set<String> ACCESSORIES = Set.of("glasses", "headset", "earrings", "necklace", "hairpin", "hat", "ribbon", "scarf");

    public AvatarAppearanceConfig normalize(AvatarAppearanceConfig source) {
        AvatarAppearanceConfig input = source == null ? new AvatarAppearanceConfig() : source;
        AvatarAppearanceConfig normalized = new AvatarAppearanceConfig();
        normalized.setGender(allowed(input.getGender(), GENDERS, "female"));
        normalized.setHairColor(allowed(input.getHairColor(), HAIR_COLORS, "black"));
        normalized.setHairStyle(allowed(input.getHairStyle(), HAIR_STYLES, "long"));
        normalized.setEyeColor(allowed(input.getEyeColor(), EYE_COLORS, "blue"));
        normalized.setBodyType(allowed(input.getBodyType(), BODY_TYPES, "slim"));
        normalized.setOutfitStyle(allowed(input.getOutfitStyle(), OUTFIT_STYLES, "casual"));
        normalized.setOutfitColor(allowed(input.getOutfitColor(), OUTFIT_COLORS, "white"));
        normalized.setEarType(allowed(input.getEarType(), EAR_TYPES, "human"));
        normalized.setWingType(allowed(input.getWingType(), WING_TYPES, "none"));
        normalized.setAccessories(normalizeAccessories(input.getAccessories()));
        return normalized;
    }

    private String allowed(String value, Set<String> allowed, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return allowed.contains(normalized) ? normalized : defaultValue;
    }

    private List<String> normalizeAccessories(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (normalized.length() <= MAX_ACCESSORY_LENGTH && ACCESSORIES.contains(normalized)) {
                result.add(normalized);
            }
            if (result.size() == MAX_ACCESSORIES) {
                break;
            }
        }
        return List.copyOf(result);
    }
}
