package com.companion.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank
    private String secret;

    @Min(60)
    private long expiration = 7200;

    @NotBlank
    private String issuer = "ai-virtual-companion";

    @NotBlank
    private String audience = "ai-virtual-companion-web";

    @NotBlank
    private String header = "Authorization";

    @NotBlank
    private String prefix = "Bearer ";
}
