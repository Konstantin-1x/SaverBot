package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.domain.AdMediaType;

public record MediaContent(AdMediaType mediaType, String telegramFileId, String text) {
    public boolean empty() {
        return (telegramFileId == null || telegramFileId.isBlank())
                && (text == null || text.isBlank());
    }

    public boolean validLength() {
        int length = text == null ? 0 : text.length();
        return mediaType == null ? length <= 4096 : length <= 1024;
    }
}
