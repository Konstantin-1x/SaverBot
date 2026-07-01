package com.jackpotsaver.bot.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jackpotsaver.bot.domain.MediaType;
import com.jackpotsaver.bot.domain.Platform;
import org.junit.jupiter.api.Test;

class PlatformDetectorTest {
    private final PlatformDetector detector = new PlatformDetector();

    @Test
    void detectsRegularYoutubeWatchLinks() {
        VideoLink link = detector.parse("https://www.youtube.com/watch?v=abc123&t=10").orElseThrow();

        assertThat(link.platform()).isEqualTo(Platform.YOUTUBE);
        assertThat(link.mediaType()).isEqualTo(MediaType.YOUTUBE_VIDEO);
        assertThat(link.qualityRequired()).isTrue();
        assertThat(link.normalizedUrl()).isEqualTo("https://youtube.com/watch?v=abc123");
    }

    @Test
    void stripsTrackingQueryFromShortYoutubeLinks() {
        VideoLink link = detector.parse("https://youtu.be/h6tZr2J2vW0?si=W6vhHnh6oG3WVA8v").orElseThrow();

        assertThat(link.platform()).isEqualTo(Platform.YOUTUBE);
        assertThat(link.normalizedUrl()).isEqualTo("https://youtu.be/h6tZr2J2vW0");
    }

    @Test
    void detectsYoutubeShortsWithoutQualitySelection() {
        VideoLink link = detector.parse("https://youtube.com/shorts/abc123").orElseThrow();

        assertThat(link.platform()).isEqualTo(Platform.YOUTUBE_SHORTS);
        assertThat(link.mediaType()).isEqualTo(MediaType.YOUTUBE_SHORTS);
        assertThat(link.qualityRequired()).isFalse();
    }

    @Test
    void detectsInstagramAndTiktok() {
        assertThat(detector.parse("https://www.instagram.com/reel/abc/").orElseThrow().mediaType())
                .isEqualTo(MediaType.INSTAGRAM_VIDEO);
        assertThat(detector.parse("https://vm.tiktok.com/ZMabc/").orElseThrow().mediaType())
                .isEqualTo(MediaType.TIKTOK_VIDEO);
        assertThat(detector.parse("https://vt.tiktok.com/ZSQ9YbLKp/").orElseThrow().mediaType())
                .isEqualTo(MediaType.TIKTOK_VIDEO);
    }

    @Test
    void rejectsUnsupportedUrlButStillKnowsItIsUrl() {
        assertThat(detector.parse("https://example.com/video")).isEmpty();
        assertThat(detector.isUrl("https://example.com/video")).isTrue();
    }

    @Test
    void rejectsPlainText() {
        assertThat(detector.parse("hello")).isEmpty();
        assertThat(detector.isUrl("hello")).isFalse();
    }
}
