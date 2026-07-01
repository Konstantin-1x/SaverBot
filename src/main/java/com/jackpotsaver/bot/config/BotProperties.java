package com.jackpotsaver.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bot")
public record BotProperties(String token, String username, Polling polling, Network network) {
    public record Polling(boolean enabled, int timeoutSeconds) {
    }

    public record Network(int connectTimeoutSeconds, int responseTimeoutSeconds,
                          int maxRetries, long retryBaseDelayMillis) {
    }

    public boolean configured() {
        return token != null && !token.isBlank();
    }
}
