package com.companion.testsupport;

import com.companion.AiVirtualCompanionApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.CountDownLatch;

/** Explicit opt-in launcher for local acceptance only; not a default Surefire test. */
class AcceptanceServerLauncher {
    @Test
    void serveUntilInterrupted() throws Exception {
        ConfigurableApplicationContext context = SpringApplication.run(AiVirtualCompanionApplication.class);
        Runtime.getRuntime().addShutdownHook(new Thread(context::close));
        new CountDownLatch(1).await();
    }
}
