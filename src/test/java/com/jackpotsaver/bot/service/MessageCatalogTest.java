package com.jackpotsaver.bot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jackpotsaver.bot.config.BotProperties;
import com.jackpotsaver.bot.domain.InterfaceLanguage;
import com.jackpotsaver.bot.domain.Platform;
import com.jackpotsaver.bot.domain.BotText;
import com.jackpotsaver.bot.repository.BotTextRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MessageCatalogTest {
    private final BotTextRepository botTextRepository = mock(BotTextRepository.class);
    private final MessageCatalog messages = new MessageCatalog(new BotProperties(
            "token", "JackpotSaverBot", new BotProperties.Polling(true, 25),
            new BotProperties.Network(10, 240, 3, 500)), botTextRepository);

    @Test
    void loadingMessageIsMinimal() {
        assertThat(messages.loading(InterfaceLanguage.RU)).isEqualTo("Загрузка...");
        assertThat(messages.loading(InterfaceLanguage.EN)).isEqualTo("Loading...");
    }

    @Test
    void captionsContainSourceOnly() {
        assertThat(messages.caption(Platform.YOUTUBE, InterfaceLanguage.RU))
                .isEqualTo("скачано с <a href=\"https://t.me/JackpotSaverBot\">JackpotSaverBot</a>");
        assertThat(messages.caption(Platform.TIKTOK, InterfaceLanguage.EN))
                .isEqualTo("скачано с <a href=\"https://t.me/JackpotSaverBot\">JackpotSaverBot</a>");
    }

    @Test
    void readsTextFromDatabaseWhenPresent() {
        BotText botText = mock(BotText.class);
        when(botText.getTextValue()).thenReturn("Database text");
        when(botTextRepository.findFirstByMessageKeyAndLanguage("loading", InterfaceLanguage.EN))
                .thenReturn(Optional.of(botText));

        assertThat(messages.loading(InterfaceLanguage.EN)).isEqualTo("Database text");
    }
}
