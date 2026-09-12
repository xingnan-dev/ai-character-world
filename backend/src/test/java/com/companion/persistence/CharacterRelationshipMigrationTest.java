package com.companion.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterRelationshipMigrationTest {
    @Test void createsScopedRelationshipTableAndUniqueConstraint() throws Exception {
        String url = "jdbc:h2:mem:relationship_v22;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE t_character (id BIGINT AUTO_INCREMENT PRIMARY KEY)");
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V22__add_character_relationship.sql"));
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V23__add_character_relationship_version.sql"));
            statement.execute("INSERT INTO t_character(id) VALUES (1), (2)");
            statement.execute("INSERT INTO t_character_relationship(user_id, character_id, stage, summary, interaction_style, recent_change) "
                    + "VALUES (10, 1, 'NEW', 'none', 'reserved', 'initialized')");
            statement.execute("INSERT INTO t_character_relationship(user_id, character_id, stage, summary, interaction_style, recent_change) "
                    + "VALUES (10, 2, 'NEW', 'none', 'reserved', 'initialized')");
            assertThat(count(statement)).isEqualTo(2);
            assertThat(versionColumnDefault(statement)).isEqualTo("0");
            org.junit.jupiter.api.Assertions.assertThrows(java.sql.SQLException.class, () -> statement.execute(
                    "INSERT INTO t_character_relationship(user_id, character_id, stage, summary, interaction_style, recent_change) "
                            + "VALUES (10, 1, 'NEW', 'duplicate', 'reserved', 'initialized')"));
        }
    }

    private int count(Statement statement) throws Exception {
        try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM t_character_relationship")) {
            result.next();
            return result.getInt(1);
        }
    }

    private String versionColumnDefault(Statement statement) throws Exception {
        try (ResultSet result = statement.executeQuery("SELECT COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_NAME='t_character_relationship' AND COLUMN_NAME='version'")) {
            result.next();
            return result.getString(1);
        }
    }
}
