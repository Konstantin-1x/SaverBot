package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.config.AdminProperties;
import com.jackpotsaver.bot.domain.InterfaceLanguage;
import com.jackpotsaver.bot.domain.User;
import com.jackpotsaver.bot.repository.UserRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository repository;
    private final AdminProperties adminProperties;
    private final Clock clock;

    public UserService(UserRepository repository, AdminProperties adminProperties, Clock clock) {
        this.repository = repository;
        this.adminProperties = adminProperties;
        this.clock = clock;
    }

    @Transactional
    public User upsert(TelegramUser telegramUser) {
        boolean admin = isConfiguredAdmin(telegramUser.telegramId());
        return repository.findByTelegramId(telegramUser.telegramId())
                .map(user -> {
                    user.refresh(telegramUser.username(), telegramUser.firstName(), telegramUser.lastName(),
                            telegramUser.languageCode(), clock.instant(), admin);
                    return user;
                })
                .orElseGet(() -> {
                    User user = new User(telegramUser.telegramId(), telegramUser.username(), telegramUser.firstName(),
                            telegramUser.lastName(), telegramUser.languageCode(), clock.instant(), admin);
                    log.info("Registered a new Telegram user");
                    return repository.save(user);
                });
    }

    private boolean isConfiguredAdmin(long telegramId) {
        return adminProperties.telegramIdSet().contains(telegramId);
    }

    @Transactional
    public void setLanguage(User user, InterfaceLanguage language) {
        user.setInterfaceLanguage(language, clock.instant());
        log.info("User {} changed language to {}", user.getId(), language);
    }
}
