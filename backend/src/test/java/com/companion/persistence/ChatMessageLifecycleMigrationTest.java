package com.companion.persistence;

import com.companion.entity.enums.ChatMessageStatus;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageLifecycleMigrationTest {

    @Test
    void migratesEveryHistoricalMessageToCompletedWithoutInferringCompletionTime() throws Exception {
        String url = "jdbc:h2:mem:chat_message_v8;MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE t_chat_message (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        session_id BIGINT NOT NULL,
                        role TINYINT NOT NULL,
                        content VARCHAR(4000) NOT NULL,
                        emotion VARCHAR(50),
                        deleted TINYINT NOT NULL DEFAULT 0,
                        create_time DATETIME
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO t_chat_message(session_id, role, content)
                    VALUES (10, 1, 'historical user message'), (10, 2, 'historical assistant message')
                    """);

            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V8__add_chat_message_lifecycle.sql")
            );

            try (ResultSet rows = statement.executeQuery("""
                    SELECT status, error_code, error_message, update_time, completion_time
                    FROM t_chat_message
                    ORDER BY id
                    """)) {
                int count = 0;
                while (rows.next()) {
                    count++;
                    assertThat(rows.getInt("status")).isEqualTo(ChatMessageStatus.COMPLETED.getCode());
                    assertThat(rows.getString("error_code")).isNull();
                    assertThat(rows.getString("error_message")).isNull();
                    assertThat(rows.getTimestamp("update_time")).isNotNull();
                    assertThat(rows.getTimestamp("completion_time")).isNull();
                }
                assertThat(count).isEqualTo(2);
            }
        }
    }
}
