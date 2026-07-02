package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.domain.AdMediaType;
import com.jackpotsaver.bot.telegram.TelegramApiClient;
import org.springframework.stereotype.Service;

@Service
public class TelegramContentSender {
    private final TelegramApiClient apiClient;

    public TelegramContentSender(TelegramApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void send(long chatId, MediaContent content) {
        String text = content.text() == null ? "" : content.text();
        if (content.mediaType() == AdMediaType.PHOTO) {
            apiClient.sendPhoto(chatId, content.telegramFileId(), text);
        } else if (content.mediaType() == AdMediaType.VIDEO) {
            apiClient.sendMediaVideo(chatId, content.telegramFileId(), text);
        } else if (!text.isBlank()) {
            apiClient.sendMessage(chatId, text);
        }
    }
}
