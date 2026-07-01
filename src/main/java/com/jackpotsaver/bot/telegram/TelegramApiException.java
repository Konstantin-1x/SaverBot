package com.jackpotsaver.bot.telegram;

public class TelegramApiException extends RuntimeException {
    public enum Kind {
        RATE_LIMIT,
        NETWORK,
        SERVER,
        CLIENT,
        UNKNOWN
    }

    private final Kind kind;
    private final Long retryAfterMillis;

    public TelegramApiException(Kind kind, String message, Long retryAfterMillis, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.retryAfterMillis = retryAfterMillis;
    }

    public Kind kind() {
        return kind;
    }

    public Long retryAfterMillis() {
        return retryAfterMillis;
    }

    public boolean transientFailure() {
        return kind == Kind.RATE_LIMIT || kind == Kind.NETWORK || kind == Kind.SERVER;
    }
}
