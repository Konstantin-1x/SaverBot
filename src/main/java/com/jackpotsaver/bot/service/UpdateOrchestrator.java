package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.domain.DownloadJob;
import com.jackpotsaver.bot.domain.DownloadRequest;
import com.jackpotsaver.bot.domain.InterfaceLanguage;
import com.jackpotsaver.bot.domain.Platform;
import com.jackpotsaver.bot.domain.RequestStatus;
import com.jackpotsaver.bot.domain.StoredFile;
import com.jackpotsaver.bot.domain.User;
import com.jackpotsaver.bot.domain.VideoQuality;
import com.jackpotsaver.bot.repository.DownloadJobRepository;
import com.jackpotsaver.bot.repository.DownloadJobSubscriberRepository;
import com.jackpotsaver.bot.domain.DownloadJobSubscriber;
import com.jackpotsaver.bot.repository.DownloadRequestRepository;
import com.jackpotsaver.bot.telegram.TelegramApiClient;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import com.jackpotsaver.bot.domain.JobStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class UpdateOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(UpdateOrchestrator.class);
    private final PlatformDetector platformDetector;
    private final MessageCatalog messages;
    private final LimitService limitService;
    private final PlatformService platformService;
    private final CacheService cacheService;
    private final DownloadRequestRepository requestRepository;
    private final DownloadJobRepository jobRepository;
    private final DownloadJobSubscriberRepository subscriberRepository;
    private final TelegramApiClient telegramApiClient;
    private final AdService adService;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ResourceKeyService resourceKeyService;
    private final TelegramContentSender contentSender;

    public UpdateOrchestrator(PlatformDetector platformDetector, MessageCatalog messages, LimitService limitService,
                              PlatformService platformService, CacheService cacheService,
                              DownloadRequestRepository requestRepository, DownloadJobRepository jobRepository,
                              DownloadJobSubscriberRepository subscriberRepository,
                              TelegramApiClient telegramApiClient, AdService adService, Clock clock,
                              TransactionTemplate transactionTemplate, JdbcTemplate jdbcTemplate,
                              ResourceKeyService resourceKeyService, TelegramContentSender contentSender) {
        this.platformDetector = platformDetector;
        this.messages = messages;
        this.limitService = limitService;
        this.platformService = platformService;
        this.cacheService = cacheService;
        this.requestRepository = requestRepository;
        this.jobRepository = jobRepository;
        this.subscriberRepository = subscriberRepository;
        this.telegramApiClient = telegramApiClient;
        this.adService = adService;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.resourceKeyService = resourceKeyService;
        this.contentSender = contentSender;
    }

    public void handleText(long chatId, User user, String text) {
        if (user.isBlocked()) {
            telegramApiClient.sendMessage(chatId, messages.blocked(user.getInterfaceLanguage()));
            return;
        }
        var parsed = platformDetector.parse(text);
        if (parsed.isEmpty()) {
            telegramApiClient.sendMessage(chatId, platformDetector.isUrl(text)
                    ? messages.unsupported(user.getInterfaceLanguage())
                    : messages.sendLink(user.getInterfaceLanguage()));
            return;
        }
        VideoLink link = parsed.get();
        if (!platformService.enabled(link.platform())) {
            telegramApiClient.sendMessage(chatId, messages.platformDisabled(user.getInterfaceLanguage()));
            return;
        }
        if (!limitService.allowed(user)) {
            telegramApiClient.sendMessage(chatId, messages.limit(user.getInterfaceLanguage()));
            return;
        }
        RequestAction action = createRequestAction(user, link, VideoQuality.AUTO);
        completeAction(chatId, action);
    }

    public void handleQuality(long chatId, User user, Long requestId, VideoQuality quality) {
        RequestAction action = findPendingQualityAction(user, requestId, quality);
        if (action == null) {
            telegramApiClient.sendMessage(chatId, messages.sendLink(user.getInterfaceLanguage()));
            return;
        }
        completeAction(chatId, action);
    }

    private RequestAction createRequestAction(User user, VideoLink link, VideoQuality quality) {
        return transactionTemplate.execute(status -> {
            DownloadRequest request = new DownloadRequest(user, link.sourceUrl(), link.normalizedUrl(), link.platform(),
                    link.mediaType(), null, link.qualityRequired() ? RequestStatus.WAITING_QUALITY : RequestStatus.CREATED,
                    clock.instant());
            requestRepository.save(request);
            log.info("Received {} link from user {}", link.mediaType(), user.getId());
            return actionForRequest(user.getInterfaceLanguage(), request, quality);
        });
    }

    private RequestAction findPendingQualityAction(User user, Long requestId, VideoQuality quality) {
        return transactionTemplate.execute(status -> {
            DownloadRequest request = requestId == null
                    ? requestRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), RequestStatus.WAITING_QUALITY).orElse(null)
                    : requestRepository.findByIdAndUserId(requestId, user.getId()).orElse(null);
            if (request == null || request.getStatus() != RequestStatus.WAITING_QUALITY) {
                return null;
            }
            return actionForRequest(user.getInterfaceLanguage(), request, quality);
        });
    }

    private RequestAction actionForRequest(InterfaceLanguage language, DownloadRequest request, VideoQuality quality) {
        request.setSelectedQuality(quality, clock.instant());
        String resourceKey = resourceKeyService.create(request.getNormalizedUrl(), request.getMediaType(), quality);
        request.setResourceKey(resourceKey, clock.instant());
        return cacheService.findReusable(resourceKey)
                .map(file -> cachedAction(request, file, language))
                .orElseGet(() -> RequestAction.queue(request.getId(), language));
    }

    private RequestAction cachedAction(DownloadRequest request, StoredFile file, InterfaceLanguage language) {
        return RequestAction.cached(
                request.getId(),
                request.getUser().getId(),
                language,
                file.getId(),
                file.getFilePath(),
                file.getTelegramFileId(),
                file.getPlatform()
        );
    }

    private void completeAction(long chatId, RequestAction action) {
        try {
            if (action.type() == ActionType.CACHED) {
                sendCached(chatId, action);
                markRequestCached(action.requestId());
            } else {
                Integer loadingId = telegramApiClient.sendMessage(chatId, messages.loading(action.language()));
                createJob(chatId, action, loadingId);
            }
        } catch (RuntimeException ex) {
            failRequest(action.requestId(), "TELEGRAM_SEND_FAILED");
            throw ex;
        }
    }

    private void sendCached(long chatId, RequestAction action) {
        Integer loadingId = telegramApiClient.sendMessage(chatId, messages.loading(action.language()));
        String caption = messages.caption(action.platform(), action.language());
        String telegramFileId = action.telegramFileId();
        if (telegramFileId == null || telegramFileId.isBlank()) {
            telegramApiClient.sendVideo(chatId, Path.of(action.filePath()), caption);
        } else {
            telegramApiClient.sendVideo(chatId, telegramFileId, caption);
        }
        if (loadingId != null) {
            telegramApiClient.deleteMessage(chatId, loadingId);
        }
        sendAfterDownloadAd(chatId, action.userId());
        log.info("Sent cached file {} for request {}", action.storedFileId(), action.requestId());
    }

    private void createJob(long chatId, RequestAction action, Integer loadingId) {
        transactionTemplate.executeWithoutResult(status -> {
            DownloadRequest request = requestRepository.findById(action.requestId()).orElseThrow();
            request.attachTelegramLoading(chatId, loadingId, clock.instant());
            request.status(RequestStatus.LOADING, clock.instant());
            String dedupKey = request.getResourceKey();
            jdbcTemplate.queryForObject(
                    "select 1 from (select pg_advisory_xact_lock(hashtext(?))) locked", Integer.class, dedupKey);
            var activeJob = jobRepository.findFirstByDedupKeyAndStatusInOrderByIdAsc(
                    dedupKey, List.of(JobStatus.CREATED, JobStatus.RUNNING));
            if (activeJob.isPresent()) {
                subscriberRepository.save(new DownloadJobSubscriber(activeJob.get(), request, clock.instant()));
                log.info("Subscribed request {} to active download job {}", request.getId(), activeJob.get().getId());
            } else {
                jobRepository.save(new DownloadJob(request, dedupKey));
                log.info("Created download job for request {}", request.getId());
            }
        });
    }

    private void markRequestCached(Long requestId) {
        transactionTemplate.executeWithoutResult(status -> {
            DownloadRequest request = requestRepository.findById(requestId).orElseThrow();
            request.status(RequestStatus.CACHED, clock.instant());
            request.redactUrls();
        });
    }

    private void failRequest(Long requestId, String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> requestRepository.findById(requestId)
                .ifPresent(request -> {
                    request.fail(errorMessage, clock.instant());
                    request.redactUrls();
                }));
    }

    private void sendAfterDownloadAd(long chatId, Long userId) {
        adService.contentForCompletedDownload(userId).ifPresent(content -> {
            try {
                contentSender.send(chatId, content);
            } catch (RuntimeException ex) {
                log.warn("Could not send after-download ad", ex);
            }
        });
    }

    private enum ActionType {
        CACHED,
        QUEUE
    }

    private record RequestAction(ActionType type, Long requestId, Long userId,
                                 InterfaceLanguage language, Long storedFileId,
                                 String filePath, String telegramFileId, Platform platform) {
        private static RequestAction cached(Long requestId, Long userId,
                                            InterfaceLanguage language, Long storedFileId,
                                            String filePath, String telegramFileId, Platform platform) {
            return new RequestAction(
                    ActionType.CACHED, requestId, userId, language,
                    storedFileId, filePath, telegramFileId, platform);
        }

        private static RequestAction queue(Long requestId, InterfaceLanguage language) {
            return new RequestAction(ActionType.QUEUE, requestId, null, language, null, null, null, null);
        }
    }
}
