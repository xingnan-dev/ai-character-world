package com.companion.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("t_character_image_generation")
public class CharacterImageGeneration {
 @TableId(type=IdType.AUTO) private Long id; private Long userId; private Long characterId;
 private String requestId; private String requestHash; private String prompt; private String status;
 private String imagePath; private String imageUrl; private LocalDateTime confirmedAt, createdAt, updatedAt;
}
