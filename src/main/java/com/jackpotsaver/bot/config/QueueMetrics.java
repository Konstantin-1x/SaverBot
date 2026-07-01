package com.jackpotsaver.bot.config;

import com.jackpotsaver.bot.domain.FileStatus;
import com.jackpotsaver.bot.domain.JobStatus;
import com.jackpotsaver.bot.repository.DownloadJobRepository;
import com.jackpotsaver.bot.repository.StoredFileRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class QueueMetrics {
    public QueueMetrics(MeterRegistry registry, DownloadJobRepository jobs, StoredFileRepository files) {
        for (JobStatus status : JobStatus.values()) {
            Gauge.builder("jackpot_download_jobs", jobs, repository -> repository.countByStatus(status))
                    .tag("status", status.name())
                    .description("Download jobs by status")
                    .register(registry);
        }
        Gauge.builder("jackpot_storage_available_bytes", files, StoredFileRepository::totalAvailableBytes)
                .description("Bytes occupied by available cached files")
                .baseUnit("bytes")
                .register(registry);
        Gauge.builder("jackpot_storage_staging_files", files,
                        repository -> repository.countByStatus(FileStatus.STAGING))
                .description("Files waiting to be published")
                .register(registry);
    }
}
