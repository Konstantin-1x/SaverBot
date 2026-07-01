package com.jackpotsaver.bot.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "retention")
public record RetentionProperties(
        @Min(1) int telegramUpdatesDays,
        @Min(1) int errorLogsDays,
        @Min(1) int adminActionsDays,
        @Min(1) int requestsDays,
        @Min(1) int deletedFilesDays
) {
}

