package com.companion.dto.request;

import com.companion.character.model.CharacterProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CharacterUpdateRequest {

    private String characterType;

    @Size(min = 1, max = 80, message = "name长度必须为1到80字符")
    private String name;

    @Min(value = 0, message = "age不能小于0")
    @Max(value = 150, message = "age不能大于150")
    private Integer age;

    @Size(max = 200, message = "identity不能超过200字符")
    private String identity;
    @Size(max = 1000, message = "corePersonality不能超过1000字符")
    private String corePersonality;
    @Size(max = 500, message = "currentGoal不能超过500字符")
    private String currentGoal;
    @Size(max = 5000, message = "biography不能超过5000字符")
    private String biography;
    @Size(max = 300, message = "relationshipToUser不能超过300字符")
    private String relationshipToUser;
    @Size(max = 500, message = "speakingStyle不能超过500字符")
    private String speakingStyle;

    @Valid
    private CharacterProfile profile;

    @Size(max = 2000, message = "sourceDescription不能超过2000字符")
    private String sourceDescription;
    private String visualType;
    private Long avatarId;
    @Size(max = 500, message = "imageUrl不能超过500字符")
    private String imageUrl;
    @Size(max = 32, message = "avatarColor不能超过32字符")
    private String avatarColor;
}
