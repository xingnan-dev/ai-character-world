package com.companion.avatar.generation;

/**
 * Separates the normalized appearance requested by the user from the appearance that the
 * selected persisted asset can actually realize.
 */
public record AvatarAppearanceResolution(
        AvatarAppearanceConfig requestedAppearance,
        AvatarAppearanceConfig realizedAppearance
) {
}
