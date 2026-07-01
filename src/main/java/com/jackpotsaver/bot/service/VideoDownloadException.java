package com.jackpotsaver.bot.service;

public class VideoDownloadException extends RuntimeException {
    public VideoDownloadException(String message) {
        super(message);
    }

    public VideoDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
