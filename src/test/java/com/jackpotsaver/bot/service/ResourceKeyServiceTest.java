package com.jackpotsaver.bot.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jackpotsaver.bot.config.SecurityProperties;
import com.jackpotsaver.bot.domain.MediaType;
import com.jackpotsaver.bot.domain.VideoQuality;
import org.junit.jupiter.api.Test;

class ResourceKeyServiceTest {
    @Test
    void producesStableOpaqueFingerprint() {
        ResourceKeyService service = new ResourceKeyService(
                new SecurityProperties("test-key-with-at-least-thirty-two-characters"));

        String first = service.create(
                "https://example.test/watch?v=secret", MediaType.YOUTUBE_VIDEO, VideoQuality.LOW);
        String second = service.create(
                "https://example.test/watch?v=secret", MediaType.YOUTUBE_VIDEO, VideoQuality.LOW);

        assertThat(first).isEqualTo(second).matches("[0-9a-f]{64}");
        assertThat(first).doesNotContain("secret");
    }

    @Test
    void qualityChangesFingerprint() {
        ResourceKeyService service = new ResourceKeyService(
                new SecurityProperties("test-key-with-at-least-thirty-two-characters"));

        assertThat(service.create("https://example.test", MediaType.YOUTUBE_VIDEO, VideoQuality.LOW))
                .isNotEqualTo(service.create(
                        "https://example.test", MediaType.YOUTUBE_VIDEO, VideoQuality.HIGH));
    }
}
