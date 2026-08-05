package com.companion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvatarGenerateResponse {

    private AvatarVO avatar;

    private PersonalityVO personality;

    private List<AvatarAttributeVO> attributes;

    private String reasoning;

    private Map<String, Object> parsedResult;

    private String message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvatarAttributeVO {
        private String category;
        private String attrKey;
        private String attrValue;
        private Integer sortOrder;
    }
}
