package com.jackpotsaver.bot.repository;

import com.jackpotsaver.bot.domain.BotText;
import com.jackpotsaver.bot.domain.InterfaceLanguage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotTextRepository extends JpaRepository<BotText, Long> {
    Optional<BotText> findFirstByMessageKeyAndLanguage(String messageKey, InterfaceLanguage language);
}
