package com.companion.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.ai.dto.AvatarGenerateResult;
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

    public String match(AvatarGenerateResult result) {
        if (result == null || result.getAppearanceConfig() == null) {
            log.warn("ModelMatcher: result或appearanceConfig为空，返回默认模型");
            return DEFAULT_MODEL_URL;
        }

        AvatarGenerateResult.AppearanceConfig app = result.getAppearanceConfig();
        log.info("==== ModelMatcher 开始匹配 ====");
        log.info("角色属性: gender={}, hairColor={}, hairStyle={}, eyeColor={}, earType={}, hasWing={}, wingType={}, outfitStyle={}, outfitColor={}",
                app.getGender(), app.getHairColor(), app.getHairStyle(), app.getEyeColor(),
                app.getEarType(), app.getHasWing(), app.getWingType(), app.getOutfitStyle(), app.getOutfitColor());
        if (result.getPersonality() != null) {
            log.info("性格类型: {}", result.getPersonality().getType());
        }

        // Step 1: Query all available VRM assets
        List<AvatarAsset> assets = avatarAssetMapper.selectList(
                new QueryWrapper<AvatarAsset>()
                        .eq("asset_type", 1)
                        .eq("status", 1)
        );

        log.info("数据库查询到 {} 个可用VRM资源", assets.size());
        for (AvatarAsset a : assets) {
            log.info("  资源: id={}, name={}, fileUrl={}, gender={}, styleTags={}, colorTags={}",
                    a.getId(), a.getName(), a.getFileUrl(), a.getGender(), a.getStyleTags(), a.getColorTags());
        }

        if (assets.isEmpty()) {
            log.warn("没有可用的VRM资源，返回默认模型: {}", DEFAULT_MODEL_URL);
            return DEFAULT_MODEL_URL;
        }

        // Step 2: Score each asset
        AvatarAsset bestAsset = null;
        int bestScore = -1;

        for (AvatarAsset asset : assets) {
            int score = calculateMatchScore(asset, result);
            log.info("资源评分: name={}, score={}", asset.getName(), score);
            if (score > bestScore) {
                bestScore = score;
                bestAsset = asset;
            }
        }

        // Step 3: Return the best match
        if (bestAsset != null) {
            log.info("==== 匹配到最佳模型: {}, score={}, url={} ====", bestAsset.getName(), bestScore, bestAsset.getFileUrl());
            return bestAsset.getFileUrl();
        }

        log.warn("未匹配到任何模型，返回默认: {}", DEFAULT_MODEL_URL);
        return DEFAULT_MODEL_URL;
    }

    private int calculateMatchScore(AvatarAsset asset, AvatarGenerateResult result) {
        int score = 0;
        AvatarGenerateResult.AppearanceConfig app = result.getAppearanceConfig();
        String colorTags = asset.getColorTags() != null ? asset.getColorTags().toLowerCase() : "";
        String styleTags = asset.getStyleTags() != null ? asset.getStyleTags().toLowerCase() : "";
        String supported = asset.getSupportedAttributes() != null ? asset.getSupportedAttributes().toLowerCase() : "";

        // 1. Gender matching (+10 / -5)
        if (app.getGender() != null) {
            Integer assetGender = asset.getGender();
            if (assetGender != null && assetGender != 0) {
                int targetGender = "male".equalsIgnoreCase(app.getGender()) ? 1 : 2;
                if (assetGender == targetGender) score += 10;
                else if (assetGender == 0) score += 5;
                else score -= 5;
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
            score += 2;
            if (supported.contains("ear") && supported.contains(app.getEarType().toLowerCase())) {
                score += 1;
            }
        }

        // 8. Wing type matching (+3 for matching wing type)
        if (app.getHasWing() != null && app.getHasWing()) {
            score += 2;
            if (app.getWingType() != null && !"none".equals(app.getWingType())) {
                if (supported.contains("wing") && supported.contains(app.getWingType().toLowerCase())) {
                    score += 2;
                }
            }
        }

        // 9. Personality-style synergy bonus
        if (result.getPersonality() != null) {
            String personalityType = result.getPersonality().getType();
            if ("cool".equals(personalityType)) {
                if (styleTags.contains("gothic") || styleTags.contains("elegant") || styleTags.contains("mysterious") || styleTags.contains("dark")) {
                    score += 4;
                }
            }
            if ("gentle".equals(personalityType)) {
                if (styleTags.contains("casual") || styleTags.contains("elegant")) {
                    score += 4;
                }
            }
            if ("tsundere".equals(personalityType)) {
                if (styleTags.contains("tech_future") || styleTags.contains("cyberpunk")) {
                    score += 4;
                }
            }
            if ("humorous".equals(personalityType)) {
                if (styleTags.contains("casual")) {
                    score += 4;
                }
            }
            if ("knowledgeable".equals(personalityType)) {
                if (styleTags.contains("elegant") || styleTags.contains("casual")) {
                    score += 4;
                }
            }
        }

        // 10. Download count bonus
        if (asset.getDownloadCount() != null) {
            score += Math.min(asset.getDownloadCount() / 100, 2);
        }

        return score;
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
