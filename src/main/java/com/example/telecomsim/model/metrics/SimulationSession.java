package com.example.telecomsim.model.metrics;

import com.example.telecomsim.model.compression.CompressionAlgorithm;
import com.example.telecomsim.model.simulation.SimulationConfig;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Сессия симуляции — контейнер результатов одного сравнительного запуска.
 * Содержит результаты по всем выбранным алгоритмам и метаданные запуска.
 */
@Getter
@Builder
public class SimulationSession {
    /**
     * Уникальный идентификатор сессии.
     * Генерируется автоматически при создании через Builder.
     */
    @Builder.Default
    private final String sessionId = UUID.randomUUID().toString();

    /** Конфигурация, с которой была запущена симуляция. */
    private final SimulationConfig config;

    /**
     * Список результатов — по одному на каждый алгоритм из config.
     * Порядок соответствует порядку алгоритмов в config.getAlgorithmsToCompare().
     */
    private final List<SimulationResult> results;

    /** Время начала симуляции. */
    private final LocalDateTime startTime;

    /** Время завершения симуляции. */
    private final LocalDateTime endTime;

    // ── Вычисляемые свойства ──────────────────────────────────────────────

    /**
     * Общее время выполнения симуляции.
     */
    public Duration getTotalDuration() {
        if (startTime == null || endTime == null) return Duration.ZERO;
        return Duration.between(startTime, endTime);
    }

    /**
     * Количество протестированных алгоритмов.
     */
    public int getAlgorithmCount() {
        return results == null ? 0 : results.size();
    }

    /**
     * Возвращает алгоритм с наилучшей интегральной оценкой.
     */
    public Optional<SimulationResult> getBestByEfficiencyScore() {
        if (results == null || results.isEmpty()) return Optional.empty();
        return results.stream()
                .max(Comparator.comparingDouble(SimulationResult::getOverallEfficiencyScore));
    }

    /**
     * Возвращает алгоритм с наивысшим коэффициентом сжатия.
     */
    public Optional<SimulationResult> getBestByCompressionRatio() {
        if (results == null || results.isEmpty()) return Optional.empty();
        return results.stream()
                .max(Comparator.comparingDouble(SimulationResult::getCompressionRatio));
    }

    /**
     * Возвращает алгоритм с наибольшей эффективной скоростью передачи.
     */
    public Optional<SimulationResult> getBestByTransferRate() {
        if (results == null || results.isEmpty()) return Optional.empty();
        return results.stream()
                .max(Comparator.comparingDouble(SimulationResult::getEffectiveTransferRateMbps));
    }

    /**
     * Возвращает результат для конкретного алгоритма.
     */
    public Optional<SimulationResult> getResultFor(CompressionAlgorithm algorithm) {
        if (results == null) return Optional.empty();
        return results.stream()
                .filter(r -> r.getAlgorithm() == algorithm)
                .findFirst();
    }

    /**
     * Проверяет, все ли алгоритмы сохранили целостность данных.
     */
    public boolean isAllDataIntegrityOk() {
        if (results == null || results.isEmpty()) return false;
        return results.stream().allMatch(SimulationResult::isDataIntegrityOk);
    }
}
