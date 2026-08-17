package com.companion.avatar.generation;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "avatar.asset")
public class AvatarAssetProperties {

    /**
     * Existing verified exact matches score well above 10. A score below 10 represents
     * a weak or negative match and must not be persisted as a generated 3D avatar.
     */
    private int minimumScore = 10;
}
