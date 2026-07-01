package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.domain.DownloadRequest;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class PendingQualityService {
    private final Map<Long, Long> pendingRequestIds = new ConcurrentHashMap<>();

    public void remember(long chatId, DownloadRequest request) {
        pendingRequestIds.put(chatId, request.getId());
    }

    public Optional<Long> take(long chatId) {
        return Optional.ofNullable(pendingRequestIds.remove(chatId));
    }
}
