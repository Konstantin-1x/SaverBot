package com.jackpotsaver.bot.repository;

import com.jackpotsaver.bot.domain.AdSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdSettingsRepository extends JpaRepository<AdSettings, Long> {
}
