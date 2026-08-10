package com.companion.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryReliabilityMigrationTest {

    @Test
    void createsOnlyTheRequiredMemoryIndexes() throws Exception {
        String url = "jdbc:h2:mem:memory_v11;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE t_user_memory (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        avatar_id BIGINT,
                        memory_key VARCHAR(100),
                        importance REAL DEFAULT 0.5,
                        status TINYINT NOT NULL DEFAULT 1,
                        deleted TINYINT NOT NULL DEFAULT 0
                    )
                    """);

            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V11__enhance_user_memory_reliability.sql")
            );

            assertThat(indexExists(statement, "idx_memory_user_active_importance")).isTrue();
            assertThat(indexExists(statement, "idx_memory_user_key")).isTrue();
        }
    }

    private boolean indexExists(Statement statement, String indexName) throws Exception {
        try (ResultSet result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.INDEXES
                WHERE TABLE_NAME = 't_user_memory' AND INDEX_NAME = '%s'
                """.formatted(indexName))) {
            return result.next() && result.getInt(1) == 1;
        }
    }
}
