package com.jackpotsaver.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "limits")
public record LimitProperties(int requestCooldownSeconds, int downloadsPerDay) {
}
