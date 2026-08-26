package com.companion.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterMigrationTest {

    @Test
    void v13CreatesCharacterTableAndRequiredIndexes() throws Exception {
        String url = "jdbc:h2:mem:character_v13;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V13__create_character_table.sql")
            );

            assertThat(tableExists(statement, "t_character")).isTrue();
            assertThat(columnExists(statement, "t_character", "profile_config")).isTrue();
            assertThat(columnExists(statement, "t_character", "deleted")).isTrue();
            assertThat(indexExists(statement, "idx_character_user_active_created")).isTrue();
            assertThat(indexExists(statement, "idx_character_avatar_id")).isTrue();
        }
    }

    private boolean tableExists(Statement statement, String tableName) throws Exception {
        try (ResultSet result = statement.executeQuery("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '%s'
                """.formatted(tableName))) {
            return result.next() && result.getInt(1) == 1;
        }
    }

    private boolean columnExists(Statement statement, String tableName, String columnName) throws Exception {
        try (ResultSet result = statement.executeQuery("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = '%s' AND COLUMN_NAME = '%s'
                """.formatted(tableName, columnName))) {
            return result.next() && result.getInt(1) == 1;
        }
    }

    private boolean indexExists(Statement statement, String indexName) throws Exception {
        try (ResultSet result = statement.executeQuery("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE INDEX_NAME = '%s'
                """.formatted(indexName))) {
            return result.next() && result.getInt(1) == 1;
        }
    }
}
