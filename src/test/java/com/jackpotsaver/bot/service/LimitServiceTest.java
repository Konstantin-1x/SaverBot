package com.jackpotsaver.bot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jackpotsaver.bot.config.LimitProperties;
import com.jackpotsaver.bot.domain.User;
import com.jackpotsaver.bot.repository.DownloadRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LimitServiceTest {
    private final DownloadRequestRepository repository = mock(DownloadRequestRepository.class);
    private final LimitService limitService = new LimitService(
            repository,
            new LimitProperties(10, 20),
            Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void adminsBypassCooldownAndDailyLimit() {
        User admin = new User(123456789L, "testadmin", "First", "Last", "ru",
                Instant.parse("2026-06-15T00:00:00Z"), true);

        assertThat(limitService.allowed(admin)).isTrue();
        verify(repository, never()).existsByUserIdAndCreatedAtAfter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void regularUsersKeepTenSecondCooldown() {
        User user = new User(2L, "user", "First", "Last", "ru",
                Instant.parse("2026-06-15T00:00:00Z"), false);
        when(repository.existsByUserIdAndCreatedAtAfter(user.getId(), Instant.parse("2026-06-14T23:59:50Z")))
                .thenReturn(true);

        assertThat(limitService.allowed(user)).isFalse();
    }
}
