package com.companion.character.model;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CharacterProfile {

    @Size(max = 10, message = "values最多10项")
    private List<@Size(max = 200, message = "values单项不能超过200字符") String> values = new ArrayList<>();

    @Size(max = 10, message = "likes最多10项")
    private List<@Size(max = 200, message = "likes单项不能超过200字符") String> likes = new ArrayList<>();

    @Size(max = 10, message = "dislikes最多10项")
    private List<@Size(max = 200, message = "dislikes单项不能超过200字符") String> dislikes = new ArrayList<>();

    @Size(max = 10, message = "interests最多10项")
    private List<@Size(max = 200, message = "interests单项不能超过200字符") String> interests = new ArrayList<>();

    @Size(max = 10, message = "fears最多10项")
    private List<@Size(max = 200, message = "fears单项不能超过200字符") String> fears = new ArrayList<>();

    @Size(max = 10, message = "secrets最多10项")
    private List<@Size(max = 200, message = "secrets单项不能超过200字符") String> secrets = new ArrayList<>();

    @Size(max = 10, message = "behaviorTendencies最多10项")
    private List<@Size(max = 200, message = "behaviorTendencies单项不能超过200字符") String> behaviorTendencies = new ArrayList<>();
}
