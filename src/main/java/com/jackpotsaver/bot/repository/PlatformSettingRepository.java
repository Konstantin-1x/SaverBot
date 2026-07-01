package com.jackpotsaver.bot.repository;

import com.jackpotsaver.bot.domain.Platform;
import com.jackpotsaver.bot.domain.PlatformSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, Long> {
    Optional<PlatformSetting> findByPlatform(Platform platform);
}
