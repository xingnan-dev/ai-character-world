package com.companion.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class WorldParticipantMigrationTest {

    @Test
    void v14CreatesWorldFoundationWithRequiredIndexesAndConstraints() throws Exception {
        String url = "jdbc:h2:mem:world_v14;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V14__create_world_participant_foundation.sql")
            );

            assertThat(tableExists(statement, "t_world")).isTrue();
            assertThat(tableExists(statement, "t_world_participant")).isTrue();
            assertThat(columnExists(statement, "t_world_participant", "character_snapshot")).isTrue();
            assertThat(indexExists(statement, "idx_world_owner_active_created")).isTrue();
            assertThat(indexExists(statement, "idx_world_participant_order")).isTrue();
            assertThat(indexExists(statement, "idx_world_participant_source")).isTrue();
            assertThat(constraintExists(statement, "uk_world_participant_character")).isTrue();
            assertThat(constraintExists(statement, "uk_world_participant_order")).isTrue();
        }
    }

    private boolean tableExists(Statement statement, String name) throws Exception {
        return count(statement, "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + name + "'");
    }

    private boolean columnExists(Statement statement, String table, String column) throws Exception {
        return count(statement, "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '"
                + table + "' AND COLUMN_NAME = '" + column + "'");
    }

    private boolean indexExists(Statement statement, String name) throws Exception {
        return count(statement, "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE INDEX_NAME = '" + name + "'");
    }

    private boolean constraintExists(Statement statement, String name) throws Exception {
        return count(statement, "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                + "WHERE CONSTRAINT_NAME = '" + name + "'");
    }

    private boolean count(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            return result.next() && result.getInt(1) > 0;
        }
    }
}
