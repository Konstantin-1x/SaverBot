package com.jackpotsaver.bot.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Entity
@Table(name = "bot_texts")
public class BotText {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String messageKey;
    @Enumerated(EnumType.STRING)
    private InterfaceLanguage language;
    @Column(columnDefinition = "text")
    private String textValue;

    protected BotText() {
    }

    public String getTextValue() {
        return textValue;
    }
}
