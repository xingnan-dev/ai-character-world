package com.companion.testsupport;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestDatabaseSafetyEnvironmentPostProcessorTest {
    private final TestDatabaseSafetyEnvironmentPostProcessor guard =
            new TestDatabaseSafetyEnvironmentPostProcessor();

    @Test
    void acceptsH2WithFlywayDisabledWithoutOpeningAConnection() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:h2:mem:guard")
                .withProperty("spring.flyway.enabled", "false");
        assertDoesNotThrow(() -> guard.postProcessEnvironment(environment, null));
    }

    @Test
    void rejectsNonH2BeforeDatasourceCreation() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://127.0.0.1:3307/ai_virtual_companion")
                .withProperty("spring.flyway.enabled", "false");
        assertThrows(IllegalStateException.class,
                () -> guard.postProcessEnvironment(environment, null));
    }
}
