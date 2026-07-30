package com.companion;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.companion.mapper")
public class AiVirtualCompanionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiVirtualCompanionApplication.class, args);
    }
}
