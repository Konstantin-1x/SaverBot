package com.jackpotsaver.bot.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jackpotsaver.bot.config.BotProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

class TelegramApiClientTest {
    @Test
    void replyKeyboardIsPersistent() {
        TelegramApiClient.ReplyKeyboard keyboard = new TelegramApiClient.ReplyKeyboard(
                List.of(List.of("Статистика", "Платформы")),
                true,
                false,
                true
        );

        Map<String, Object> json = keyboard.toTelegramJson();

        assertThat(json)
                .containsEntry("resize_keyboard", true)
                .containsEntry("one_time_keyboard", false)
                .containsEntry("is_persistent", true);
        assertThat(json.get("keyboard")).isEqualTo(List.of(List.of("Статистика", "Платформы")));
    }

    @Test
    void botCommandCarriesCommandAndDescription() {
        TelegramApiClient.BotCommand command = new TelegramApiClient.BotCommand("start", "Запустить бота");

        assertThat(command.command()).isEqualTo("start");
        assertThat(command.description()).isEqualTo("Запустить бота");
    }

    @Test
    void removeKeyboardMarkupIsSupported() {
        TelegramApiClient.ReplyKeyboardRemove remove = new TelegramApiClient.ReplyKeyboardRemove(true);

        assertThat(remove.toTelegramJson()).containsEntry("remove_keyboard", true);
    }

    @Test
    void extractsTelegramRetryAfterFromRateLimitResponse() {
        TelegramApiClient client = client();
        WebClientResponseException response = WebClientResponseException.create(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                HttpHeaders.EMPTY,
                "{\"parameters\":{\"retry_after\":7}}".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.charset.StandardCharsets.UTF_8);

        TelegramApiException classified = client.classify(response);

        assertThat(classified.kind()).isEqualTo(TelegramApiException.Kind.RATE_LIMIT);
        assertThat(classified.retryAfterMillis()).isEqualTo(7000);
    }

    @Test
    void classifiesServerErrorsAsTransient() {
        TelegramApiException classified = client().classify(WebClientResponseException.create(
                503, "Unavailable", HttpHeaders.EMPTY, new byte[0], java.nio.charset.StandardCharsets.UTF_8));

        assertThat(classified.kind()).isEqualTo(TelegramApiException.Kind.SERVER);
        assertThat(classified.transientFailure()).isTrue();
    }

    private TelegramApiClient client() {
        return new TelegramApiClient(
                new BotProperties("token", "bot", new BotProperties.Polling(true, 25),
                        new BotProperties.Network(1, 30, 3, 10)),
                new ObjectMapper());
    }
}
