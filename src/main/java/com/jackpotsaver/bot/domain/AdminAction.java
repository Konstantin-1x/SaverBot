package com.jackpotsaver.bot.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "admin_actions")
public class AdminAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    private User adminUser;
    private String actionType;
    @ManyToOne(fetch = FetchType.LAZY)
    private User targetUser;
    private String description;
    private Instant createdAt;

    protected AdminAction() {
    }

    public AdminAction(User adminUser, String actionType, User targetUser, String description, Instant createdAt) {
        this.adminUser = adminUser;
        this.actionType = actionType;
        this.targetUser = targetUser;
        this.description = description;
        this.createdAt = createdAt;
    }
}
