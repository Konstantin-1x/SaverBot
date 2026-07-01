package com.jackpotsaver.bot.repository;

import com.jackpotsaver.bot.domain.ErrorLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {
    long countByCreatedAtAfter(Instant after);

    List<ErrorLog> findTop10ByOrderByCreatedAtDesc();
}
