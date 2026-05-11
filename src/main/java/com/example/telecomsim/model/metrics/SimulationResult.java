package com.example.telecomsim.model.metrics;

import com.example.telecomsim.model.compression.CompressionAlgorithm;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Итоговый результат симуляции для одного алгоритма сжатия.
 * Агрегирует CompressionResult и TransmissionResult,
 * добавляет интегральные метрики для сравнительных графиков.
 */
@Getter
@Builder
public class SimulationResult {
    /** Алгоритм, для которого получен результат. */
    private final CompressionAlgorithm algorithm;

    /** Результаты измерения сжатия. */
    private final CompressionResult compressionResult;

    /** Результаты симуляции канала передачи. */
    private final TransmissionResult transmissionResult;

    /**
     * Интегральная оценка эффективности [0..100].
     * Учитывает степень сжатия, скорость и качество доставки.
     * Используется для Radar-графика.
     */
    private final double overallEfficiencyScore;

    /**
     * Итоговая эффективная скорость передачи оригинальных данных (Мбит/с).
     * Учитывает: время сжатия + время передачи сжатых данных + время распаковки.
     * Главная метрика для сравнения алгоритмов на конкретном канале.
     */
    private final double effectiveTransferRateMbps;

    /** Метка времени завершения обработки данного алгоритма. */
    private final LocalDateTime timestamp;

    // ── Удобные делегирующие методы для UI ───────────────────────────────

    public double getCompressionRatio() {
        return compressionResult.getCompressionRatio();
    }

    public double getSpaceSavingPercent() {
        return compressionResult.getSpaceSavingPercent();
    }

    public double getCompressionSpeedMbps() {
        return compressionResult.getCompressionSpeedMbps();
    }

    public double getDecompressionSpeedMbps() {
        return compressionResult.getDecompressionSpeedMbps();
    }

    public double getEffectiveThroughputMbps() {
        return transmissionResult.getEffectiveThroughputMbps();
    }

    public double getDeliveryRatePercent() {
        return transmissionResult.getDeliveryRatePercent();
    }

    public double getPacketLossPercent() {
        return transmissionResult.getPacketLossPercent();
    }

    public boolean isDataIntegrityOk() {
        return compressionResult.isDataIntegrityOk();
    }
}
