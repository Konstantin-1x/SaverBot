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
}
