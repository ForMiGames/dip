package com.example.telecomsim.model.compression;

import lombok.Builder;
import lombok.Getter;

/**
 * Настройки алгоритма сжатия.
 * Не все поля применимы ко всем алгоритмам —
 * каждый сервис берёт только нужные ему параметры.
 */
@Getter
@Builder
public class CompressionSettings {
    /**
     * Уровень сжатия.
     * DEFLATE: 1–9 | Zstd: 1–22 | LZ4: 0 (fast) или 1–17 (HC).
     * 0 — использовать значение по умолчанию алгоритма.
     */
    @Builder.Default
    private final int compressionLevel = 0;

    /**
     * Размер скользящего окна (байт).
     * Применяется в LZ77, DEFLATE.
     * 0 — использовать значение по умолчанию.
     */
    @Builder.Default
    private final int windowSize = 0;

    /**
     * Размер буфера просмотра вперёд (байт).
     * Применяется в LZ77.
     * 0 — использовать значение по умолчанию.
     */
    @Builder.Default
    private final int lookaheadSize = 0;

    /**
     * Включить вычисление контрольной суммы (CRC/Adler).
     * Применяется в DEFLATE (GZIP).
     */
    @Builder.Default
    private final boolean enableChecksum = true;

    /**
     * Предустановка настроек по умолчанию для алгоритма.
     */
    public static CompressionSettings defaults() {
        return CompressionSettings.builder().build();
    }
}
