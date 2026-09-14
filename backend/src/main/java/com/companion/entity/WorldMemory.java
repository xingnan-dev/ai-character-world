package com.companion.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("t_world_memory") public class WorldMemory { @TableId(type=IdType.AUTO) private Long id; private Long userId; private Long worldId; private String memoryType; private String memoryKey; private String content; private String dedupeHash; private Long sourceRoundId; private Long sourceEventId; private Integer importance; private Integer version; @TableLogic private Integer deleted; private LocalDateTime createTime; private LocalDateTime updateTime; }
