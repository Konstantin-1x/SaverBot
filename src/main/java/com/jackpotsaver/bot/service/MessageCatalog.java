package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.domain.InterfaceLanguage;
import com.jackpotsaver.bot.domain.Platform;
import com.jackpotsaver.bot.config.BotProperties;
import com.jackpotsaver.bot.repository.BotTextRepository;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MessageCatalog {
    private final BotProperties botProperties;
    private final BotTextRepository botTextRepository;

    public MessageCatalog(BotProperties botProperties, BotTextRepository botTextRepository) {
        this.botProperties = botProperties;
        this.botTextRepository = botTextRepository;
    }

    public String start(InterfaceLanguage language) {
        return text("start", language);
    }

    public String help(InterfaceLanguage language) {
        return text("help", language);
    }

    public String loading(InterfaceLanguage language) {
        return text("loading", language);
    }

    public String sendLink(InterfaceLanguage language) {
        return text("send_link", language);
    }

    public String unsupported(InterfaceLanguage language) {
        return text("unsupported", language);
    }

    public String platformDisabled(InterfaceLanguage language) {
        return text("platform_disabled", language);
    }

    public String blocked(InterfaceLanguage language) {
        return text("blocked", language);
    }

    public String limit(InterfaceLanguage language) {
        return text("limit", language);
    }

    public String languageChanged(InterfaceLanguage language) {
        return text("language_changed", language);
    }

    public String unavailable(InterfaceLanguage language) {
        return text("unavailable", language);
    }

    public String serverError(InterfaceLanguage language) {
        return text("server_error", language);
    }

    public String tooLarge(InterfaceLanguage language) {
        return text("too_large", language);
    }

    public String caption(Platform platform, InterfaceLanguage language) {
        return "скачано с <a href=\"" + botLink() + "\">JackpotSaverBot</a>";
    }

    private String botLink() {
        String username = botProperties.username();
        if (username == null || username.isBlank()) {
            return "бота";
        }
        return "https://t.me/" + username.replace("@", "");
    }

    private String text(String key, InterfaceLanguage language) {
        return botTextRepository.findFirstByMessageKeyAndLanguage(key, language)
                .map(text -> text.getTextValue())
                .orElseGet(() -> fallback(key, language));
    }

    private String fallback(String key, InterfaceLanguage language) {
        String value = FALLBACKS.get(key + "." + language.name());
        if (value != null) {
            return value;
        }
        return FALLBACKS.getOrDefault(key + ".RU", key);
    }

    private static final Map<String, String> FALLBACKS = Map.ofEntries(
            Map.entry("start.RU", "Привет! Я помогу скачать видео из YouTube, Instagram и TikTok.\n"
                    + "Просто отправь мне ссылку на видео, а я пришлю тебе файл.\n"
                    + "Видео из YouTube скачиваются в низком качестве, чтобы ускорить обработку.\n"
                    + "Для YouTube Shorts, Instagram и TikTok качество выбирается автоматически."),
            Map.entry("start.EN", "Hi! I can download videos from YouTube, Instagram and TikTok.\n"
                    + "Send me a video link and I will send the file back.\n"
                    + "YouTube videos are downloaded in low quality to speed up processing.\n"
                    + "For YouTube Shorts, Instagram and TikTok quality is selected automatically."),
            Map.entry("help.RU", "Как пользоваться ботом:\n1. Отправь ссылку на видео.\n2. Дождись сообщения «Загрузка...».\n3. Получи готовый видеофайл.\n\nПоддерживаются: YouTube, YouTube Shorts, Instagram и TikTok.\nYouTube скачивается в низком качестве без выбора."),
            Map.entry("help.EN", "How to use the bot:\n1. Send a video link.\n2. Wait for the \"Loading...\" message.\n3. Receive the video file.\n\nSupported: YouTube, YouTube Shorts, Instagram and TikTok.\nYouTube is downloaded in low quality without selection."),
            Map.entry("loading.RU", "Загрузка..."),
            Map.entry("loading.EN", "Loading..."),
            Map.entry("send_link.RU", "Отправь ссылку на видео из YouTube, Instagram или TikTok."),
            Map.entry("send_link.EN", "Send a video link from YouTube, Instagram or TikTok."),
            Map.entry("unsupported.RU", "Эта ссылка не поддерживается."),
            Map.entry("unsupported.EN", "This link is not supported."),
            Map.entry("platform_disabled.RU", "Загрузка с этой платформы временно отключена."),
            Map.entry("platform_disabled.EN", "Downloads from this platform are temporarily disabled."),
            Map.entry("blocked.RU", "Доступ к боту ограничен."),
            Map.entry("blocked.EN", "Access to the bot is restricted."),
            Map.entry("limit.RU", "Лимит загрузок временно исчерпан. Попробуй позже."),
            Map.entry("limit.EN", "Download limit is temporarily exhausted. Try again later."),
            Map.entry("choose_quality.RU", "Выбери качество видео:"),
            Map.entry("choose_quality.EN", "Choose video quality:"),
            Map.entry("checking_quality.RU", "Проверяю доступные качества видео..."),
            Map.entry("checking_quality.EN", "Checking available video qualities..."),
            Map.entry("language_changed.RU", "Язык интерфейса изменен."),
            Map.entry("language_changed.EN", "Interface language changed."),
            Map.entry("unavailable.RU", "Не удалось скачать видео. Возможно, оно удалено, приватное или недоступно."),
            Map.entry("unavailable.EN", "Could not download the video. It may be deleted, private or unavailable."),
            Map.entry("server_error.RU", "Произошла ошибка. Попробуй позже."),
            Map.entry("server_error.EN", "An error occurred. Try again later."),
            Map.entry("too_large.RU", "Видео слишком большое для отправки через Telegram."),
            Map.entry("too_large.EN", "The video is too large to send through Telegram."),
            Map.entry("no_suitable_quality.RU", "Это видео слишком большое для отправки через Telegram."),
            Map.entry("no_suitable_quality.EN", "This video is too large to send through Telegram."),
            Map.entry("quality_check_unavailable.RU", "Не удалось проверить качества: YouTube сейчас не отвечает. Попробуй позже."),
            Map.entry("quality_check_unavailable.EN", "Could not check available qualities because YouTube is not responding. Try again later."),
            Map.entry("auto_quality_fallback.RU", "Не удалось заранее проверить качества. Попробую автоматически подобрать качество, которое поместится в Telegram."),
            Map.entry("auto_quality_fallback.EN", "Could not pre-check qualities. I will automatically try to pick a quality that fits Telegram.")
    );
}
