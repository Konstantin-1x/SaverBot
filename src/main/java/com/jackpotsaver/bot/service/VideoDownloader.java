package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.config.DownloadProperties;
import com.jackpotsaver.bot.domain.VideoQuality;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VideoDownloader {
    private static final Logger log = LoggerFactory.getLogger(VideoDownloader.class);
    private final DownloadProperties properties;

    public VideoDownloader(DownloadProperties properties) {
        this.properties = properties;
    }

    public DownloadedVideo download(String url, VideoQuality quality) {
        if (isYouTubeShorts(url)) {
            return downloadYouTubeShorts(url);
        }
        VideoQuality resolvedQuality = resolveQuality(url, quality);
        log.info("Starting yt-dlp download with requestedQuality={} resolvedQuality={}",
                quality, resolvedQuality);
        return downloadSingle(url, resolvedQuality.name(), format(resolvedQuality, isYouTube(url)));
    }

    private DownloadedVideo downloadYouTubeShorts(String url) {
        int[] heights = {1080, 720, 480, 360};
        VideoDownloadException lastFailure = null;
        for (int height : heights) {
            try {
                DownloadedVideo video = downloadSingle(
                        url, "SHORTS_" + height, formatForHeight(height, true));
                long maxBytes = properties.maxFileSizeMb() * 1024 * 1024;
                if (video.sizeBytes() <= maxBytes) {
                    return video;
                }
                Files.deleteIfExists(video.path());
                log.info("YouTube Shorts {}p candidate exceeded the configured size limit; trying lower quality",
                        height);
            } catch (IOException ex) {
                throw new VideoDownloadException("Could not remove oversized Shorts candidate", ex);
            } catch (VideoDownloadException ex) {
                lastFailure = ex;
                if (!allowsQualityFallback(ex)) {
                    throw ex;
                }
                log.info("YouTube Shorts {}p candidate is unavailable or too large; trying lower quality", height);
            }
        }
        throw new VideoDownloadException("No YouTube Shorts quality fits the configured file size", lastFailure);
    }

    private DownloadedVideo downloadSingle(String url, String qualityLabel, String ytDlpFormat) {
        Instant startedAt = Instant.now();
        try {
            log.info("Starting yt-dlp download with resolvedQuality={}", qualityLabel);
            Files.createDirectories(properties.tempDir());
            Files.createDirectories(properties.storageDir());
            Path workDir = properties.tempDir().resolve(UUID.randomUUID().toString());
            Files.createDirectories(workDir);
            Path outputTemplate = workDir.resolve("video.%(ext)s");
            Path outputLog = workDir.resolve("yt-dlp.log");
            List<String> command = new ArrayList<>();
            command.add(properties.ytDlpCommand());
            command.add("--no-playlist");
            command.add("--no-progress");
            command.add("--socket-timeout");
            command.add("8");
            command.add("--retries");
            command.add("1");
            command.add("--fragment-retries");
            command.add("1");
            command.add("--extractor-retries");
            command.add("1");
            command.add("--file-access-retries");
            command.add("1");
            command.add("--retry-sleep");
            command.add("0");
            command.add("--max-filesize");
            command.add(properties.maxFileSizeMb() + "M");
            command.add("--merge-output-format");
            command.add("mp4");
            command.add("-f");
            command.add(ytDlpFormat);
            command.add("-o");
            command.add(outputTemplate.toString());
            command.add(url);
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(outputLog.toFile())
                    .start();
            boolean finished = process.waitFor(properties.ytDlpTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                cleanup(workDir);
                throw new VideoDownloadException("yt-dlp timed out after " + properties.ytDlpTimeoutSeconds() + " seconds");
            }
            String output = Files.exists(outputLog) ? Files.readString(outputLog) : "";
            int exit = process.waitFor();
            if (exit != 0) {
                cleanup(workDir);
                throw new VideoDownloadException("yt-dlp failed: " + output);
            }
            Path downloaded = Files.list(workDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.equals(outputLog))
                    .max(Comparator.comparingLong(this::size))
                    .orElseThrow(() -> new VideoDownloadException("yt-dlp produced no file"));
            Path stored = properties.storageDir().resolve(UUID.randomUUID() + extension(downloaded));
            Files.move(downloaded, stored);
            cleanup(workDir);
            log.info("Downloaded {} bytes to {} with quality {} in {} ms",
                    Files.size(stored), stored, qualityLabel, Duration.between(startedAt, Instant.now()).toMillis());
            return new DownloadedVideo(stored, Files.size(stored));
        } catch (IOException ex) {
            throw new VideoDownloadException("Could not download video", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new VideoDownloadException("Video download interrupted", ex);
        }
    }

    private VideoQuality resolveQuality(String url, VideoQuality quality) {
        if (quality != null && quality != VideoQuality.AUTO) {
            return quality;
        }
        return isYouTube(url) ? VideoQuality.MEDIUM : VideoQuality.LOW;
    }

    private boolean isYouTube(String url) {
        String lower = url == null ? "" : url.toLowerCase();
        return lower.contains("youtube.com") || lower.contains("youtu.be");
    }

    private boolean isYouTubeShorts(String url) {
        String lower = url == null ? "" : url.toLowerCase();
        return lower.contains("youtube.com/shorts/");
    }

    private boolean allowsQualityFallback(VideoDownloadException exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
        return message.contains("max-filesize")
                || message.contains("larger than")
                || message.contains("requested format")
                || message.contains("no video formats")
                || message.contains("does not pass filter");
    }

    private String format(VideoQuality quality) {
        return format(quality, false);
    }

    private String format(VideoQuality quality, boolean strictHeight) {
        return switch (quality == null ? VideoQuality.AUTO : quality) {
            case HIGH -> formatForHeight(1080, strictHeight);
            case MEDIUM -> formatForHeight(720, strictHeight);
            case LOW, AUTO -> formatForHeight(360, strictHeight);
        };
    }

    private String formatForHeight(int maxHeight, boolean strictHeight) {
        String bounded = "bv*[ext=mp4][height<=" + maxHeight + "]+ba[ext=m4a]/"
                + "b[ext=mp4][height<=" + maxHeight + "]/"
                + "bv*[height<=" + maxHeight + "]+ba/"
                + "b[height<=" + maxHeight + "]";
        if (strictHeight) {
            return bounded;
        }
        return "bv*[ext=mp4][height<=" + maxHeight + "]+ba[ext=m4a]/"
                + "b[ext=mp4][height<=" + maxHeight + "]/"
                + "bv*[height<=" + maxHeight + "]+ba/"
                + "b[height<=" + maxHeight + "]/best";
    }

    private long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            return 0;
        }
    }

    private String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : ".mp4";
    }

    private void cleanup(Path directory) {
        try (var files = Files.walk(directory)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
