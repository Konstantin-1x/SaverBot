package com.jackpotsaver.bot.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.jackpotsaver.bot.config.BotProperties;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "bot.polling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TelegramPollingService {
    private static final Logger log = LoggerFactory.getLogger(TelegramPollingService.class);
    private final TelegramApiClient apiClient;
    private final TelegramUpdateHandler handler;
    private final BotProperties properties;
    private final TelegramUpdateTracker updateTracker;
    private final TelegramConnectivityState connectivityState;
    private final ExecutorService[] updateExecutors = new ExecutorService[16];
    private long offset = 0;

    public TelegramPollingService(TelegramApiClient apiClient, TelegramUpdateHandler handler, BotProperties properties,
                                  TelegramUpdateTracker updateTracker, TelegramConnectivityState connectivityState) {
        this.apiClient = apiClient;
        this.handler = handler;
        this.properties = properties;
        this.updateTracker = updateTracker;
        this.connectivityState = connectivityState;
        for (int i = 0; i < updateExecutors.length; i++) {
            updateExecutors[i] = new ThreadPoolExecutor(
                    1, 1, 0L, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(100),
                    new ThreadPoolExecutor.CallerRunsPolicy());
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        if (!apiClient.configured()) {
            log.info("BOT_TOKEN is empty, Telegram polling is disabled until token is configured.");
            return;
        }
        try {
            JsonNode response = apiClient.getUpdates(offset, properties.polling().timeoutSeconds());
            if (response == null || !response.path("ok").asBoolean(false)) {
                connectivityState.failure();
                return;
            }
            connectivityState.success();
            List<PendingUpdate> pending = new ArrayList<>();
            for (JsonNode update : response.path("result")) {
                long updateId = update.path("update_id").asLong();
                boolean claimed = updateTracker.claim(updateId);
                if (!claimed) {
                    pending.add(new PendingUpdate(updateId, null));
                    continue;
                }
                Future<Boolean> future = executorFor(update).submit(() -> {
                    try {
                        if (handler.handle(update)) {
                            updateTracker.complete(updateId);
                            return true;
                        } else {
                            updateTracker.fail(updateId, "Update handler failed");
                            return false;
                        }
                    } catch (RuntimeException ex) {
                        updateTracker.fail(updateId, ex.getMessage());
                        log.warn("Failed to process claimed Telegram update {}", updateId, ex);
                        return false;
                    }
                });
                pending.add(new PendingUpdate(updateId, future));
            }
            for (PendingUpdate item : pending) {
                boolean completed = item.future() == null || item.future().get();
                if (!completed) {
                    break;
                }
                offset = item.updateId() + 1;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            connectivityState.failure();
            log.warn("Telegram polling interrupted");
        } catch (Exception ex) {
            connectivityState.failure();
            log.warn("Telegram polling failed; will retry on the next scheduled tick: {}", ex.getMessage());
        }
    }

    private ExecutorService executorFor(JsonNode update) {
        long key = update.path("message").path("chat").path("id").asLong(
                update.path("callback_query").path("from").path("id").asLong(update.path("update_id").asLong()));
        return updateExecutors[Math.floorMod(Long.hashCode(key), updateExecutors.length)];
    }

    private record PendingUpdate(long updateId, Future<Boolean> future) {
    }

    @PreDestroy
    public void shutdown() {
        for (ExecutorService executor : updateExecutors) {
            executor.shutdown();
        }
        try {
            for (ExecutorService executor : updateExecutors) {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("Telegram update executor did not stop within 30 seconds; forcing shutdown");
                    executor.shutdownNow();
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            for (ExecutorService executor : updateExecutors) {
                executor.shutdownNow();
            }
        }
    }
}
