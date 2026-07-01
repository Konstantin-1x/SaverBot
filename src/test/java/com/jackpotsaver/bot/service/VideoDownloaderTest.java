package com.jackpotsaver.bot.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jackpotsaver.bot.config.DownloadProperties;
import com.jackpotsaver.bot.domain.VideoQuality;
import java.lang.reflect.Method;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class VideoDownloaderTest {
    @Test
    void mapsUserQualityToYtDlpFormats() throws Exception {
        DownloadProperties properties = new DownloadProperties(
                Path.of("tmp"), Path.of("storage"), 3, 48, 24, 30, 5000, 60, 3, 15, 180, "yt-dlp");
        VideoDownloader downloader = new VideoDownloader(properties);
        Method format = VideoDownloader.class.getDeclaredMethod("format", VideoQuality.class);
        format.setAccessible(true);

        assertThat(format.invoke(downloader, VideoQuality.HIGH).toString()).contains("1080").contains("ext=mp4");
        assertThat(format.invoke(downloader, VideoQuality.MEDIUM).toString()).contains("720").contains("ext=mp4");
        assertThat(format.invoke(downloader, VideoQuality.LOW).toString()).contains("360").contains("ext=mp4");
        assertThat(format.invoke(downloader, VideoQuality.AUTO).toString()).contains("ext=mp4");
    }

    @Test
    void strictYoutubeFormatDoesNotFallbackToUnboundedBest() throws Exception {
        DownloadProperties properties = new DownloadProperties(
                Path.of("tmp"), Path.of("storage"), 3, 48, 24, 30, 5000, 60, 3, 15, 180, "yt-dlp");
        VideoDownloader downloader = new VideoDownloader(properties);
        Method format = VideoDownloader.class.getDeclaredMethod("format", VideoQuality.class, boolean.class);
        format.setAccessible(true);

        assertThat(format.invoke(downloader, VideoQuality.LOW, true).toString())
                .contains("height<=360")
                .doesNotContain("/best");
    }

    @Test
    void regularYoutubeHonorsSelectedQualityAndAutoDefaultsTo720p() throws Exception {
        VideoDownloader downloader = downloader();
        Method resolve = VideoDownloader.class.getDeclaredMethod(
                "resolveQuality", String.class, VideoQuality.class);
        resolve.setAccessible(true);

        assertThat(resolve.invoke(
                downloader, "https://youtube.com/watch?v=test", VideoQuality.HIGH))
                .isEqualTo(VideoQuality.HIGH);
        assertThat(resolve.invoke(
                downloader, "https://youtube.com/watch?v=test", VideoQuality.AUTO))
                .isEqualTo(VideoQuality.MEDIUM);
    }

    @Test
    void shortsDetectionIsLimitedToShortsUrls() throws Exception {
        VideoDownloader downloader = downloader();
        Method isShorts = VideoDownloader.class.getDeclaredMethod("isYouTubeShorts", String.class);
        isShorts.setAccessible(true);

        assertThat(isShorts.invoke(downloader, "https://youtube.com/shorts/abc")).isEqualTo(true);
        assertThat(isShorts.invoke(downloader, "https://youtube.com/watch?v=abc")).isEqualTo(false);
    }

    private VideoDownloader downloader() {
        return new VideoDownloader(new DownloadProperties(
                Path.of("tmp"), Path.of("storage"), 3, 48, 24, 30, 5000, 60, 3, 15, 180, "yt-dlp"));
    }
}
