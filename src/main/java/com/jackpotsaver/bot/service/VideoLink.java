package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.domain.MediaType;
import com.jackpotsaver.bot.domain.Platform;

public record VideoLink(String sourceUrl, String normalizedUrl, Platform platform, MediaType mediaType) {
    public boolean qualityRequired() {
        return mediaType == MediaType.YOUTUBE_VIDEO;
    }
}
