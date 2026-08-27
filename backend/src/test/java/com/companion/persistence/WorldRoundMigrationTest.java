package com.companion.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorldRoundMigrationTest {

    @Test
    void v16AddsLeaseFencingRecoveryIndexAndParticipantUniqueness() throws Exception {
        String url = "jdbc:h2:mem:world_v16;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V14__create_world_participant_foundation.sql"));
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V15__create_world_round_event.sql"));
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V16__add_world_execution_recovery.sql"));

            assertThat(columnType(statement, "t_world_round", "execution_version")).isEqualTo(Types.BIGINT);
            assertThat(columnType(statement, "t_world_round", "lease_until")).isEqualTo(Types.TIMESTAMP);
            assertThat(indexExists(statement, "idx_world_round_recovery")).isTrue();
            assertThat(constraintExists(statement, "uk_world_event_participant")).isTrue();

            statement.executeUpdate("INSERT INTO t_world (owner_user_id, name) VALUES (1, 'world')");
            statement.executeUpdate("INSERT INTO t_world_round (world_id, request_id, user_input, status) "
                    + "VALUES (1, 'request', 'hello', 'PENDING')");
            assertThat(value(statement, "SELECT execution_version FROM t_world_round WHERE id=1")).isEqualTo(0L);

            statement.executeUpdate("INSERT INTO t_world_event "
                    + "(round_id, sequence_no, participant_id, event_type, content, status) "
                    + "VALUES (1, 1, NULL, 'USER_MESSAGE', 'hello', 'COMPLETED')");
            statement.executeUpdate("INSERT INTO t_world_event "
                    + "(round_id, sequence_no, participant_id, event_type, content, status) "
                    + "VALUES (1, 2, 10, 'AI_MESSAGE', 'one', 'COMPLETED')");
            assertThatThrownBy(() -> statement.executeUpdate("INSERT INTO t_world_event "
                    + "(round_id, sequence_no, participant_id, event_type, content, status) "
                    + "VALUES (1, 3, 10, 'AI_MESSAGE', 'duplicate', 'COMPLETED')"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Test
    void v15CreatesRoundEventConstraintsIndexesDefaultsAndCascades() throws Exception {
        String url = "jdbc:h2:mem:world_v15;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V14__create_world_participant_foundation.sql"));
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/migration/V15__create_world_round_event.sql"));

            assertThat(tableExists(statement, "t_world_round")).isTrue();
            assertThat(tableExists(statement, "t_world_event")).isTrue();
            assertThat(indexExists(statement, "idx_world_round_status_created")).isTrue();
            assertThat(indexExists(statement, "idx_world_event_round_sequence")).isTrue();
            assertThat(indexExists(statement, "idx_world_event_participant")).isTrue();
            assertThat(constraintExists(statement, "uk_world_round_request")).isTrue();
            assertThat(constraintExists(statement, "uk_world_event_sequence")).isTrue();
            assertThat(columnType(statement, "t_world_round", "world_id")).isEqualTo(Types.BIGINT);
            assertThat(columnType(statement, "t_world_event", "round_id")).isEqualTo(Types.BIGINT);
            assertThat(columnType(statement, "t_world_event", "participant_id")).isEqualTo(Types.BIGINT);

            statement.executeUpdate("INSERT INTO t_world (owner_user_id, name) VALUES (1, 'world')");
            statement.executeUpdate("INSERT INTO t_world_round (world_id, request_id, user_input, status) "
                    + "VALUES (1, 'request', 'hello', 'PENDING')");
            statement.executeUpdate("INSERT INTO t_world_event (round_id, sequence_no, event_type, content, status) "
                    + "VALUES (1, 1, 'USER_MESSAGE', 'hello', 'COMPLETED')");
            assertThat(value(statement, "SELECT create_time FROM t_world_round WHERE id=1")).isNotNull();
            assertThat(value(statement, "SELECT update_time FROM t_world_round WHERE id=1")).isNotNull();
            assertThat(value(statement, "SELECT create_time FROM t_world_event WHERE id=1")).isNotNull();

            assertThatThrownBy(() -> statement.executeUpdate(
                    "INSERT INTO t_world_round (world_id, request_id, user_input, status) "
                            + "VALUES (1, 'request', 'duplicate', 'PENDING')"))
                    .isInstanceOf(Exception.class);
            assertThatThrownBy(() -> statement.executeUpdate(
                    "INSERT INTO t_world_event (round_id, sequence_no, event_type, content, status) "
                            + "VALUES (1, 1, 'USER_MESSAGE', 'duplicate', 'COMPLETED')"))
                    .isInstanceOf(Exception.class);

            statement.executeUpdate("DELETE FROM t_world WHERE id=1");
            assertThat(count(statement, "SELECT COUNT(*) FROM t_world_round")).isZero();
            assertThat(count(statement, "SELECT COUNT(*) FROM t_world_event")).isZero();
        }
    }

    private boolean tableExists(Statement statement, String name) throws Exception {
        return count(statement, "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='" + name + "'") > 0;
    }

    private boolean indexExists(Statement statement, String name) throws Exception {
        return count(statement, "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE INDEX_NAME='" + name + "'") > 0;
    }

    private boolean constraintExists(Statement statement, String name) throws Exception {
        return count(statement, "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE CONSTRAINT_NAME='"
                + name + "'") > 0;
    }

    private int count(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private Object value(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            return result.next() ? result.getObject(1) : null;
        }
    }

    private int columnType(Statement statement, String table, String column) throws Exception {
        try (ResultSet result = statement.getConnection().getMetaData()
                .getColumns(null, null, table, column)) {
            return result.next() ? result.getInt("DATA_TYPE") : Integer.MIN_VALUE;
        }
    }
}
