package com.jackpotsaver.bot.repository;

import com.jackpotsaver.bot.domain.DownloadRequest;
import com.jackpotsaver.bot.domain.RequestStatus;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DownloadRequestRepository extends JpaRepository<DownloadRequest, Long> {
    long countByCreatedAtAfter(Instant after);

    long countByStatus(RequestStatus status);

    long countByStatusAndCreatedAtAfter(RequestStatus status, Instant after);

    long countByUserIdAndCreatedAtAfter(Long userId, Instant after);

    boolean existsByUserIdAndCreatedAtAfter(Long userId, Instant after);

    Optional<DownloadRequest> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, RequestStatus status);

    Optional<DownloadRequest> findByIdAndUserId(Long id, Long userId);
}
