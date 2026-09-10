package com.companion.image;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class CharacterImageGenerationSchemaTest {
    @Test void v19EnforcesPerUserRequestUniquenessInH2() throws Exception {
        String url = "jdbc:h2:mem:image_v19;MODE=MySQL;DB_CLOSE_DELAY=-1";
        String migration = Files.readString(Path.of("src/main/resources/db/migration/V19__create_character_image_generation.sql"));
        try (var connection = DriverManager.getConnection(url)) {
            connection.createStatement().execute(migration);
            insert(connection, 1, "same");
            assertThrows(SQLException.class, () -> insert(connection, 1, "same"));
            assertDoesNotThrow(() -> insert(connection, 2, "same"));
        }
    }

    private void insert(java.sql.Connection connection, long userId, String requestId) throws SQLException {
        try (var statement = connection.prepareStatement("INSERT INTO t_character_image_generation(user_id,request_id,request_hash,prompt,status) VALUES(?,?,?,?,?)")) {
            statement.setLong(1, userId); statement.setString(2, requestId); statement.setString(3, "0".repeat(64));
            statement.setString(4, "p"); statement.setString(5, "PROCESSING"); statement.executeUpdate();
        }
    }
}
