package com.jackpotsaver.bot.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.jackpotsaver.bot.domain.InterfaceLanguage;
import com.jackpotsaver.bot.domain.User;
import com.jackpotsaver.bot.domain.VideoQuality;
import com.jackpotsaver.bot.service.AdminConversationService;
import com.jackpotsaver.bot.service.AdminConversationState;
import com.jackpotsaver.bot.service.AdminService;
import com.jackpotsaver.bot.service.MessageCatalog;
import com.jackpotsaver.bot.service.TelegramUser;
import com.jackpotsaver.bot.service.UpdateOrchestrator;
import com.jackpotsaver.bot.service.UserService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TelegramUpdateHandler {
    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateHandler.class);
    private final UserService userService;
    private final MessageCatalog messages;
    private final TelegramApiClient apiClient;
    private final UpdateOrchestrator orchestrator;
    private final AdminService adminService;
    private final AdminConversationService adminConversationService;

    public TelegramUpdateHandler(UserService userService, MessageCatalog messages, TelegramApiClient apiClient,
                                 UpdateOrchestrator orchestrator, AdminService adminService,
                                 AdminConversationService adminConversationService) {
        this.userService = userService;
        this.messages = messages;
        this.apiClient = apiClient;
        this.orchestrator = orchestrator;
        this.adminService = adminService;
        this.adminConversationService = adminConversationService;
    }

    public boolean handle(JsonNode update) {
        try {
            if (update.has("callback_query")) {
                handleCallback(update.path("callback_query"));
            } else if (update.has("message")) {
                handleMessage(update.path("message"));
            }
            return true;
        } catch (RuntimeException ex) {
            log.warn("Failed to handle Telegram update {}", update, ex);
            return false;
        }
    }

    private void handleMessage(JsonNode message) {
        long chatId = message.path("chat").path("id").asLong();
        User user = userService.upsert(toUser(message.path("from")));
        String text = message.path("text").asText("");
        if (!user.admin() && (text.equals("/admin") || text.equals("Админ-панель"))) {
            apiClient.sendMessageRemovingKeyboard(chatId, messages.sendLink(user.getInterfaceLanguage()));
            return;
        }
        if (user.admin() && isCancelText(text) && adminConversationService.clear(user.getTelegramId())) {
            apiClient.sendMessage(chatId, "Действие отменено.", adminReplyKeyboard());
            return;
        }
        if (user.admin() && !text.startsWith("/") && handleAdminState(chatId, user, text)) {
            return;
        }
        if (text.equals("/start")) {
            apiClient.sendMessage(chatId, messages.start(user.getInterfaceLanguage()), startKeyboard(user.getInterfaceLanguage()));
            return;
        }
        if ((text.equals("/admin") || text.equals("Админ-панель")) && user.admin()) {
            apiClient.sendMessage(chatId, "Админ-панель открыта.", adminReplyKeyboard());
            return;
        }
        if (user.admin() && handleAdminButton(chatId, user, text)) {
            return;
        }
        if (text.equals("/help")) {
            apiClient.sendMessage(chatId, messages.help(user.getInterfaceLanguage()));
            return;
        }
        if (text.equals("/language")) {
            apiClient.sendMessage(chatId, "Выберите язык / Choose language:", languageKeyboard());
            return;
        }
        if (text.startsWith("/admin_stats") && user.admin()) {
            apiClient.sendMessage(chatId, adminService.stats());
            return;
        }
        if (text.startsWith("/admin_help") && user.admin()) {
            apiClient.sendMessage(chatId, adminService.help());
            return;
        }
        if (text.startsWith("/errors") && user.admin()) {
            apiClient.sendMessage(chatId, adminService.errors());
            return;
        }
        if (text.startsWith("/ad_after_off") && user.admin()) {
            apiClient.sendMessage(chatId, adminService.clearAfterDownloadAd(user));
            return;
        }
        if (text.startsWith("/ad_after") && user.admin()) {
            apiClient.sendMessage(chatId, adminService.setAfterDownloadAd(user, commandText(text)));
            return;
        }
        if (text.startsWith("/broadcast") && user.admin()) {
            apiClient.sendMessage(chatId, adminService.broadcast(user, commandText(text)));
            return;
        }
        if (text.startsWith("/block") && user.admin()) {
            long target = commandId(text);
            apiClient.sendMessage(chatId, adminService.block(user, target, true));
            return;
        }
        if (text.startsWith("/unblock") && user.admin()) {
            long target = commandId(text);
            apiClient.sendMessage(chatId, adminService.block(user, target, false));
            return;
        }
        if (text.startsWith("/platforms") && user.admin()) {
            apiClient.sendMessage(chatId, adminService.platforms(text.split("\\s+")));
            return;
        }
        orchestrator.handleText(chatId, user, text);
    }

    private void handleCallback(JsonNode callback) {
        String id = callback.path("id").asText();
        JsonNode message = callback.path("message");
        long chatId = message.path("chat").path("id").asLong();
        User user = userService.upsert(toUser(callback.path("from")));
        String data = callback.path("data").asText("");
        safeAnswerCallback(id);
        if (data.startsWith("admin:")) {
            handleAdminCallback(chatId, user, data);
            return;
        }
        if (data.equals("language:RU")) {
            userService.setLanguage(user, InterfaceLanguage.RU);
            apiClient.sendMessage(chatId, messages.languageChanged(InterfaceLanguage.RU));
            return;
        }
        if (data.equals("language:EN")) {
            userService.setLanguage(user, InterfaceLanguage.EN);
            apiClient.sendMessage(chatId, messages.languageChanged(InterfaceLanguage.EN));
            return;
        }
        if (data.equals("help")) {
            apiClient.sendMessage(chatId, messages.help(user.getInterfaceLanguage()));
            return;
        }
        if (data.equals("language")) {
            apiClient.sendMessage(chatId, "Выберите язык / Choose language:", languageKeyboard());
            return;
        }
        if (data.startsWith("quality:")) {
            JsonNode messageId = message.path("message_id");
            if (messageId.isNumber()) {
                safeDeleteMessage(chatId, messageId.asInt());
            }
            QualityCallback qualityCallback = parseQualityCallback(data);
            if (qualityCallback == null) {
                apiClient.sendMessage(chatId, messages.sendLink(user.getInterfaceLanguage()));
                return;
            }
            orchestrator.handleQuality(chatId, user, qualityCallback.requestId(), qualityCallback.quality());
        }
    }

    private boolean handleAdminState(long chatId, User user, String text) {
        return adminConversationService.take(user.getTelegramId())
                .map(state -> {
                    switch (state) {
                        case WAITING_AFTER_DOWNLOAD_AD ->
                                apiClient.sendMessage(chatId, adminService.setAfterDownloadAd(user, text), adminReplyKeyboard());
                        case WAITING_BROADCAST ->
                                apiClient.sendMessage(chatId, adminService.broadcast(user, text), adminReplyKeyboard());
                        case WAITING_ADD_ADMIN ->
                                apiClient.sendMessage(chatId, adminService.addAdmin(user, text), adminReplyKeyboard());
                        case WAITING_REMOVE_ADMIN ->
                                apiClient.sendMessage(chatId, adminService.removeAdmin(user, text), adminReplyKeyboard());
                    }
                    return true;
                })
                .orElse(false);
    }

    private boolean handleAdminButton(long chatId, User user, String text) {
        switch (text) {
            case "Статистика" -> apiClient.sendMessage(chatId, adminService.stats(), adminReplyKeyboard());
            case "Ошибки" -> apiClient.sendMessage(chatId, adminService.errors(), adminReplyKeyboard());
            case "Реклама после скачивания" -> {
                adminConversationService.set(user.getTelegramId(), AdminConversationState.WAITING_AFTER_DOWNLOAD_AD);
                apiClient.sendMessage(chatId, "Отправьте текст рекламы после скачивания.", adminWaitingReplyKeyboard());
            }
            case "Отключить рекламу" -> apiClient.sendMessage(chatId, adminService.clearAfterDownloadAd(user), adminReplyKeyboard());
            case "Рассылка" -> {
                adminConversationService.set(user.getTelegramId(), AdminConversationState.WAITING_BROADCAST);
                apiClient.sendMessage(chatId, "Отправьте текст рассылки всем пользователям.", adminWaitingReplyKeyboard());
            }
            case "Добавить админа" -> {
                adminConversationService.set(user.getTelegramId(), AdminConversationState.WAITING_ADD_ADMIN);
                apiClient.sendMessage(chatId, "Отправьте username пользователя, которого нужно сделать админом. Например: @username", adminWaitingReplyKeyboard());
            }
            case "Удалить админа" -> {
                adminConversationService.set(user.getTelegramId(), AdminConversationState.WAITING_REMOVE_ADMIN);
                apiClient.sendMessage(chatId, "Отправьте username админа, которого нужно удалить. Например: @username", adminWaitingReplyKeyboard());
            }
            case "Платформы" -> apiClient.sendMessage(chatId, "Платформы", platformReplyKeyboard());
            case "YouTube ON" -> apiClient.sendMessage(chatId, adminService.platforms(new String[]{"/platforms", "YOUTUBE", "on"}), platformReplyKeyboard());
            case "YouTube OFF" -> apiClient.sendMessage(chatId, adminService.platforms(new String[]{"/platforms", "YOUTUBE", "off"}), platformReplyKeyboard());
            case "Shorts ON" -> apiClient.sendMessage(chatId, adminService.platforms(new String[]{"/platforms", "YOUTUBE_SHORTS", "on"}), platformReplyKeyboard());
            case "Shorts OFF" -> apiClient.sendMessage(chatId, adminService.platforms(new String[]{"/platforms", "YOUTUBE_SHORTS", "off"}), platformReplyKeyboard());
            case "Instagram ON" -> apiClient.sendMessage(chatId, adminService.platforms(new String[]{"/platforms", "INSTAGRAM", "on"}), platformReplyKeyboard());
            case "Instagram OFF" -> apiClient.sendMessage(chatId, adminService.platforms(new String[]{"/platforms", "INSTAGRAM", "off"}), platformReplyKeyboard());
            case "TikTok ON" -> apiClient.sendMessage(chatId, adminService.platforms(new String[]{"/platforms", "TIKTOK", "on"}), platformReplyKeyboard());
            case "TikTok OFF" -> apiClient.sendMessage(chatId, adminService.platforms(new String[]{"/platforms", "TIKTOK", "off"}), platformReplyKeyboard());
            default -> {
                return false;
            }
        }
        return true;
    }

    private void handleAdminCallback(long chatId, User user, String data) {
        if (!user.admin()) {
            return;
        }
        switch (data) {
            case "admin:menu" -> apiClient.sendMessage(chatId, "Админ-панель открыта.", adminReplyKeyboard());
            case "admin:stats" -> apiClient.sendMessage(chatId, adminService.stats(), adminReplyKeyboard());
            case "admin:errors" -> apiClient.sendMessage(chatId, adminService.errors(), adminReplyKeyboard());
            case "admin:ad_after" -> {
                adminConversationService.set(user.getTelegramId(), AdminConversationState.WAITING_AFTER_DOWNLOAD_AD);
                apiClient.sendMessage(chatId, "Отправьте текст рекламы после скачивания.", adminWaitingReplyKeyboard());
            }
            case "admin:ad_after_off" -> apiClient.sendMessage(chatId, adminService.clearAfterDownloadAd(user), adminReplyKeyboard());
            case "admin:broadcast" -> {
                adminConversationService.set(user.getTelegramId(), AdminConversationState.WAITING_BROADCAST);
                apiClient.sendMessage(chatId, "Отправьте текст рассылки всем пользователям.", adminWaitingReplyKeyboard());
            }
            case "admin:add_admin" -> {
                adminConversationService.set(user.getTelegramId(), AdminConversationState.WAITING_ADD_ADMIN);
                apiClient.sendMessage(chatId, "Отправьте username пользователя, которого нужно сделать админом. Например: @username", adminWaitingReplyKeyboard());
            }
            case "admin:remove_admin" -> {
                adminConversationService.set(user.getTelegramId(), AdminConversationState.WAITING_REMOVE_ADMIN);
                apiClient.sendMessage(chatId, "Отправьте username админа, которого нужно удалить. Например: @username", adminWaitingReplyKeyboard());
            }
            case "admin:platforms" -> apiClient.sendMessage(chatId, "Платформы", platformKeyboard());
            case "admin:platform:YOUTUBE:on" -> apiClient.sendMessage(chatId, adminService.platforms(new String[]{"/platforms", "YOUTUBE", "on"}), platformKeyboard());
            case "admin:platform:YOUTUBE:off" -> apiClient.sendMessage(chatId, adminService.platforms(new String[]{"/platforms", "YOUTUBE", "off"}), platformKeyboard());
            case "admin:platform:YOUTUBE_SHORTS:on" -> apiClient.sendMessage(chatId, adminService.platforms(new String[]{"/platforms", "YOUTUBE_SHORTS", "on"}), platformKeyboard());
            case "admin:platform:YOUTUBE_SHORTS:off" -> apiClient.sendMessage(chatId, adminService.platforms(new String[]{"/platforms", "YOUTUBE_SHORTS", "off"}), platformKeyboard());
            case "admin:platform:INSTAGRAM:on" -> apiClient.sendMessage(chatId, adminService.platforms(new String[]{"/platforms", "INSTAGRAM", "on"}), platformKeyboard());
            case "admin:platform:INSTAGRAM:off" -> apiClient.sendMessage(chatId, adminService.platforms(new String[]{"/platforms", "INSTAGRAM", "off"}), platformKeyboard());
            case "admin:platform:TIKTOK:on" -> apiClient.sendMessage(chatId, adminService.platforms(new String[]{"/platforms", "TIKTOK", "on"}), platformKeyboard());
            case "admin:platform:TIKTOK:off" -> apiClient.sendMessage(chatId, adminService.platforms(new String[]{"/platforms", "TIKTOK", "off"}), platformKeyboard());
            default -> apiClient.sendMessage(chatId, "Неизвестное действие.", adminReplyKeyboard());
        }
    }

    private TelegramUser toUser(JsonNode from) {
        return new TelegramUser(
                from.path("id").asLong(),
                textOrNull(from, "username"),
                textOrNull(from, "first_name"),
                textOrNull(from, "last_name"),
                textOrNull(from, "language_code")
        );
    }

    private String textOrNull(JsonNode node, String name) {
        JsonNode value = node.path(name);
        return value.isMissingNode() ? null : value.asText();
    }

    private TelegramApiClient.InlineKeyboard startKeyboard(InterfaceLanguage language) {
        return new TelegramApiClient.InlineKeyboard(List.of(
                List.of(new TelegramApiClient.Button(language == InterfaceLanguage.EN ? "🌐 Choose language" : "🌐 Выбрать язык", "language")),
                List.of(new TelegramApiClient.Button(language == InterfaceLanguage.EN ? "❓ Help" : "❓ Помощь", "help"))
        ));
    }

    private TelegramApiClient.InlineKeyboard languageKeyboard() {
        return new TelegramApiClient.InlineKeyboard(List.of(
                List.of(new TelegramApiClient.Button("Русский", "language:RU")),
                List.of(new TelegramApiClient.Button("English", "language:EN"))
        ));
    }

    private TelegramApiClient.InlineKeyboard adminKeyboard() {
        return new TelegramApiClient.InlineKeyboard(List.of(
                List.of(new TelegramApiClient.Button("Статистика", "admin:stats"),
                        new TelegramApiClient.Button("Платформы", "admin:platforms")),
                List.of(new TelegramApiClient.Button("Реклама после скачивания", "admin:ad_after"),
                        new TelegramApiClient.Button("Отключить рекламу", "admin:ad_after_off")),
                List.of(new TelegramApiClient.Button("Рассылка", "admin:broadcast"),
                        new TelegramApiClient.Button("Ошибки", "admin:errors")),
                List.of(new TelegramApiClient.Button("Добавить админа", "admin:add_admin"),
                        new TelegramApiClient.Button("Удалить админа", "admin:remove_admin"))
        ));
    }

    private TelegramApiClient.ReplyKeyboard adminReplyKeyboard() {
        return new TelegramApiClient.ReplyKeyboard(List.of(
                List.of("Статистика", "Платформы"),
                List.of("Реклама после скачивания", "Отключить рекламу"),
                List.of("Рассылка", "Ошибки"),
                List.of("Добавить админа", "Удалить админа")
        ), true, false, true);
    }

    private TelegramApiClient.ReplyKeyboard adminWaitingReplyKeyboard() {
        return new TelegramApiClient.ReplyKeyboard(List.of(
                List.of("Отмена"),
                List.of("Статистика", "Платформы"),
                List.of("Реклама после скачивания", "Отключить рекламу"),
                List.of("Рассылка", "Ошибки"),
                List.of("Добавить админа", "Удалить админа")
        ), true, false, true);
    }

    private TelegramApiClient.ReplyKeyboard platformReplyKeyboard() {
        return new TelegramApiClient.ReplyKeyboard(List.of(
                List.of("YouTube ON", "YouTube OFF"),
                List.of("Shorts ON", "Shorts OFF"),
                List.of("Instagram ON", "Instagram OFF"),
                List.of("TikTok ON", "TikTok OFF"),
                List.of("Админ-панель")
        ), true, false, true);
    }

    private TelegramApiClient.InlineKeyboard platformKeyboard() {
        return new TelegramApiClient.InlineKeyboard(List.of(
                List.of(new TelegramApiClient.Button("YouTube ON", "admin:platform:YOUTUBE:on"),
                        new TelegramApiClient.Button("YouTube OFF", "admin:platform:YOUTUBE:off")),
                List.of(new TelegramApiClient.Button("Shorts ON", "admin:platform:YOUTUBE_SHORTS:on"),
                        new TelegramApiClient.Button("Shorts OFF", "admin:platform:YOUTUBE_SHORTS:off")),
                List.of(new TelegramApiClient.Button("Instagram ON", "admin:platform:INSTAGRAM:on"),
                        new TelegramApiClient.Button("Instagram OFF", "admin:platform:INSTAGRAM:off")),
                List.of(new TelegramApiClient.Button("TikTok ON", "admin:platform:TIKTOK:on"),
                        new TelegramApiClient.Button("TikTok OFF", "admin:platform:TIKTOK:off")),
                List.of(new TelegramApiClient.Button("Назад", "admin:menu"))
        ));
    }

    private long commandId(String text) {
        String[] parts = text.split("\\s+");
        if (parts.length < 2) {
            return 0;
        }
        return Long.parseLong(parts[1]);
    }

    private String commandText(String text) {
        int space = text.indexOf(' ');
        return space < 0 ? "" : text.substring(space + 1);
    }

    private boolean isCancelText(String text) {
        return text.equalsIgnoreCase("/cancel") || text.equalsIgnoreCase("cancel") || text.equalsIgnoreCase("отмена");
    }

    private long parseLong(String text) {
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void safeAnswerCallback(String id) {
        try {
            apiClient.answerCallback(id);
        } catch (RuntimeException ex) {
            log.warn("Could not answer callback query {}: {}", id, ex.getMessage());
        }
    }

    private void safeDeleteMessage(long chatId, int messageId) {
        try {
            apiClient.deleteMessage(chatId, messageId);
        } catch (RuntimeException ex) {
            log.warn("Could not delete Telegram message {}", messageId, ex);
        }
    }

    private QualityCallback parseQualityCallback(String data) {
        String[] parts = data.split(":");
        try {
            if (parts.length == 2) {
                return new QualityCallback(null, VideoQuality.valueOf(parts[1]));
            }
            if (parts.length == 3) {
                return new QualityCallback(Long.parseLong(parts[1]), VideoQuality.valueOf(parts[2]));
            }
            return null;
        } catch (RuntimeException ex) {
            log.warn("Invalid quality callback data: {}", data, ex);
            return null;
        }
    }

    private record QualityCallback(Long requestId, VideoQuality quality) {
    }
}
