package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.config.AdminProperties;
import com.jackpotsaver.bot.domain.AdminAction;
import com.jackpotsaver.bot.domain.FileStatus;
import com.jackpotsaver.bot.domain.JobStatus;
import com.jackpotsaver.bot.domain.Platform;
import com.jackpotsaver.bot.domain.RequestStatus;
import com.jackpotsaver.bot.domain.User;
import com.jackpotsaver.bot.domain.UserRole;
import com.jackpotsaver.bot.repository.AdminActionRepository;
import com.jackpotsaver.bot.repository.DownloadJobRepository;
import com.jackpotsaver.bot.repository.DownloadRequestRepository;
import com.jackpotsaver.bot.repository.ErrorLogRepository;
import com.jackpotsaver.bot.repository.StoredFileRepository;
import com.jackpotsaver.bot.repository.UserRepository;
import com.jackpotsaver.bot.telegram.TelegramApiClient;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final DownloadRequestRepository requestRepository;
    private final DownloadJobRepository jobRepository;
    private final ErrorLogRepository errorLogRepository;
    private final StoredFileRepository storedFileRepository;
    private final AdminActionRepository adminActionRepository;
    private final PlatformService platformService;
    private final AdService adService;
    private final TelegramApiClient telegramApiClient;
    private final Clock clock;
    private final AdminProperties adminProperties;

    public AdminService(UserRepository userRepository, DownloadRequestRepository requestRepository,
                        DownloadJobRepository jobRepository, ErrorLogRepository errorLogRepository,
                        StoredFileRepository storedFileRepository, AdminActionRepository adminActionRepository,
                        PlatformService platformService, AdService adService, TelegramApiClient telegramApiClient,
                        Clock clock, AdminProperties adminProperties) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.jobRepository = jobRepository;
        this.errorLogRepository = errorLogRepository;
        this.storedFileRepository = storedFileRepository;
        this.adminActionRepository = adminActionRepository;
        this.platformService = platformService;
        this.adService = adService;
        this.telegramApiClient = telegramApiClient;
        this.clock = clock;
        this.adminProperties = adminProperties;
    }

    public String stats() {
        var dayAgo = clock.instant().minus(Duration.ofDays(1));
        return "Статистика:\n"
                + "Пользователей всего: " + userRepository.count() + "\n"
                + "Новых за день: " + userRepository.countByCreatedAtAfter(dayAgo) + "\n"
                + "Запросов за день: " + requestRepository.countByCreatedAtAfter(dayAgo) + "\n"
                + "Успешных за день: " + requestRepository.countByStatusAndCreatedAtAfter(RequestStatus.SUCCESS, dayAgo) + "\n"
                + "Ошибок за день: " + errorLogRepository.countByCreatedAtAfter(dayAgo) + "\n"
                + "Файлов хранится: " + storedFileRepository.countByStatus(FileStatus.AVAILABLE) + "\n"
                + "Занято байт: " + storedFileRepository.totalAvailableBytes() + "\n"
                + "Активных задач: " + jobRepository.countByStatus(JobStatus.RUNNING);
    }

    @Transactional
    public String block(User admin, long telegramId, boolean blocked) {
        Optional<User> target = userRepository.findByTelegramId(telegramId);
        if (target.isEmpty()) {
            return "Пользователь не найден.";
        }
        target.get().setBlocked(blocked, clock.instant());
        adminActionRepository.save(new AdminAction(admin, blocked ? "BLOCK_USER" : "UNBLOCK_USER", target.get(),
                null, clock.instant()));
        return blocked ? "Пользователь заблокирован." : "Пользователь разблокирован.";
    }

    @Transactional
    public String platforms(String[] args) {
        if (args.length == 3) {
            Platform platform = parsePlatform(args[1]);
            boolean enabled = args[2].equalsIgnoreCase("on") || args[2].equalsIgnoreCase("enable");
            if (platform == null || !platformService.setEnabled(platform, enabled)) {
                return "Использование: /platforms YOUTUBE|YOUTUBE_SHORTS|INSTAGRAM|TIKTOK on|off";
            }
            return "Платформа " + platform + " " + (enabled ? "включена." : "отключена.");
        }
        return "Платформы: " + Arrays.toString(Platform.values())
                + "\nИспользование: /platforms YOUTUBE on";
    }

    public String errors() {
        StringBuilder builder = new StringBuilder("Последние ошибки:");
        errorLogRepository.findTop10ByOrderByCreatedAtDesc()
                .forEach(error -> builder.append("\n#").append(error.hashCode()).append(" см. БД error_logs"));
        return builder.toString();
    }

    @Transactional
    public String setAfterDownloadAd(User admin, String text) {
        if (text == null || text.isBlank()) {
            return "Использование: /ad_after текст рекламы";
        }
        String normalized = text.trim();
        adService.setAfterDownloadText(normalized, admin);
        adminActionRepository.save(new AdminAction(admin, "SET_AFTER_DOWNLOAD_AD", null, null, clock.instant()));
        return "Реклама после скачивания обновлена.";
    }

    @Transactional
    public String clearAfterDownloadAd(User admin) {
        adService.clearAfterDownloadText(admin);
        adminActionRepository.save(new AdminAction(admin, "CLEAR_AFTER_DOWNLOAD_AD", null, null, clock.instant()));
        return "Реклама после скачивания отключена.";
    }

    @Transactional
    public String broadcast(User admin, String text) {
        if (text == null || text.isBlank()) {
            return "Использование: /broadcast текст рассылки";
        }
        int success = 0;
        int failed = 0;
        String normalized = text.trim();
        for (User user : userRepository.findAll()) {
            try {
                telegramApiClient.sendMessage(user.getTelegramId(), normalized);
                success++;
            } catch (RuntimeException ex) {
                failed++;
            }
        }
        adminActionRepository.save(new AdminAction(admin, "BROADCAST", null,
                "success=" + success + "; failed=" + failed, clock.instant()));
        return "Рассылка завершена. Успешно: " + success + ", ошибок: " + failed + ".";
    }

    public String help() {
        return """
                Админ-команды:
                /admin_stats - статистика
                /block <telegram_id> - заблокировать пользователя
                /unblock <telegram_id> - разблокировать пользователя
                /platforms <YOUTUBE|YOUTUBE_SHORTS|INSTAGRAM|TIKTOK> <on|off> - платформы
                /ad_after <текст> - реклама после каждого скачивания
                /ad_after_off - отключить рекламу после скачивания
                /broadcast <текст> - разовая рассылка всем пользователям
                Добавление и удаление админов доступно через кнопки админ-панели.
                /errors - последние ошибки
                """;
    }

    @Transactional
    public String addAdmin(User admin, String username) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.isBlank()) {
            return "Отправьте username пользователя, например @username.";
        }
        User target = userRepository.findFirstByUsernameIgnoreCase(normalizedUsername).orElse(null);
        if (target == null) {
            return "Пользователь @" + normalizedUsername + " не найден в базе. Он должен хотя бы раз написать боту.";
        }
        target.setRole(UserRole.ADMIN, clock.instant());
        adminActionRepository.save(new AdminAction(admin, "ADD_ADMIN", target, null, clock.instant()));
        return "Админ добавлен: @" + normalizedUsername;
    }

    @Transactional
    public String removeAdmin(User admin, String username) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.isBlank()) {
            return "Отправьте username админа, например @username.";
        }
        User target = userRepository.findFirstByUsernameIgnoreCase(normalizedUsername).orElse(null);
        if (target == null) {
            return "Пользователь @" + normalizedUsername + " не найден в базе.";
        }
        if (adminProperties.telegramIdSet().contains(target.getTelegramId())) {
            return "Нельзя удалить основного админа.";
        }
        target.setRole(UserRole.USER, clock.instant());
        adminActionRepository.save(new AdminAction(admin, "REMOVE_ADMIN", target, null, clock.instant()));
        return "Админ удален: @" + normalizedUsername;
    }

    private Platform parsePlatform(String raw) {
        try {
            return Platform.valueOf(raw.toUpperCase());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        String normalized = username.trim();
        return normalized.startsWith("@") ? normalized.substring(1) : normalized;
    }
}
