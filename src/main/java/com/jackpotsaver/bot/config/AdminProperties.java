package com.jackpotsaver.bot.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "admin")
public record AdminProperties(String telegramIds) {
    public Set<Long> telegramIdSet() {
        if (telegramIds == null || telegramIds.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(telegramIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Long::parseLong)
                .collect(Collectors.toUnmodifiableSet());
    }
}
