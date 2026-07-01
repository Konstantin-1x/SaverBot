package com.jackpotsaver.bot.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AdminConversationService {
    private final Map<Long, AdminConversationState> states = new ConcurrentHashMap<>();

    public void set(long telegramId, AdminConversationState state) {
        states.put(telegramId, state);
    }

    public Optional<AdminConversationState> take(long telegramId) {
        return Optional.ofNullable(states.remove(telegramId));
    }

    public boolean clear(long telegramId) {
        return states.remove(telegramId) != null;
    }
}
