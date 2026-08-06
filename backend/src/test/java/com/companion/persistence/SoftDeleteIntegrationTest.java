package com.companion.persistence;

import com.companion.entity.Avatar;
import com.companion.entity.AvatarAsset;
import com.companion.entity.ChatMessage;
import com.companion.entity.ChatSession;
import com.companion.entity.UserMemory;
import com.companion.mapper.AvatarAssetMapper;
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.ChatMessageMapper;
import com.companion.mapper.ChatSessionMapper;
import com.companion.mapper.UserMemoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("soft-delete-test")
@Transactional
class SoftDeleteIntegrationTest {

    @Autowired
    private AvatarMapper avatarMapper;

    @Autowired
    private AvatarAssetMapper avatarAssetMapper;

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private UserMemoryMapper userMemoryMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("删除Avatar后记录保留、普通查询不可见且status不变")
    void avatarDeleteShouldUseDeletedColumn() {
        Avatar avatar = new Avatar();
        avatar.setUserId(1001L);
        avatar.setName("soft-delete-avatar");
        avatar.setStatus(1);
        avatarMapper.insert(avatar);

        assertThat(avatarMapper.deleteById(avatar.getId())).isEqualTo(1);
        assertThat(avatarMapper.selectById(avatar.getId())).isNull();
        assertDeletedInDatabase("t_avatar", avatar.getId(), 1);
    }

    @Test
    @DisplayName("删除聊天会话后逻辑删除生效")
    void chatSessionDeleteShouldUseDeletedColumn() {
        ChatSession session = new ChatSession();
        session.setUserId(1001L);
        session.setAvatarId(2001L);
        session.setTitle("soft-delete-session");
        session.setStatus(1);
        chatSessionMapper.insert(session);

        assertThat(chatSessionMapper.deleteById(session.getId())).isEqualTo(1);
        assertThat(chatSessionMapper.selectById(session.getId())).isNull();
        assertDeletedInDatabase("t_chat_session", session.getId(), 1);
    }

    @Test
    @DisplayName("删除聊天消息后逻辑删除生效")
    void chatMessageDeleteShouldUseDeletedColumn() {
        ChatMessage message = new ChatMessage();
        message.setSessionId(3001L);
        message.setRole(1);
        message.setContent("soft-delete-message");
        chatMessageMapper.insert(message);

        assertThat(chatMessageMapper.deleteById(message.getId())).isEqualTo(1);
        assertThat(chatMessageMapper.selectById(message.getId())).isNull();
        assertDeletedInDatabase("t_chat_message", message.getId(), null);
    }

    @Test
    @DisplayName("批量清理Memory时记录保留且普通查询不可见")
    void memoryDeleteWrapperShouldUseDeletedColumn() {
        UserMemory memory = new UserMemory();
        memory.setUserId(1001L);
        memory.setMemoryKey("soft-delete-memory");
        memory.setValue("value");
        memory.setStatus(1);
        userMemoryMapper.insert(memory);

        assertThat(userMemoryMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserMemory>()
                        .eq("user_id", 1001L)
        )).isEqualTo(1);
        assertThat(userMemoryMapper.selectById(memory.getId())).isNull();
        assertDeletedInDatabase("t_user_memory", memory.getId(), 1);
    }

    @Test
    @DisplayName("下架资源仍可按ID查询且不会被视为已删除")
    void offlineAssetShouldNotBeLogicallyDeleted() {
        AvatarAsset asset = new AvatarAsset();
        asset.setName("offline-asset");
        asset.setAssetType(1);
        asset.setFileUrl("/test/offline.vrm");
        asset.setStatus(0);
        avatarAssetMapper.insert(asset);

        AvatarAsset selected = avatarAssetMapper.selectById(asset.getId());
        assertThat(selected).isNotNull();
        assertThat(selected.getStatus()).isZero();
        assertThat(selected.getDeleted()).isZero();
    }

    private void assertDeletedInDatabase(String table, Long id, Integer expectedStatus) {
        Integer deleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM " + table + " WHERE id = ?", Integer.class, id
        );
        assertThat(deleted).isEqualTo(1);

        if (expectedStatus != null) {
            Integer status = jdbcTemplate.queryForObject(
                    "SELECT status FROM " + table + " WHERE id = ?", Integer.class, id
            );
            assertThat(status).isEqualTo(expectedStatus);
        }
    }
}
