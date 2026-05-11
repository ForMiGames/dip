package com.example.telecomsim.model.simulation;

import com.example.telecomsim.model.channel.ChannelParameters;
import com.example.telecomsim.model.compression.CompressionAlgorithm;
import com.example.telecomsim.model.compression.CompressionSettings;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Конфигурация одного запуска симуляции.
 * Иммутабельный объект — все поля задаются через Builder до запуска.
 *
 * Пример создания:
 * <pre>{@code
 * SimulationConfig config = SimulationConfig.builder()
 *     .simulationName("Тест Wi-Fi канала")
 *     .algorithm(CompressionAlgorithm.HUFFMAN)
 *     .algorithm(CompressionAlgorithm.LZ77)
 *     .channelParameters(wifiParams)
 *     .dataType(DataType.TEXT_UTF8)
 *     .dataSizeBytes(1024 * 1024)  // 1 МБ
 *     .repetitions(5)
 *     .protocol(TransmissionProtocol.TCP)
 *     .build();
 * }</pre>
 */
@Getter
@Builder
public class SimulationConfig {
    // ── Метаданные ────────────────────────────────────────────────────────

    /**
     * Название симуляции (для истории и отображения в UI).
     * По умолчанию генерируется автоматически.
     */
    @Builder.Default
    private final String simulationName = "Симуляция " +
            java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

    // ── Алгоритмы сжатия ──────────────────────────────────────────────────

    /**
     * Список алгоритмов для сравнения.
     * {@code @Singular} позволяет добавлять алгоритмы по одному:
     * .algorithm(HUFFMAN).algorithm(LZ77)
     * или списком: .algorithmsToCompare(list)
     */
    @Singular("algorithm")
    private final List<CompressionAlgorithm> algorithmsToCompare;

    /**
     * Индивидуальные настройки сжатия для каждого алгоритма.
     * Ключ — алгоритм, значение — его настройки.
     * Если для алгоритма нет записи — используются настройки по умолчанию.
     *
     * {@code @Singular} позволяет добавлять записи по одной:
     * .compressionSetting(DEFLATE, DeflateSettings)
     */
    @Singular("compressionSetting")
    private final Map<CompressionAlgorithm, CompressionSettings> compressionSettings;

    // ── Параметры канала ──────────────────────────────────────────────────

    /** Параметры симулируемого канала связи. */
    private final ChannelParameters channelParameters;

    // ── Входные данные ────────────────────────────────────────────────────

    /** Тип генерируемых тестовых данных. */
    @Builder.Default
    private final DataType dataType = DataType.TEXT_UTF8;

    /**
     * Размер входных данных для симуляции (байт).
     * По умолчанию — 1 МБ.
     */
    @Builder.Default
    private final long dataSizeBytes = 1024 * 1024L;

    /**
     * Путь к файлу пользователя.
     * Используется только если dataType == DataType.REAL_FILE.
     * Иначе игнорируется.
     */
    private final Path userFilePath;

    // ── Параметры симуляции ───────────────────────────────────────────────

    /**
     * Количество повторений каждого измерения для усреднения результатов.
     * Минимум 1, рекомендуется 3–10 для стабильных замеров.
     */
    @Builder.Default
    private final int repetitions = 3;

    /**
     * Протокол передачи данных.
     * TCP — с подтверждением и перепередачей.
     * UDP — без гарантий доставки.
     */
    @Builder.Default
    private final TransmissionProtocol protocol = TransmissionProtocol.TCP;

    // ── Вспомогательные методы ────────────────────────────────────────────

    /**
     * Возвращает настройки сжатия для конкретного алгоритма.
     * Если настройки не заданы явно — возвращает null
     * (сервис сжатия использует свои значения по умолчанию).
     */
    public CompressionSettings getCompressionSettings(CompressionAlgorithm algorithm) {
        if (compressionSettings == null) return null;
        return compressionSettings.get(algorithm);
    }

    /**
     * Проверяет, используется ли файл пользователя как источник данных.
     */
    public boolean isUsingUserFile() {
        return dataType == DataType.REAL_FILE && userFilePath != null;
    }

    /**
     * Проверяет корректность конфигурации.
     * Используется в SimulationServiceImpl.validateConfig().
     */
    public List<String> validate() {
        List<String> errors = new java.util.ArrayList<>();

        if (algorithmsToCompare == null || algorithmsToCompare.isEmpty()) {
            errors.add("Не выбран ни один алгоритм сжатия");
        }
        if (channelParameters == null) {
            errors.add("Параметры канала не заданы");
        }
        if (dataSizeBytes <= 0) {
            errors.add("Размер данных должен быть больше нуля");
        }
        if (dataType == DataType.REAL_FILE && userFilePath == null) {
            errors.add("Выбран тип 'Файл пользователя', но файл не указан");
        }
        if (dataType == DataType.REAL_FILE && userFilePath != null
                && !userFilePath.toFile().exists()) {
            errors.add("Указанный файл не существует: " + userFilePath);
        }
        if (repetitions < 1) {
            errors.add("Количество повторений должно быть не менее 1");
        }

        return errors;
    }
}
