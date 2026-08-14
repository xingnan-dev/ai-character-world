package com.companion.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.avatar.generation.AvatarAppearanceConfig;
import com.companion.entity.AvatarAsset;
import com.companion.mapper.AvatarAssetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelMatcher {

    private final AvatarAssetMapper avatarAssetMapper;

    private static final String DEFAULT_MODEL_URL = "/models/avatars/nova.vrm";

    public AvatarAsset match(AvatarAppearanceConfig appearance) {
        if (appearance == null) {
            return defaultAsset();
        }

        // Step 1: Query all available VRM assets
        List<AvatarAsset> assets = avatarAssetMapper.selectList(
                new QueryWrapper<AvatarAsset>()
                        .eq("asset_type", 1)
                        .eq("status", 1)
        );

        if (assets.isEmpty()) {
            log.warn("No available VRM asset; using built-in default");
            return defaultAsset();
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
            return bestAsset;
        }
        return defaultAsset();
    }

    private int calculateMatchScore(AvatarAsset asset, AvatarAppearanceConfig app) {
        int score = 0;
        String colorTags = asset.getColorTags() != null ? asset.getColorTags().toLowerCase() : "";
        String styleTags = asset.getStyleTags() != null ? asset.getStyleTags().toLowerCase() : "";
        String supported = asset.getSupportedAttributes() != null ? asset.getSupportedAttributes().toLowerCase() : "";

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
            } else if (supported.contains("hair") && supported.contains("\"color\"") && supported.contains(hc)) {
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
            } else if (supported.contains("eye") && supported.contains("\"color\"") && supported.contains(ec)) {
                score += 6;
            }
        }

        // 4. Outfit style matching (+12, the most important differentiator)
        if (app.getOutfitStyle() != null && !app.getOutfitStyle().isEmpty()) {
            String os = app.getOutfitStyle().toLowerCase();
            if (styleTags.contains(os)) {
                score += 12;
            } else if (supported.contains("outfit") && supported.contains(os)) {
                score += 8;
            } else {
                score -= 4;
            }
        }

        // 5. Outfit color matching (+4)
        if (app.getOutfitColor() != null && !app.getOutfitColor().isEmpty()) {
            String oc = app.getOutfitColor().toLowerCase();
            if (colorTags.contains(oc)) {
                score += 4;
            }
        }

        // 6. Hair style matching (+3)
        if (app.getHairStyle() != null && !app.getHairStyle().isEmpty()) {
            String hs = app.getHairStyle().toLowerCase();
            if (supported.contains("hair") && supported.contains(hs)) {
                score += 3;
            }
        }

        // 7. Ear type bonus (+2 for non-human)
        if (app.getEarType() != null && !"human".equals(app.getEarType())) {
            if (supported.contains("ear") && supported.contains(app.getEarType().toLowerCase())) score += 3;
            else score -= 8;
        }

        // 8. Wing type matching (+3 for matching wing type)
        if (app.getWingType() != null && !"none".equals(app.getWingType())) {
            if (supported.contains("wing") && supported.contains(app.getWingType().toLowerCase())) score += 4;
            else score -= 10;
        }

        // Download count bonus
        if (asset.getDownloadCount() != null) {
            score += Math.min(asset.getDownloadCount() / 100, 2);
        }

        return score;
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

    private AvatarAsset defaultAsset() {
        AvatarAsset asset = new AvatarAsset();
        asset.setName("Nova");
        asset.setFileUrl(DEFAULT_MODEL_URL);
        asset.setFileSize(5_475_092L);
        asset.setGender(1);
        asset.setStyleTags("basic,modern,casual");
        asset.setColorTags("brown,blue,teal,black,white");
        asset.setSupportedAttributes("{\"hair\":{\"color\":[\"brown\"],\"style\":[\"short\"]},\"eye\":{\"color\":[\"blue\"]},\"body\":{\"type\":[\"slim\"]},\"outfit\":{\"style\":[\"casual\"],\"color\":[\"teal\",\"black\",\"white\"]},\"ear\":[\"human\"],\"wing\":[\"none\"],\"accessories\":[]}");
        asset.setIsOfficial(1);
        return asset;
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
