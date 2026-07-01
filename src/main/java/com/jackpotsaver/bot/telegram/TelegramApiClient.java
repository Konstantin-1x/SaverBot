package com.jackpotsaver.bot.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jackpotsaver.bot.config.BotProperties;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.util.concurrent.TimeUnit;

@Component
public class TelegramApiClient {
    private static final Logger log = LoggerFactory.getLogger(TelegramApiClient.class);
    private final BotProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public TelegramApiClient(BotProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        BotProperties.Network network = properties.network();
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, network.connectTimeoutSeconds() * 1000)
                .responseTimeout(Duration.ofSeconds(network.responseTimeoutSeconds()))
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(network.responseTimeoutSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(network.responseTimeoutSeconds(), TimeUnit.SECONDS)));
        this.webClient = WebClient.builder()
                .baseUrl("https://api.telegram.org/bot" + properties.token())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public JsonNode getUpdates(long offset, int timeoutSeconds) {
        return withRetry("getUpdates", () -> webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/getUpdates")
                        .queryParam("offset", offset)
                        .queryParam("timeout", timeoutSeconds)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block());
    }

    public Integer sendMessage(long chatId, String text) {
        JsonNode response = execute("sendMessage", false, () -> webClient.post()
                .uri("/sendMessage")
                .bodyValue(Map.of("chat_id", chatId, "text", text))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block());
        return messageId(response);
    }

    public Integer sendMessage(long chatId, String text, InlineKeyboard keyboard) {
        JsonNode response = execute("sendMessage", false, () -> webClient.post()
                .uri("/sendMessage")
                .bodyValue(Map.of("chat_id", chatId, "text", text, "reply_markup", keyboard.toTelegramJson()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block());
        return messageId(response);
    }

    public Integer sendMessage(long chatId, String text, ReplyKeyboard keyboard) {
        JsonNode response = execute("sendMessage", false, () -> webClient.post()
                .uri("/sendMessage")
                .bodyValue(Map.of("chat_id", chatId, "text", text, "reply_markup", keyboard.toTelegramJson()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block());
        return messageId(response);
    }

    public Integer sendMessageRemovingKeyboard(long chatId, String text) {
        JsonNode response = execute("sendMessage", false, () -> webClient.post()
                .uri("/sendMessage")
                .bodyValue(Map.of("chat_id", chatId, "text", text, "reply_markup", new ReplyKeyboardRemove(true).toTelegramJson()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block());
        return messageId(response);
    }

    public void setMyCommands(List<BotCommand> commands) {
        withRetry("setMyCommands", () -> webClient.post()
                .uri("/setMyCommands")
                .bodyValue(Map.of("commands", commands))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block());
    }

    public void answerCallback(String callbackQueryId) {
        withRetry("answerCallbackQuery", () -> webClient.post()
                .uri("/answerCallbackQuery")
                .bodyValue(Map.of("callback_query_id", callbackQueryId))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block());
    }

    public void deleteMessage(long chatId, int messageId) {
        webClient.post()
                .uri("/deleteMessage")
                .bodyValue(Map.of("chat_id", chatId, "message_id", messageId))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorComplete()
                .block();
    }

    public JsonNode sendVideo(long chatId, Path file, String caption) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("chat_id", String.valueOf(chatId));
        bodyBuilder.part("caption", caption);
        bodyBuilder.part("parse_mode", "HTML");
        bodyBuilder.part("video", new FileSystemResource(file));
        return execute("sendVideo", false, () -> webClient.post()
                .uri("/sendVideo")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block());
    }

    public JsonNode sendVideo(long chatId, String telegramFileId, String caption) {
        return execute("sendVideo", false, () -> webClient.post()
                .uri("/sendVideo")
                .bodyValue(Map.of(
                        "chat_id", chatId,
                        "video", telegramFileId,
                        "caption", caption,
                        "parse_mode", "HTML"))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block());
    }

    public boolean configured() {
        return properties.configured();
    }

    private Integer messageId(JsonNode response) {
        if (response == null || !response.path("ok").asBoolean(false)) {
            return null;
        }
        JsonNode messageId = response.path("result").path("message_id");
        return messageId.isNumber() ? messageId.asInt() : null;
    }

    private JsonNode withRetry(String operation, Supplier<JsonNode> request) {
        return execute(operation, true, request);
    }

    private JsonNode execute(String operation, boolean retryTransient, Supplier<JsonNode> request) {
        TelegramApiException last = null;
        int maxAttempts = Math.max(1, properties.network().maxRetries());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return request.get();
            } catch (RuntimeException ex) {
                last = classify(ex);
                boolean mayRetry = last.kind() == TelegramApiException.Kind.RATE_LIMIT
                        || (retryTransient && last.transientFailure());
                if (!mayRetry || attempt == maxAttempts) {
                    break;
                }
                sleepBeforeRetry(operation, attempt, last);
            }
        }
        throw last;
    }

    TelegramApiException classify(RuntimeException exception) {
        if (exception instanceof TelegramApiException telegramException) {
            return telegramException;
        }
        if (exception instanceof WebClientResponseException response) {
            int status = response.getStatusCode().value();
            if (status == 429) {
                return new TelegramApiException(TelegramApiException.Kind.RATE_LIMIT,
                        "Telegram rate limit", retryAfterMillis(response), response);
            }
            if (status >= 500) {
                return new TelegramApiException(TelegramApiException.Kind.SERVER,
                        "Telegram server error " + status, null, response);
            }
            return new TelegramApiException(TelegramApiException.Kind.CLIENT,
                    "Telegram client error " + status, null, response);
        }
        if (exception.getCause() instanceof java.util.concurrent.TimeoutException
                || exception instanceof org.springframework.web.reactive.function.client.WebClientRequestException) {
            return new TelegramApiException(TelegramApiException.Kind.NETWORK,
                    "Telegram network failure", null, exception);
        }
        return new TelegramApiException(TelegramApiException.Kind.UNKNOWN,
                "Unexpected Telegram failure", null, exception);
    }

    private Long retryAfterMillis(WebClientResponseException response) {
        try {
            long seconds = objectMapper.readTree(response.getResponseBodyAsString())
                    .path("parameters").path("retry_after").asLong(0);
            return seconds > 0 ? seconds * 1000 : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void sleepBeforeRetry(String operation, int attempt, TelegramApiException ex) {
        long exponential = properties.network().retryBaseDelayMillis() * (1L << Math.min(attempt - 1, 10));
        long delayMillis = ex.retryAfterMillis() == null ? exponential : Math.max(exponential, ex.retryAfterMillis());
        log.warn("Telegram {} failed kind={} attempt={}; retrying in {} ms",
                operation, ex.kind(), attempt, delayMillis);
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted before retrying Telegram " + operation, interrupted);
        }
    }

    public record InlineKeyboard(List<List<Button>> inline_keyboard) {
        public Map<String, Object> toTelegramJson() {
            return new ObjectMapper().convertValue(this, Map.class);
        }
    }

    public record Button(String text, String callback_data) {
    }

    public record BotCommand(String command, String description) {
    }

    public record ReplyKeyboard(List<List<String>> keyboard, boolean resize_keyboard, boolean one_time_keyboard, boolean is_persistent) {
        public Map<String, Object> toTelegramJson() {
            return new ObjectMapper().convertValue(this, Map.class);
        }
    }

    public record ReplyKeyboardRemove(boolean remove_keyboard) {
        public Map<String, Object> toTelegramJson() {
            return new ObjectMapper().convertValue(this, Map.class);
        }
    }
}
