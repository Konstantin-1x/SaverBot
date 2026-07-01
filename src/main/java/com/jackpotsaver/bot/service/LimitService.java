package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.config.LimitProperties;
import com.jackpotsaver.bot.domain.User;
import com.jackpotsaver.bot.repository.DownloadRequestRepository;
import java.time.Clock;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class LimitService {
    private final DownloadRequestRepository requestRepository;
    private final LimitProperties properties;
    private final Clock clock;

    public LimitService(DownloadRequestRepository requestRepository, LimitProperties properties, Clock clock) {
        this.requestRepository = requestRepository;
        this.properties = properties;
        this.clock = clock;
    }

    public boolean allowed(User user) {
        if (user.admin()) {
            return true;
        }
        if (requestRepository.existsByUserIdAndCreatedAtAfter(user.getId(),
                clock.instant().minus(Duration.ofSeconds(properties.requestCooldownSeconds())))) {
            return false;
        }
        return requestRepository.countByUserIdAndCreatedAtAfter(user.getId(), clock.instant().minus(Duration.ofDays(1)))
                < properties.downloadsPerDay();
    }
}
