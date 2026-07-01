package com.jackpotsaver.bot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private long telegramId;
    private String username;
    private String firstName;
    private String lastName;
    private String languageCode;
    @Enumerated(EnumType.STRING)
    private InterfaceLanguage interfaceLanguage;
    @Column(name = "is_blocked")
    private boolean blocked;
    @Enumerated(EnumType.STRING)
    private UserRole role;
    private Instant createdAt;
    private Instant lastActiveAt;

    protected User() {
    }

    public User(long telegramId, String username, String firstName, String lastName, String languageCode, Instant now, boolean admin) {
        this.telegramId = telegramId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.languageCode = languageCode;
        this.interfaceLanguage = InterfaceLanguage.RU;
        this.role = admin ? UserRole.ADMIN : UserRole.USER;
        this.createdAt = now;
        this.lastActiveAt = now;
    }

    public Long getId() {
        return id;
    }

    public long getTelegramId() {
        return telegramId;
    }

    public InterfaceLanguage getInterfaceLanguage() {
        return interfaceLanguage;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public UserRole getRole() {
        return role;
    }

    public void refresh(String username, String firstName, String lastName, String languageCode, Instant now, boolean admin) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.languageCode = languageCode;
        if (admin) {
            this.role = UserRole.ADMIN;
        }
        this.lastActiveAt = now;
    }

    public void setInterfaceLanguage(InterfaceLanguage interfaceLanguage, Instant now) {
        this.interfaceLanguage = interfaceLanguage;
        this.lastActiveAt = now;
    }

    public void setBlocked(boolean blocked, Instant now) {
        this.blocked = blocked;
        this.lastActiveAt = now;
    }

    public void setRole(UserRole role, Instant now) {
        this.role = role;
        this.lastActiveAt = now;
    }

    public boolean admin() {
        return role == UserRole.ADMIN;
    }
}
