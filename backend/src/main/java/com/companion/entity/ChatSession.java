package com.companion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_chat_session")
public class ChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long avatarId;
    private Long characterId;
    private String characterSnapshot;
    private Integer characterSnapshotVersion;

    private Long userCharacterId;
    private String userCharacterSnapshot;
    private Integer userCharacterSnapshotVersion;

    private Long personalityId;

    @TableField("personality_snapshot")
    private String personalitySnapshot;

    @TableField("personality_snapshot_version")
    private Integer personalitySnapshotVersion;

    private String title;

    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
