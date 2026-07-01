package com.jackpotsaver.bot.repository;

import com.jackpotsaver.bot.domain.AdminAction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionRepository extends JpaRepository<AdminAction, Long> {
}
