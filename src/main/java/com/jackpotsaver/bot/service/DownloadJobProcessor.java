package com.jackpotsaver.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jackpotsaver.bot.config.DownloadProperties;
import com.jackpotsaver.bot.domain.DownloadRequest;
import com.jackpotsaver.bot.domain.JobStatus;
import com.jackpotsaver.bot.domain.Platform;
import com.jackpotsaver.bot.domain.RequestStatus;
import com.jackpotsaver.bot.domain.StoredFile;
import com.jackpotsaver.bot.repository.DownloadJobRepository;
import com.jackpotsaver.bot.repository.DownloadJobSubscriberRepository;
import com.jackpotsaver.bot.repository.DownloadRequestRepository;
import com.jackpotsaver.bot.repository.StoredFileRepository;
import com.jackpotsaver.bot.telegram.TelegramApiClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DownloadJobProcessor {
    private static final Logger log = LoggerFactory.getLogger(DownloadJobProcessor.class);
    private final DownloadJobRepository jobRepository;
    private final StoredFileRepository storedFileRepository;
    private final DownloadJobSubscriberRepository subscriberRepository;
    private final DownloadRequestRepository requestRepository;
    private final VideoDownloader downloader;
    private final TelegramApiClient telegramApiClient;
    private final MessageCatalog messages;
    private final ErrorLogService errorLogService;
    private final AdService adService;
    private final DownloadProperties properties;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public DownloadJobProcessor(DownloadJobRepository jobRepository, StoredFileRepository storedFileRepository,
                                DownloadJobSubscriberRepository subscriberRepository,
                                DownloadRequestRepository requestRepository,
                                VideoDownloader downloader, TelegramApiClient telegramApiClient,
                                MessageCatalog messages, ErrorLogService errorLogService,
                                AdService adService, DownloadProperties properties, Clock clock,
                                TransactionTemplate transactionTemplate) {
        this.jobRepository = jobRepository;
        this.storedFileRepository = storedFileRepository;
        this.subscriberRepository = subscriberRepository;
        this.requestRepository = requestRepository;
        this.downloader = downloader;
        this.telegramApiClient = telegramApiClient;
        this.messages = messages;
        this.errorLogService = errorLogService;
        this.adService = adService;
        this.properties = properties;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
    }

    public void process(Long jobId, String workerId) {
        ProcessingJob processingJob = startJob(jobId, workerId);
        if (processingJob == null) {
            return;
        }
        Instant startedAt = Instant.now();
        DownloadedVideo downloadedVideo = null;
        Long stagedFileId = null;
        log.info("Processing download job {} for request {} status={} quality={}",
                jobId, processingJob.requestId(), processingJob.requestStatus(), processingJob.selectedQuality());
        try {
            DownloadedVideo video = downloader.download(processingJob.normalizedUrl(), processingJob.selectedQuality());
            downloadedVideo = video;
            validateDownloadedFile(video);
            log.info("Job {} downloaded file in {} ms; sizeBytes={}",
                    jobId, Duration.between(startedAt, Instant.now()).toMillis(), video.sizeBytes());
            long maxBytes = properties.maxFileSizeMb() * 1024 * 1024;
            if (video.sizeBytes() > maxBytes) {
                Files.deleteIfExists(video.path());
                failJob(jobId, "FILE_TOO_LARGE", "File is larger than configured Telegram limit", null);
                notifyFailure(processingJob, messages.tooLarge(processingJob.language()));
                return;
            }
            StoredFileSnapshot file = storeDownloadedFile(jobId, video);
            stagedFileId = file.storedFileId();
            Instant sendStartedAt = Instant.now();
            log.info("Sending request {} file {} bytes to Telegram",
                    file.requestId(), file.fileSize());
            JsonNode response = telegramApiClient.sendVideo(file.telegramChatId(), Path.of(file.filePath()),
                    messages.caption(file.platform(), file.language()));
            String telegramFileId = extractTelegramFileId(response);
            finishJob(jobId, file.storedFileId(), telegramFileId);
            deleteLoading(file.telegramChatId(), file.loadingMessageId());
            sendAfterDownloadAd(file.telegramChatId());
            deliverSubscribers(jobId, file, telegramFileId);
            log.info("Sent request {} to Telegram in {} ms; totalJobMs={}; telegramFileIdStored={}",
                    file.requestId(),
                    Duration.between(sendStartedAt, Instant.now()).toMillis(),
                    Duration.between(startedAt, Instant.now()).toMillis(), telegramFileId != null);
        } catch (Exception ex) {
            compensateStagedFile(stagedFileId, downloadedVideo);
            boolean deadLettered = retryOrDeadLetter(jobId, "DOWNLOAD_FAILED", ex.getMessage(), ex);
            if (deadLettered) {
                notifyFailure(processingJob, messages.unavailable(processingJob.language()));
                notifySubscribersFailure(jobId);
            }
        }
    }

    private void deliverSubscribers(Long jobId, StoredFileSnapshot file, String telegramFileId) {
        for (SubscriberSnapshot subscriber : subscriberSnapshots(jobId)) {
            try {
                if (telegramFileId == null || telegramFileId.isBlank()) {
                    telegramApiClient.sendVideo(subscriber.chatId(), Path.of(file.filePath()),
                            messages.caption(file.platform(), subscriber.language()));
                } else {
                    telegramApiClient.sendVideo(subscriber.chatId(), telegramFileId,
                            messages.caption(file.platform(), subscriber.language()));
                }
                markSubscriberSuccess(subscriber.requestId());
                deleteLoading(subscriber.chatId(), subscriber.loadingMessageId());
                sendAfterDownloadAd(subscriber.chatId());
            } catch (RuntimeException ex) {
                markSubscriberFailed(subscriber.requestId(), "TELEGRAM_SEND_FAILED");
                log.warn("Could not deliver shared job {} to request {}", jobId, subscriber.requestId(), ex);
            }
        }
    }

    private void notifySubscribersFailure(Long jobId) {
        for (SubscriberSnapshot subscriber : subscriberSnapshots(jobId)) {
            markSubscriberFailed(subscriber.requestId(), "DOWNLOAD_FAILED");
            notifyFailure(new ProcessingJob(
                    subscriber.requestId(), null, null, RequestStatus.FAILED,
                    subscriber.chatId(), subscriber.loadingMessageId(), subscriber.language()),
                    messages.unavailable(subscriber.language()));
        }
    }

    private java.util.List<SubscriberSnapshot> subscriberSnapshots(Long jobId) {
        return transactionTemplate.execute(status -> subscriberRepository.findByJobIdOrderByIdAsc(jobId).stream()
                .map(item -> {
                    DownloadRequest request = item.getRequest();
                    return new SubscriberSnapshot(
                            request.getId(), request.getTelegramChatId(), request.getLoadingMessageId(),
                            request.getUser().getInterfaceLanguage());
                })
                .toList());
    }

    private void markSubscriberSuccess(Long requestId) {
        transactionTemplate.executeWithoutResult(status -> requestRepository.findById(requestId)
                .ifPresent(request -> {
                    request.status(RequestStatus.SUCCESS, clock.instant());
                    request.redactUrls();
                }));
    }

    private void markSubscriberFailed(Long requestId, String code) {
        transactionTemplate.executeWithoutResult(status -> requestRepository.findById(requestId)
                .ifPresent(request -> {
                    request.fail(code, clock.instant());
                    request.redactUrls();
                }));
    }

    private void validateDownloadedFile(DownloadedVideo video) throws java.io.IOException {
        if (video == null || video.path() == null || !Files.isRegularFile(video.path())) {
            throw new VideoDownloadException("Downloader produced no regular file");
        }
        long actualSize = Files.size(video.path());
        if (actualSize <= 0 || actualSize != video.sizeBytes()) {
            throw new VideoDownloadException("Downloaded file size mismatch");
        }
    }

    private void compensateStagedFile(Long storedFileId, DownloadedVideo video) {
        Path path = video == null ? null : video.path();
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (Exception ex) {
                log.warn("Could not remove staged file {}", path, ex);
            }
        }
        if (storedFileId != null) {
            transactionTemplate.executeWithoutResult(status ->
                    storedFileRepository.findById(storedFileId).ifPresent(storedFileRepository::delete));
        }
    }

    private ProcessingJob startJob(Long jobId, String workerId) {
        return transactionTemplate.execute(status -> {
            int claimed = jobRepository.claimCreatedJob(
                    jobId, workerId, clock.instant(), JobStatus.CREATED, JobStatus.RUNNING);
            if (claimed == 0) {
                return null;
            }
            var job = jobRepository.findById(jobId).orElseThrow();
            DownloadRequest request = job.getRequest();
            return new ProcessingJob(
                    request.getId(),
                    request.getNormalizedUrl(),
                    request.getSelectedQuality(),
                    request.getStatus(),
                    request.getTelegramChatId(),
                    request.getLoadingMessageId(),
                    request.getUser().getInterfaceLanguage()
            );
        });
    }

    private StoredFileSnapshot storeDownloadedFile(Long jobId, DownloadedVideo video) {
        return transactionTemplate.execute(status -> {
            var job = jobRepository.findById(jobId).orElseThrow();
            DownloadRequest request = job.getRequest();
            StoredFile file = storedFileRepository.save(new StoredFile(request, video.path().toString(),
                    video.sizeBytes(), clock.instant(), clock.instant().plus(Duration.ofHours(properties.fileLifetimeHours()))));
            request.status(RequestStatus.SENDING, clock.instant());
            return new StoredFileSnapshot(
                    file.getId(),
                    request.getId(),
                    request.getTelegramChatId(),
                    request.getLoadingMessageId(),
                    file.getFilePath(),
                    file.getFileSize(),
                    request.getPlatform(),
                    request.getUser().getInterfaceLanguage()
            );
        });
    }

    private void finishJob(Long jobId, Long storedFileId, String telegramFileId) {
        transactionTemplate.executeWithoutResult(status -> {
            var job = jobRepository.findById(jobId).orElseThrow();
            DownloadRequest request = job.getRequest();
            StoredFile file = storedFileRepository.findById(storedFileId).orElseThrow();
            file.setTelegramFileId(telegramFileId);
            file.markAvailable();
            request.status(RequestStatus.SUCCESS, clock.instant());
            request.redactUrls();
            job.finish(clock.instant());
        });
    }

    private void failJob(Long jobId, String code, String details, Exception exception) {
        transactionTemplate.executeWithoutResult(status -> {
            var job = jobRepository.findById(jobId).orElse(null);
            if (job == null) {
                return;
            }
            DownloadRequest request = job.getRequest();
            request.fail(code, clock.instant());
            request.redactUrls();
            job.fail(code, details, clock.instant());
            if (exception != null) {
                errorLogService.record(request.getUser(), request, code,
                        SensitiveDataSanitizer.sanitize(details), exception);
            }
        });
    }

    private boolean retryOrDeadLetter(Long jobId, String code, String details, Exception exception) {
        Boolean deadLettered = transactionTemplate.execute(status -> {
            var job = jobRepository.findById(jobId).orElse(null);
            if (job == null) {
                return true;
            }
            DownloadRequest request = job.getRequest();
            boolean exhausted = job.getAttemptCount() >= properties.maxAttempts();
            if (exhausted) {
                request.fail(code, clock.instant());
                request.redactUrls();
                job.deadLetter(code, details, clock.instant());
            } else {
                long multiplier = Math.max(1, job.getAttemptCount());
                Instant nextAttempt = clock.instant().plusSeconds(properties.retryDelaySeconds() * multiplier);
                job.retry(code, details, nextAttempt);
                request.status(RequestStatus.LOADING, clock.instant());
            }
            errorLogService.record(request.getUser(), request, code,
                    SensitiveDataSanitizer.sanitize(details), exception);
            return exhausted;
        });
        if (Boolean.TRUE.equals(deadLettered)) {
            log.error("Download job {} exhausted {} attempts and moved to DEAD_LETTER: {}",
                    jobId, properties.maxAttempts(), SensitiveDataSanitizer.sanitize(exception.getMessage()));
            return true;
        }
        log.warn("Download job {} failed and was scheduled for retry: {}",
                jobId, SensitiveDataSanitizer.sanitize(exception.getMessage()));
        return false;
    }

    private void notifyFailure(ProcessingJob job, String message) {
        try {
            if (job.telegramChatId() != null) {
                telegramApiClient.sendMessage(job.telegramChatId(), message);
                deleteLoading(job.telegramChatId(), job.loadingMessageId());
            }
        } catch (RuntimeException ex) {
            log.warn("Could not notify Telegram about failed request {}", job.requestId(), ex);
        }
    }

    private void deleteLoading(Long telegramChatId, Integer loadingMessageId) {
        try {
            if (telegramChatId != null && loadingMessageId != null) {
                telegramApiClient.deleteMessage(telegramChatId, loadingMessageId);
            }
        } catch (RuntimeException ex) {
            log.warn("Could not delete loading message {}", loadingMessageId, ex);
        }
    }

    private void sendAfterDownloadAd(Long telegramChatId) {
        if (telegramChatId == null) {
            return;
        }
        adService.afterDownloadText().ifPresent(text -> {
            try {
                telegramApiClient.sendMessage(telegramChatId, text);
            } catch (RuntimeException ex) {
                log.warn("Could not send after-download ad", ex);
            }
        });
    }

    private String extractTelegramFileId(JsonNode response) {
        JsonNode result = response.path("result");
        for (String field : new String[]{"video", "document", "animation"}) {
            JsonNode fileId = result.path(field).path("file_id");
            if (fileId.isTextual()) {
                return fileId.asText();
            }
        }
        return null;
    }

    private record ProcessingJob(Long requestId, String normalizedUrl,
                                 com.jackpotsaver.bot.domain.VideoQuality selectedQuality,
                                 RequestStatus requestStatus, Long telegramChatId, Integer loadingMessageId,
                                 com.jackpotsaver.bot.domain.InterfaceLanguage language) {
    }

    private record StoredFileSnapshot(Long storedFileId, Long requestId, Long telegramChatId, Integer loadingMessageId,
                                      String filePath, long fileSize, Platform platform,
                                      com.jackpotsaver.bot.domain.InterfaceLanguage language) {
    }

    private record SubscriberSnapshot(Long requestId, Long chatId, Integer loadingMessageId,
                                      com.jackpotsaver.bot.domain.InterfaceLanguage language) {
    }
}
