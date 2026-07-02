package com.jackpotsaver.bot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jackpotsaver.bot.config.AdminProperties;
import com.jackpotsaver.bot.domain.User;
import com.jackpotsaver.bot.domain.UserRole;
import com.jackpotsaver.bot.repository.AdminActionRepository;
import com.jackpotsaver.bot.repository.DownloadJobRepository;
import com.jackpotsaver.bot.repository.DownloadRequestRepository;
import com.jackpotsaver.bot.repository.ErrorLogRepository;
import com.jackpotsaver.bot.repository.StoredFileRepository;
import com.jackpotsaver.bot.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdminServiceTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AdminActionRepository actionRepository = mock(AdminActionRepository.class);
    private final AdminService adminService = new AdminService(
            userRepository,
            mock(DownloadRequestRepository.class),
            mock(DownloadJobRepository.class),
            mock(ErrorLogRepository.class),
            mock(StoredFileRepository.class),
            actionRepository,
            mock(PlatformService.class),
            mock(AdService.class),
            mock(TelegramContentSender.class),
            Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC),
            new AdminProperties("42")
    );

    @Test
    void addsAdminByUsername() {
        User admin = user(1, "owner", true);
        User target = user(2, "newadmin", false);
        when(userRepository.findFirstByUsernameIgnoreCase("newadmin")).thenReturn(Optional.of(target));

        String result = adminService.addAdmin(admin, "@newadmin");

        assertThat(result).contains("@newadmin");
        assertThat(target.getRole()).isEqualTo(UserRole.ADMIN);
        verify(actionRepository).save(any());
    }

    @Test
    void reportsMissingUsernameWhenAddingAdmin() {
        User admin = user(1, "owner", true);
        when(userRepository.findFirstByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());

        String result = adminService.addAdmin(admin, "missing");

        assertThat(result).contains("не найден");
    }

    @Test
    void doesNotRemoveSuperAdminByUsername() {
        User admin = user(1, "owner", true);
        User superAdmin = user(42, "configuredadmin", true);
        when(userRepository.findFirstByUsernameIgnoreCase("configuredadmin")).thenReturn(Optional.of(superAdmin));

        String result = adminService.removeAdmin(admin, "@configuredadmin");

        assertThat(result).contains("Нельзя удалить");
        assertThat(superAdmin.getRole()).isEqualTo(UserRole.ADMIN);
    }

    private User user(long telegramId, String username, boolean admin) {
        return new User(telegramId, username, "First", "Last", "ru", Instant.parse("2026-06-15T00:00:00Z"), admin);
    }
}
