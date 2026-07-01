package com.jackpotsaver.bot.telegram;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TelegramCommandInitializer {
    private static final Logger log = LoggerFactory.getLogger(TelegramCommandInitializer.class);
    private final TelegramApiClient apiClient;

    public TelegramCommandInitializer(TelegramApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeCommands() {
        if (!apiClient.configured()) {
            return;
        }
        try {
            apiClient.setMyCommands(List.of(
                    new TelegramApiClient.BotCommand("start", "Запустить бота"),
                    new TelegramApiClient.BotCommand("help", "Помощь"),
                    new TelegramApiClient.BotCommand("language", "Выбрать язык")
            ));
            log.info("Telegram bot commands have been registered");
        } catch (RuntimeException ex) {
            log.warn("Could not register Telegram bot commands: {}", ex.getMessage());
        }
    }
}
