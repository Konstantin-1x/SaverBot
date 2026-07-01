package com.jackpotsaver.bot.repository;

import com.jackpotsaver.bot.domain.DownloadJob;
import com.jackpotsaver.bot.domain.JobStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DownloadJobRepository extends JpaRepository<DownloadJob, Long> {
    @Query("""
            select job from DownloadJob job
            where job.status = :status
              and (job.nextAttemptAt is null or job.nextAttemptAt <= :now)
            order by job.priority asc, job.id asc
            """)
    List<DownloadJob> findReadyJobs(@Param("status") JobStatus status, @Param("now") Instant now, Pageable pageable);

    long countByStatus(JobStatus status);

    Optional<DownloadJob> findFirstByDedupKeyAndStatusInOrderByIdAsc(String dedupKey, List<JobStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            update DownloadJob job
            set job.status = :runningStatus,
                job.workerId = :workerId,
                job.startedAt = :startedAt,
                job.heartbeatAt = :startedAt,
                job.nextAttemptAt = null,
                job.attemptCount = job.attemptCount + 1
            where job.id = :id
              and job.status = :createdStatus
              and (job.nextAttemptAt is null or job.nextAttemptAt <= :startedAt)
            """)
    int claimCreatedJob(@Param("id") Long id,
                        @Param("workerId") String workerId,
                        @Param("startedAt") Instant startedAt,
                        @Param("createdStatus") JobStatus createdStatus,
                        @Param("runningStatus") JobStatus runningStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            update DownloadJob job
            set job.heartbeatAt = :heartbeatAt
            where job.status = :runningStatus and job.workerId = :workerId
            """)
    int heartbeatWorkerJobs(@Param("workerId") String workerId,
                            @Param("heartbeatAt") Instant heartbeatAt,
                            @Param("runningStatus") JobStatus runningStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            update DownloadJob job
            set job.status = :createdStatus,
                job.workerId = null,
                job.startedAt = null,
                job.heartbeatAt = null,
                job.nextAttemptAt = :nextAttemptAt,
                job.errorCode = 'LEASE_EXPIRED',
                job.errorDetails = 'Worker heartbeat expired'
            where job.status = :runningStatus
              and coalesce(job.heartbeatAt, job.startedAt) < :heartbeatBefore
              and job.attemptCount < :maxAttempts
            """)
    int requeueStaleRunningJobs(@Param("heartbeatBefore") Instant heartbeatBefore,
                                @Param("nextAttemptAt") Instant nextAttemptAt,
                                @Param("maxAttempts") int maxAttempts,
                                @Param("runningStatus") JobStatus runningStatus,
                                @Param("createdStatus") JobStatus createdStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            update DownloadJob job
            set job.status = :deadLetterStatus,
                job.workerId = null,
                job.heartbeatAt = null,
                job.finishedAt = :finishedAt,
                job.errorCode = 'LEASE_EXPIRED',
                job.errorDetails = 'Worker heartbeat expired after maximum attempts'
            where job.status = :runningStatus
              and coalesce(job.heartbeatAt, job.startedAt) < :heartbeatBefore
              and job.attemptCount >= :maxAttempts
            """)
    int deadLetterStaleRunningJobs(@Param("heartbeatBefore") Instant heartbeatBefore,
                                   @Param("finishedAt") Instant finishedAt,
                                   @Param("maxAttempts") int maxAttempts,
                                   @Param("runningStatus") JobStatus runningStatus,
                                   @Param("deadLetterStatus") JobStatus deadLetterStatus);
}
