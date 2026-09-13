package com.companion.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("t_character_growth")
public class CharacterGrowth { @TableId(type=IdType.AUTO) private Long id; private Long userId; private Long characterId; private String growthSummary; private String behaviorAdaptation; private String userUnderstanding; private String growthDirection; private Integer version; @TableLogic private Integer deleted; private LocalDateTime createTime; private LocalDateTime updateTime; }
