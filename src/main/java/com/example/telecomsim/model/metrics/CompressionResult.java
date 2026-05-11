package com.example.telecomsim.model.metrics;

import com.example.telecomsim.model.compression.CompressionAlgorithm;
import lombok.Builder;
import lombok.Getter;

/**
 * Результат измерения сжатия для одного алгоритма и одного прогона.
 * Иммутабельный объект — создаётся только через Builder.
 */
@Getter
@Builder

public class CompressionResult {
    /** Алгоритм, которым были получены результаты. */
    private final CompressionAlgorithm algorithm;

    // ── Размеры ──────────────────────────────────────────────────────────

    /** Размер исходных данных (байт). */
    private final long originalSizeBytes;

    /** Размер после сжатия (байт). */
    private final long compressedSizeBytes;

    /**
     * Сжатые байты — нужны SimulationService для передачи в ChannelSimulator.
     * Не отображаются в UI напрямую.
     */
    private final byte[] compressedData;

    // ── Метрики эффективности сжатия ─────────────────────────────────────

    /**
     * Коэффициент сжатия: originalSize / compressedSize.
     * Значение > 1 означает, что данные сжались.
     * Значение < 1 означает, что данные «распухли» (редко, для случайных данных).
     */
    private final double compressionRatio;

    /**
     * Экономия места в процентах: (1 - compressedSize / originalSize) * 100.
     * Положительное — сжатие уменьшило размер.
     * Отрицательное — размер увеличился.
     */
    private final double spaceSavingPercent;

    // ── Временны́е метрики ────────────────────────────────────────────────

    /** Время сжатия (наносекунды). */
    private final long compressionTimeNs;

    /** Время распаковки (наносекунды). */
    private final long decompressionTimeNs;

    // ── Скоростные метрики ────────────────────────────────────────────────

    /** Скорость сжатия (Мбит/с). */
    private final double compressionSpeedMbps;

    /** Скорость распаковки (Мбит/с). */
    private final double decompressionSpeedMbps;

    // ── Контроль целостности ──────────────────────────────────────────────

    /**
     * Флаг целостности данных.
     * true  — данные после compress → decompress совпадают с исходными.
     * false — ошибка алгоритма или повреждение (не должно происходить для lossless).
     */
    private final boolean dataIntegrityOk;

    // ── Вычисляемые свойства ──────────────────────────────────────────────

    /** Время сжатия в миллисекундах (удобно для отображения). */
    public double getCompressionTimeMs() {
        return compressionTimeNs / 1_000_000.0;
    }

    /** Время распаковки в миллисекундах. */
    public double getDecompressionTimeMs() {
        return decompressionTimeNs / 1_000_000.0;
    }

    /** Overhead: насколько сжатые данные больше/меньше исходных (байт). */
    public long getSizeDeltaBytes() {
        return compressedSizeBytes - originalSizeBytes;
    }
}
