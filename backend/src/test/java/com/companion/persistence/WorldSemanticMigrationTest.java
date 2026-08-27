package com.companion.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class WorldSemanticMigrationTest {
    @Test
    void v17AddsNullableSemanticColumnsWithoutTheme() throws Exception {
        String url = "jdbc:h2:mem:world_v17;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V14__create_world_participant_foundation.sql"));
            statement.executeUpdate("INSERT INTO t_world(owner_user_id,name) VALUES(1,'legacy')");
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V17__extend_world_semantics.sql"));

            assertThat(nullableColumn(statement, "atmosphere", 500)).isTrue();
            assertThat(nullableColumn(statement, "scene", 1000)).isTrue();
            assertThat(nullableColumn(statement, "source_description", 2000)).isTrue();
            assertThat(columnExists(statement, "theme_config")).isFalse();
            try (ResultSet result = statement.executeQuery(
                    "SELECT atmosphere,scene,source_description FROM t_world WHERE name='legacy'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isNull();
                assertThat(result.getString(2)).isNull();
                assertThat(result.getString(3)).isNull();
            }
        }
    }

    private boolean nullableColumn(Statement statement, String column, int size) throws Exception {
        try (ResultSet result = statement.executeQuery("SELECT IS_NULLABLE,CHARACTER_MAXIMUM_LENGTH "
                + "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='t_world' AND COLUMN_NAME='" + column + "'")) {
            return result.next() && "YES".equals(result.getString(1)) && result.getInt(2) == size;
        }
    }

    private boolean columnExists(Statement statement, String column) throws Exception {
        try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_NAME='t_world' AND COLUMN_NAME='" + column + "'")) {
            return result.next() && result.getInt(1) > 0;
        }
    }
}
