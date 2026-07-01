package com.jackpotsaver.bot.repository;

import com.jackpotsaver.bot.domain.User;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTelegramId(long telegramId);

    Optional<User> findFirstByUsernameIgnoreCase(String username);

    long countByCreatedAtAfter(Instant after);
}
