package com.companion.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.avatar.generation.AvatarAppearanceConfig;
import com.companion.entity.AvatarAsset;
import com.companion.mapper.AvatarAssetMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelMatcher {

    private final AvatarAssetMapper avatarAssetMapper;

    private final ObjectMapper objectMapper;

    public AssetMatchResult match(AvatarAppearanceConfig appearance) {
        if (appearance == null) {
            return AssetMatchResult.empty();
        }

        // Step 1: Query all available VRM assets
        List<AvatarAsset> assets = avatarAssetMapper.selectList(
                new QueryWrapper<AvatarAsset>()
                        .eq("asset_type", 1)
                        .eq("status", 1)
        );

        if (assets.isEmpty()) {
            log.warn("No available persisted VRM asset");
            return AssetMatchResult.empty();
        }

        // Step 2: Score each asset
        AvatarAsset bestAsset = null;
        int bestScore = Integer.MIN_VALUE;

        for (AvatarAsset asset : assets) {
            int score = calculateMatchScore(asset, appearance);
            if (score > bestScore || (score == bestScore && isPreferred(asset, bestAsset))) {
                bestScore = score;
                bestAsset = asset;
            }
        }

        // Step 3: Return the best match
        if (bestAsset != null) {
            log.info("Avatar asset selected: assetId={}, score={}", bestAsset.getId(), bestScore);
            return new AssetMatchResult(bestAsset, bestScore);
        }
        return AssetMatchResult.empty();
    }

    int calculateMatchScore(AvatarAsset asset, AvatarAppearanceConfig app) {
        int score = 0;
        Set<String> colorTags = tags(asset.getColorTags());
        Set<String> styleTags = tags(asset.getStyleTags());
        JsonNode supported = supportedAttributes(asset);

        // 1. Gender matching (+10 / -5)
        if (app.getGender() != null) {
            Integer assetGender = asset.getGender();
            if (assetGender != null) {
                int targetGender = "male".equalsIgnoreCase(app.getGender()) ? 1 : 2;
                if (assetGender == targetGender) score += 10;
                else if (assetGender != 0) score -= 20;
            }
        }

        // 2. Hair color matching (+8)
        if (app.getHairColor() != null && !app.getHairColor().isEmpty()) {
            String hc = app.getHairColor().toLowerCase();
            if (colorTags.contains(hc)) {
                score += 8;
            } else if (supports(supported.path("hair").path("color"), hc)) {
                score += 8;
            } else {
                score -= 3;
            }
        }

        // 3. Eye color matching (+6)
        if (app.getEyeColor() != null && !app.getEyeColor().isEmpty()) {
            String ec = app.getEyeColor().toLowerCase();
            if (colorTags.contains(ec)) {
                score += 6;
            } else if (supports(supported.path("eye").path("color"), ec)) {
                score += 6;
            }
        }

        // 4. Outfit style matching (+12, the most important differentiator)
        if (app.getOutfitStyle() != null && !app.getOutfitStyle().isEmpty()) {
            String os = app.getOutfitStyle().toLowerCase();
            if (styleTags.contains(os)) {
                score += 12;
            } else if (supports(supported.path("outfit").path("style"), os)) {
                score += 8;
            } else {
                score -= 4;
            }
        }

        // 5. Outfit color matching (+4)
        if (app.getOutfitColor() != null && !app.getOutfitColor().isEmpty()) {
            String oc = app.getOutfitColor().toLowerCase();
            if (colorTags.contains(oc) || supports(supported.path("outfit").path("color"), oc)) {
                score += 4;
            }
        }

        // 6. Hair style matching (+3)
        if (app.getHairStyle() != null && !app.getHairStyle().isEmpty()) {
            String hs = app.getHairStyle().toLowerCase();
            if (supports(supported.path("hair").path("style"), hs)) {
                score += 3;
            }
        }

        // 7. Ear type bonus (+2 for non-human)
        if (app.getEarType() != null && !"human".equals(app.getEarType())) {
            if (supports(supported.path("ear"), app.getEarType())) score += 3;
            else score -= 8;
        }

        // 8. Wing type matching (+3 for matching wing type)
        if (app.getWingType() != null && !"none".equals(app.getWingType())) {
            if (supports(supported.path("wing"), app.getWingType())) score += 4;
            else score -= 10;
        }

        // Download count bonus
        if (asset.getDownloadCount() != null) {
            score += Math.min(asset.getDownloadCount() / 100, 2);
        }

        return score;
    }

    private Set<String> tags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(rawTags.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    private JsonNode supportedAttributes(AvatarAsset asset) {
        try {
            JsonNode node = objectMapper.readTree(asset.getSupportedAttributes());
            return node != null && node.isObject() ? node : objectMapper.createObjectNode();
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private boolean supports(JsonNode values, String requested) {
        if (requested == null || !values.isArray()) {
            return false;
        }
        for (JsonNode value : values) {
            if (requested.equalsIgnoreCase(value.asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean isPreferred(AvatarAsset candidate, AvatarAsset current) {
        if (current == null) return true;
        int candidateOfficial = candidate.getIsOfficial() == null ? 0 : candidate.getIsOfficial();
        int currentOfficial = current.getIsOfficial() == null ? 0 : current.getIsOfficial();
        if (candidateOfficial != currentOfficial) return candidateOfficial > currentOfficial;
        long candidateId = candidate.getId() == null ? Long.MAX_VALUE : candidate.getId();
        long currentId = current.getId() == null ? Long.MAX_VALUE : current.getId();
        return candidateId < currentId;
    }

    public AvatarAsset getAssetByUrl(String modelUrl) {
        if (modelUrl == null || modelUrl.isEmpty()) {
            return null;
        }
        return avatarAssetMapper.selectOne(
                new QueryWrapper<AvatarAsset>()
                        .eq("file_url", modelUrl)
                        .eq("status", 1)
        );
    }

    public List<AvatarAsset> getAllAvailableAssets() {
        return avatarAssetMapper.selectList(
                new QueryWrapper<AvatarAsset>()
                        .eq("asset_type", 1)
                        .eq("status", 1)
                        .orderByDesc("download_count")
        );
    }
}
