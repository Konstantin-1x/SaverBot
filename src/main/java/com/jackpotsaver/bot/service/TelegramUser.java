package com.jackpotsaver.bot.service;

public record TelegramUser(long telegramId, String username, String firstName, String lastName, String languageCode) {
}
