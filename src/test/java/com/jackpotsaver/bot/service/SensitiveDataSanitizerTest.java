package com.jackpotsaver.bot.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveDataSanitizerTest {
    @Test
    void removesTelegramTokensAndUrls() {
        String value = """
                POST https://api.telegram.org/bot123456789:ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcd/sendVideo
                source=https://youtube.com/shorts/private-id?token=value
                token=123456789:ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcd
                """;

        String sanitized = SensitiveDataSanitizer.sanitize(value);

        assertThat(sanitized)
                .doesNotContain("123456789:")
                .doesNotContain("youtube.com")
                .doesNotContain("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
                .contains("[TOKEN_REDACTED]")
                .contains("[URL]");
    }
}
