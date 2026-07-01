package com.jackpotsaver.bot.repository;

import com.jackpotsaver.bot.domain.DownloadJobSubscriber;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DownloadJobSubscriberRepository extends JpaRepository<DownloadJobSubscriber, Long> {
    List<DownloadJobSubscriber> findByJobIdOrderByIdAsc(Long jobId);
}
