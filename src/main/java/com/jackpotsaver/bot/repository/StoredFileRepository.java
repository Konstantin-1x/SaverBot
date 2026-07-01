package com.jackpotsaver.bot.repository;

import com.jackpotsaver.bot.domain.FileStatus;
import com.jackpotsaver.bot.domain.StoredFile;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    Optional<StoredFile> findFirstByDownloadRequestResourceKeyAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            String resourceKey,
            FileStatus status,
            Instant now
    );

    List<StoredFile> findByStatusAndExpiresAtBefore(FileStatus status, Instant now);

    List<StoredFile> findByStatusAndCreatedAtBefore(FileStatus status, Instant before);

    long countByStatus(FileStatus status);

    @Query(value = "select coalesce(sum(file_size), 0) from stored_files where status = 'AVAILABLE'", nativeQuery = true)
    long totalAvailableBytes();

    @Query("select file.filePath from StoredFile file where file.status in :statuses")
    List<String> findPathsByStatusIn(List<FileStatus> statuses);
}
