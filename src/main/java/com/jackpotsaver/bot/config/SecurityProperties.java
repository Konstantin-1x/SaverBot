package com.jackpotsaver.bot.config;

import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security")
public record SecurityProperties(
        @Size(min = 32, message = "security.data-hash-key must contain at least 32 characters")
        String dataHashKey
) {
}

