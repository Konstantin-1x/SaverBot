package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.domain.FileStatus;
import com.jackpotsaver.bot.domain.StoredFile;
import com.jackpotsaver.bot.repository.StoredFileRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CacheService {
    private final StoredFileRepository repository;
    private final Clock clock;

    public CacheService(StoredFileRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Optional<StoredFile> findReusable(String resourceKey) {
        return repository.findFirstByDownloadRequestResourceKeyAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        resourceKey, FileStatus.AVAILABLE, clock.instant())
                .filter(file -> Files.exists(Path.of(file.getFilePath())));
    }
}
