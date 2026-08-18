package com.companion.avatar.generation;

import com.companion.entity.AvatarAsset;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvatarAppearanceResolverTest {

    private final AvatarAppearanceResolver resolver = new AvatarAppearanceResolver(new ObjectMapper());

    @Test
    void matchedAppearanceHasEqualContentButIndependentMutableState() {
        AvatarAppearanceConfig requested = requested();
        AvatarAppearanceResolution resolution = resolver.resolve(requested, fullyMatchingAsset());

        assertThat(resolution.requestedAppearance()).isEqualTo(requested);
        assertThat(resolution.realizedAppearance()).isEqualTo(requested);
        assertThat(resolution.requestedAppearance()).isNotSameAs(requested);
        assertThat(resolution.realizedAppearance()).isNotSameAs(requested);
        assertThat(resolution.realizedAppearance()).isNotSameAs(resolution.requestedAppearance());
        assertThat(resolution.realizedAppearance().getAccessories())
                .isNotSameAs(requested.getAccessories());
    }

    @Test
    void nearestUsesDeclaredBrownHairAndKeepsSupportedBlueEyes() {
        AvatarAsset asset = fullyMatchingAsset();
        asset.setSupportedAttributes(asset.getSupportedAttributes().replace(
                "\"color\":[\"silver\"],\"style\":[\"long\"]",
                "\"color\":[\"brown\"],\"style\":[\"long\"]"));

        AvatarAppearanceConfig realized = resolver.resolve(requested(), asset).realizedAppearance();

        assertThat(realized.getHairColor()).isEqualTo("brown");
        assertThat(realized.getEyeColor()).isEqualTo("blue");
    }

    @Test
    void assetGenderIsAuthoritativeEvenWhenRequestedGenderDiffers() {
        AvatarAsset asset = fullyMatchingAsset();
        asset.setGender(1);

        AvatarAppearanceConfig realized = resolver.resolve(requested(), asset).realizedAppearance();

        assertThat(realized.getGender()).isEqualTo("male");
    }

    @Test
    void undeclaredCapabilitiesNeverCopyRequestedValues() {
        AvatarAsset asset = new AvatarAsset();
        asset.setGender(0);
        asset.setSupportedAttributes("{}");

        AvatarAppearanceConfig realized = resolver.resolve(requested(), asset).realizedAppearance();

        assertThat(realized.getGender()).isEqualTo("other");
        assertThat(realized.getHairColor()).isNull();
        assertThat(realized.getHairStyle()).isNull();
        assertThat(realized.getEyeColor()).isNull();
        assertThat(realized.getBodyType()).isNull();
        assertThat(realized.getOutfitStyle()).isNull();
        assertThat(realized.getOutfitColor()).isNull();
        assertThat(realized.getEarType()).isNull();
        assertThat(realized.getWingType()).isNull();
        assertThat(realized.getAccessories()).isEmpty();
    }

    @Test
    void accessoriesAreLimitedToExplicitlySupportedRequestedIntersection() {
        AvatarAppearanceConfig requested = requested();
        requested.setAccessories(List.of("glasses", "headset", "necklace"));
        AvatarAsset asset = fullyMatchingAsset();
        asset.setSupportedAttributes(asset.getSupportedAttributes().replace(
                "\"accessories\":[\"glasses\"]",
                "\"accessories\":[\"glasses\",\"hat\"]"));

        AvatarAppearanceConfig realized = resolver.resolve(requested, asset).realizedAppearance();

        assertThat(realized.getAccessories()).containsExactly("glasses");
    }

    @Test
    void malformedSupportedAttributesAreHandledAsUnknownCapabilities() {
        AvatarAsset asset = fullyMatchingAsset();
        asset.setSupportedAttributes("{broken");

        AvatarAppearanceConfig realized = resolver.resolve(requested(), asset).realizedAppearance();

        assertThat(realized.getGender()).isEqualTo("female");
        assertThat(realized.getHairColor()).isNull();
        assertThat(realized.getEyeColor()).isNull();
        assertThat(realized.getAccessories()).isEmpty();
    }

    private AvatarAppearanceConfig requested() {
        return new AvatarAppearanceConfig(
                "female", "silver", "long", "blue", "slim", "tech_future", "silver",
                "human", "none", new ArrayList<>(List.of("glasses"))
        );
    }

    private AvatarAsset fullyMatchingAsset() {
        AvatarAsset asset = new AvatarAsset();
        asset.setGender(2);
        asset.setSupportedAttributes("""
                {"hair":{"color":["silver"],"style":["long"]},"eye":{"color":["blue"]},
                 "body":{"type":["slim"]},"outfit":{"style":["tech_future"],"color":["silver"]},
                 "ear":["human"],"wing":["none"],"accessories":["glasses"]}
                """);
        return asset;
    }
}
