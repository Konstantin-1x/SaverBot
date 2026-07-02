package com.jackpotsaver.bot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jackpotsaver.bot.domain.AdMediaType;
import com.jackpotsaver.bot.domain.AdSettings;
import com.jackpotsaver.bot.repository.AdSettingsRepository;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class AdServiceTest {
    private final AdSettingsRepository repository = mock(AdSettingsRepository.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final AdService service = new AdService(repository, Clock.systemUTC(), jdbc);

    @Test
    void returnsMediaAdOnEveryNthCompletedDownload() {
        AdSettings settings = mock(AdSettings.class);
        when(settings.getFrequency()).thenReturn(3);
        when(settings.getMediaType()).thenReturn(AdMediaType.PHOTO);
        when(settings.getTelegramFileId()).thenReturn("photo-file-id");
        when(settings.getAfterDownloadText()).thenReturn("caption");
        when(repository.findById(1L)).thenReturn(Optional.of(settings));
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(7L))).thenReturn(6L);

        assertThat(service.contentForCompletedDownload(7L))
                .contains(new MediaContent(AdMediaType.PHOTO, "photo-file-id", "caption"));
    }

    @Test
    void skipsAdBetweenConfiguredIntervals() {
        AdSettings settings = mock(AdSettings.class);
        when(settings.getFrequency()).thenReturn(3);
        when(repository.findById(1L)).thenReturn(Optional.of(settings));
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(7L))).thenReturn(5L);

        assertThat(service.contentForCompletedDownload(7L)).isEmpty();
    }
}

