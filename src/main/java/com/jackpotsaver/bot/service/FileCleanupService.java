package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.domain.FileStatus;
import com.jackpotsaver.bot.repository.StoredFileRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FileCleanupService {
    private static final Logger log = LoggerFactory.getLogger(FileCleanupService.class);
    private final StoredFileRepository repository;
    private final Clock clock;
    private final com.jackpotsaver.bot.config.DownloadProperties properties;

    public FileCleanupService(StoredFileRepository repository, Clock clock,
                              com.jackpotsaver.bot.config.DownloadProperties properties) {
        this.repository = repository;
        this.clock = clock;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "#{${download.cleanup-interval-minutes:30} * 60000}")
    public void cleanupExpiredFiles() {
        var now = clock.instant();
        repository.findByStatusAndExpiresAtBefore(FileStatus.AVAILABLE, now).forEach(file -> {
            try {
                Files.deleteIfExists(Path.of(file.getFilePath()));
                file.markDeleted(now);
                repository.save(file);
                log.info("Deleted expired file {}", file.getId());
            } catch (Exception ex) {
                log.warn("Could not delete expired file {}", file.getId(), ex);
            }
        });
        repository.findByStatusAndCreatedAtBefore(
                FileStatus.STAGING, now.minus(Duration.ofMinutes(properties.stalledJobTimeoutMinutes()))).forEach(file -> {
            try {
                Files.deleteIfExists(Path.of(file.getFilePath()));
                file.markDeleted(now);
                repository.save(file);
                log.info("Deleted abandoned staged file {}", file.getId());
            } catch (Exception ex) {
                log.warn("Could not delete abandoned staged file {}", file.getId(), ex);
            }
        });
        cleanupOrphanFiles(now);
    }

    private void cleanupOrphanFiles(java.time.Instant now) {
        var referenced = new HashSet<Path>();
        repository.findPathsByStatusIn(List.of(FileStatus.STAGING, FileStatus.AVAILABLE)).stream()
                .map(path -> Path.of(path).toAbsolutePath().normalize())
                .forEach(referenced::add);
        if (!Files.isDirectory(properties.storageDir())) {
            return;
        }
        try (var paths = Files.list(properties.storageDir())) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> !referenced.contains(path.toAbsolutePath().normalize()))
                    .filter(path -> oldEnough(path, now))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                            log.info("Deleted orphan file {}", path);
                        } catch (Exception ex) {
                            log.warn("Could not delete orphan file {}", path, ex);
                        }
                    });
        } catch (Exception ex) {
            log.warn("Could not scan storage directory for orphan files", ex);
        }
    }

    private boolean oldEnough(Path path, java.time.Instant now) {
        try {
            return Files.getLastModifiedTime(path).toInstant()
                    .isBefore(now.minus(Duration.ofMinutes(properties.stalledJobTimeoutMinutes())));
        } catch (Exception ex) {
            return false;
        }
    }
}
