package com.jackpotsaver.bot.domain;

public enum RequestStatus {
    CREATED,
    WAITING_QUALITY,
    LOADING,
    SENDING,
    SUCCESS,
    FAILED,
    CACHED,
    EXPIRED,
    DELETED
}
