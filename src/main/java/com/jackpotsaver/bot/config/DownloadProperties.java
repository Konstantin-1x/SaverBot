package com.jackpotsaver.bot.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "download")
public record DownloadProperties(
        Path tempDir,
        Path storageDir,
        int maxParallelJobs,
        long maxFileSizeMb,
        long fileLifetimeHours,
        long cleanupIntervalMinutes,
        long workerIntervalMs,
        long stalledJobTimeoutMinutes,
        int maxAttempts,
        long retryDelaySeconds,
        long ytDlpTimeoutSeconds,
        String ytDlpCommand,
        Path cookiesFile
) {
}
