package com.companion.avatar.generation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvatarAppearanceNormalizerTest {

    private final AvatarAppearanceNormalizer normalizer = new AvatarAppearanceNormalizer();

    @Test
    void normalizesUnknownValuesAndDeduplicatesAccessories() {
        AvatarAppearanceConfig input = new AvatarAppearanceConfig(
                "princess", "rainbow", "unknown", "laser", "giant",
                "spacesuit", "transparent", "robot", "huge",
                List.of("glasses", "glasses", "headset", "untrusted", "earrings", "necklace", "hat", "ribbon")
        );

        AvatarAppearanceConfig result = normalizer.normalize(input);

        assertThat(result.getGender()).isEqualTo("female");
        assertThat(result.getHairColor()).isEqualTo("black");
        assertThat(result.getHairStyle()).isEqualTo("long");
        assertThat(result.getEyeColor()).isEqualTo("blue");
        assertThat(result.getBodyType()).isEqualTo("slim");
        assertThat(result.getOutfitStyle()).isEqualTo("casual");
        assertThat(result.getOutfitColor()).isEqualTo("white");
        assertThat(result.getEarType()).isEqualTo("human");
        assertThat(result.getWingType()).isEqualTo("none");
        assertThat(result.getAccessories()).containsExactly("glasses", "headset", "earrings", "necklace", "hat");
    }
}
