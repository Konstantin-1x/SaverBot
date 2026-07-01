package com.jackpotsaver.bot.service;

import java.util.regex.Pattern;

public final class SensitiveDataSanitizer {
    private static final int MAX_LENGTH = 16_000;
    private static final Pattern BOT_TOKEN = Pattern.compile("bot\\d+:[A-Za-z0-9_-]+");
    private static final Pattern TELEGRAM_TOKEN = Pattern.compile("\\d{6,}:[A-Za-z0-9_-]{20,}");
    private static final Pattern URL = Pattern.compile("https?://\\S+");

    private SensitiveDataSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = BOT_TOKEN.matcher(value).replaceAll("bot[REDACTED]");
        sanitized = TELEGRAM_TOKEN.matcher(sanitized).replaceAll("[TOKEN_REDACTED]");
        sanitized = URL.matcher(sanitized).replaceAll("[URL]");
        return sanitized.length() <= MAX_LENGTH ? sanitized : sanitized.substring(0, MAX_LENGTH);
    }
}

