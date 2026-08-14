package com.companion.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class AvatarAssetCalibrationMigrationTest {

    @Test
    void calibratesBundledAssetsWithoutChangingSchema() throws Exception {
        String url = "jdbc:h2:mem:avatar_v12;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE t_avatar_asset (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100), file_url VARCHAR(500), file_size BIGINT, gender TINYINT,
                        style_tags VARCHAR(500), color_tags VARCHAR(500), supported_attributes VARCHAR(4000),
                        description VARCHAR(1000)
                    )
                    """);
            statement.execute("""
                    INSERT INTO t_avatar_asset(name,file_url,file_size,gender,style_tags,color_tags,supported_attributes,description)
                    VALUES ('old nova','/models/avatars/nova.vrm',1,2,'tech','silver','{}','old'),
                           ('old sky','/models/avatars/sky.vrm',1,2,'gothic','purple','{}','old')
                    """);

            ScriptUtils.executeSqlScript(connection, new ClassPathResource(
                    "db/migration/V12__calibrate_avatar_asset_metadata.sql"
            ));

            try (ResultSet nova = statement.executeQuery(
                    "SELECT * FROM t_avatar_asset WHERE file_url='/models/avatars/nova.vrm'")) {
                assertThat(nova.next()).isTrue();
                assertThat(nova.getInt("gender")).isEqualTo(1);
                assertThat(nova.getLong("file_size")).isEqualTo(5_475_092L);
                assertThat(nova.getString("supported_attributes"))
                        .contains("\"ear\":[\"human\"]", "\"wing\":[\"none\"]", "\"accessories\":[]");
            }
            try (ResultSet sky = statement.executeQuery(
                    "SELECT * FROM t_avatar_asset WHERE file_url='/models/avatars/sky.vrm'")) {
                assertThat(sky.next()).isTrue();
                assertThat(sky.getInt("gender")).isZero();
                assertThat(sky.getLong("file_size")).isEqualTo(5_481_132L);
                assertThat(sky.getString("supported_attributes")).doesNotContain("angel", "demon", "elf");
            }
        }
    }
}
