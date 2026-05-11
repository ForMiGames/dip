package com.example.telecomsim.service.history;

import com.example.telecomsim.model.metrics.SimulationSession;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Сервис хранения истории симуляций.
 * Текущая реализация — in-memory (данные живут до закрытия приложения).
 */
public class SimulationHistoryService {
    private static final int MAX_HISTORY_SIZE = 100;

    /** Хранилище: новые записи в начале. */
    private final LinkedList<SimulationSession> sessions = new LinkedList<>();

    public void save(SimulationSession session) {
        sessions.addFirst(session);
        if (sessions.size() > MAX_HISTORY_SIZE) {
            sessions.removeLast();
        }
    }

    public List<SimulationSession> loadAll() {
        return new ArrayList<>(sessions);
    }

    public void delete(String sessionId) {
        sessions.removeIf(s -> s.getSessionId().equals(sessionId));
    }

    public void deleteAll() {
        sessions.clear();
    }

    public int getCount() {
        return sessions.size();
    }
}
