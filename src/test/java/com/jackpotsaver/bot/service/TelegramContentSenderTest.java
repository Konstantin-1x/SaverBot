package com.jackpotsaver.bot.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.jackpotsaver.bot.domain.AdMediaType;
import com.jackpotsaver.bot.telegram.TelegramApiClient;
import org.junit.jupiter.api.Test;

class TelegramContentSenderTest {
    private final TelegramApiClient api = mock(TelegramApiClient.class);
    private final TelegramContentSender sender = new TelegramContentSender(api);

    @Test
    void sendsPhotoWithCaptionByReusableFileId() {
        sender.send(42L, new MediaContent(AdMediaType.PHOTO, "photo-id", "caption"));

        verify(api).sendPhoto(42L, "photo-id", "caption");
    }

    @Test
    void sendsVideoWithCaptionByReusableFileId() {
        sender.send(42L, new MediaContent(AdMediaType.VIDEO, "video-id", "caption"));

        verify(api).sendMediaVideo(42L, "video-id", "caption");
    }
}
