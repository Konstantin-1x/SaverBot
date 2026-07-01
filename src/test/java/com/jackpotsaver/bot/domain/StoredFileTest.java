package com.jackpotsaver.bot.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class StoredFileTest {
    @Test
    void fileIsNotReusableUntilExplicitlyPublished() {
        Instant now = Instant.parse("2026-06-30T00:00:00Z");
        StoredFile file = new StoredFile(
                mock(DownloadRequest.class), "storage/video.mp4", 100,
                now, now.plusSeconds(3600));

        assertThat(file.getStatus()).isEqualTo(FileStatus.STAGING);

        file.markAvailable();

        assertThat(file.getStatus()).isEqualTo(FileStatus.AVAILABLE);
    }
}
