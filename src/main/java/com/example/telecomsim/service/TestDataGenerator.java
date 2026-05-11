package com.example.telecomsim.service;

import com.example.telecomsim.model.simulation.DataType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

import static java.util.Collections.replaceAll;

/**
 * Генератор тестовых данных для симуляции сжатия.
 *
 * Каждый тип специально сконструирован так, чтобы показывать
 * реалистичные коэффициенты сжатия:
 *
 *  TEXT_UTF8         → 2.5–4x   (повторяющиеся слова, предложения)
 *  TEXT_ASCII        → 3–6x     (логи с одинаковой структурой строк)
 *  BINARY_STRUCTURED → 1.5–2.5x (заголовки пакетов с повторами + случайный payload)
 *  BINARY_RANDOM     → ~1x      (высокая энтропия — сжатие невозможно)
 *  REAL_FILE         → зависит от файла
 */
public class TestDataGenerator {

    // Фиксированный seed для воспроизводимости результатов между запусками
    private static final long SEED = 42L;

    // ── Публичный API ─────────────────────────────────────────────────────

    /**
     * Генерирует тестовые данные заданного типа и размера.
     */
    public byte[] generate(DataType dataType, long sizeBytes) {
        validateSize(sizeBytes);
        int size = (int) Math.min(sizeBytes, Integer.MAX_VALUE);

        return switch (dataType) {
            case TEXT_UTF8         -> generateTextUtf8(size);
            case TEXT_ASCII        -> generateTextAscii(size);
            case BINARY_RANDOM     -> generateBinaryRandom(size);
            case BINARY_STRUCTURED -> generateBinaryStructured(size);
            case REAL_FILE         -> throw new IllegalArgumentException(
                    "Для REAL_FILE используйте generateFromFile(Path, long)"
            );
        };
    }

    /**
     * Читает данные из файла пользователя.
     * Если файл больше maxSizeBytes — берём только начало.
     * Если меньше — повторяем содержимое до нужного размера.
     */
    public byte[] generateFromFile(Path filePath, long maxSizeBytes) {
        validateSize(maxSizeBytes);
        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            if (fileBytes.length == 0) {
                throw new IllegalArgumentException("Файл пуст: " + filePath);
            }
            return adjustToTargetSize(fileBytes, (int) maxSizeBytes);
        } catch (IOException e) {
            throw new TestDataGeneratorException(
                    "Не удалось прочитать файл: " + filePath, e
            );
        }
    }

    // ── Генераторы по типу ────────────────────────────────────────────────

    /**
     * Текст UTF-8 — имитация реального русскоязычного текста.
     *
     * Ключ хорошего сжатия: высокая повторяемость слов и фраз.
     * Стратегия: генерируем параграфы из ограниченного словаря (~40 слов),
     * чередуем с числами и знаками препинания.
     *
     * Ожидаемый коэффициент: 2.5–4x (Huffman), 3–5x (DEFLATE/Zstd)
     */
    private byte[] generateTextUtf8(int sizeBytes) {
        // Ограниченный словарь → высокая повторяемость → хорошее сжатие
        String[] words = {
                "данные", "передача", "сеть", "канал", "протокол", "сжатие",
                "алгоритм", "пакет", "байт", "скорость", "задержка", "ошибка",
                "буфер", "поток", "интерфейс", "маршрутизатор", "коммутатор",
                "соединение", "сервер", "клиент", "запрос", "ответ", "заголовок",
                "телекоммуникации", "информация", "система", "сигнал", "частота",
                "модуляция", "кодирование", "декодирование", "шифрование",
                "пропускная", "способность", "надёжность", "эффективность"
        };

        // Шаблоны предложений — структурная повторяемость даёт лучшее сжатие
        String[] sentenceTemplates = {
                "Алгоритм сжатия %s обеспечивает высокую %s при передаче %s по каналу %s.\n",
                "Скорость передачи %s данных через %s составляет %d Мбит/с с задержкой %d мс.\n",
                "Протокол %s использует %s для обеспечения надёжной %s информации.\n",
                "Коэффициент сжатия %s при обработке %s данных равен %.2f.\n",
                "Система %s обеспечивает %s соединение между %s и %s узлами сети.\n",
                "Буфер %s содержит %d байт данных, ожидающих %s через %s.\n"
        };

        Random rnd = new Random(SEED);
        StringBuilder sb = new StringBuilder(sizeBytes + 256);

        while (sb.length() < sizeBytes) {
            // Выбираем шаблон предложения
            String template = sentenceTemplates[rnd.nextInt(sentenceTemplates.length)];

            // Заполняем шаблон словами из словаря
            // Считаем количество %s/%d/%f в шаблоне
            String sentence = fillTemplate(template, words, rnd);
            sb.append(sentence);

            // Периодически добавляем пустую строку — имитация параграфов
            if (rnd.nextInt(8) == 0) {
                sb.append("\n");
            }
        }

        return adjustToTargetSize(
                sb.toString().getBytes(StandardCharsets.UTF_8),
                sizeBytes
        );
    }

    /**
     * Заполняет шаблон словами, числами и вещественными значениями.
     */
    private String fillTemplate(String template, String[] words, Random rnd) {
        String result = template;

        // Заменяем %s словами
        while (result.contains("%s")) {
            result = result.replaceFirst(
                    "%s",
                    words[rnd.nextInt(words.length)]
            );
        }
        // Заменяем %d числами
        while (result.contains("%d")) {
            result = result.replaceFirst(
                    "%d",
                    String.valueOf(rnd.nextInt(1000))
            );
        }
        // Заменяем %.2f дробными
        while (result.contains("%.2f")) {
            result = result.replaceFirst(
                    "%.2f",
                    String.format("%.2f", 1.0 + rnd.nextDouble() * 9.0)
            );
        }
        return result;
    }

    /**
     * ASCII-текст — имитация лог-файлов с однородной структурой строк.
     *
     * Логи идеально сжимаются: каждая строка начинается одинаково,
     * отличаются только числовые значения.
     *
     * Ожидаемый коэффициент: 3–6x
     */
    private byte[] generateTextAscii(int sizeBytes) {
        // Уровни логов и компоненты — ограниченный набор → повторяемость
        String[] levels      = {"INFO ", "DEBUG", "WARN ", "ERROR"};
        String[] components  = {"channel", "compressor", "network", "buffer", "scheduler"};
        String[] operations  = {"compress", "transmit", "receive", "encode", "decode", "flush"};
        String[] statuses    = {"started", "completed", "failed", "retrying", "queued"};

        Random rnd = new Random(SEED);
        StringBuilder sb = new StringBuilder(sizeBytes + 128);
        int lineNumber = 0;

        while (sb.length() < sizeBytes) {
            // Структура строки лога — однородная, хорошо сжимается
            String level     = levels[rnd.nextInt(levels.length)];
            String component = components[rnd.nextInt(components.length)];
            String operation = operations[rnd.nextInt(operations.length)];
            String status    = statuses[rnd.nextInt(statuses.length)];

            // Формат: [уровень] [номер] компонент.операция статус метрики
            sb.append(String.format(
                    "[%s] [%06d] %s.%s %s bytes=%d latency=%dms ratio=%.3f session=%08X\n",
                    level,
                    lineNumber++,
                    component,
                    operation,
                    status,
                    rnd.nextInt(65536),
                    rnd.nextInt(500),
                    1.0 + rnd.nextDouble() * 8.0,
                    rnd.nextInt(Integer.MAX_VALUE)
            ));
        }

        return adjustToTargetSize(
                sb.toString().getBytes(StandardCharsets.US_ASCII),
                sizeBytes
        );
    }

    /**
     * Случайные бинарные данные — высокая энтропия, сжатие невозможно.
     *
     * Имитирует уже сжатые или зашифрованные данные.
     * Все алгоритмы покажут ratio ≈ 1.0 — это корректное поведение,
     * важное для демонстрации ограничений сжатия.
     *
     * Ожидаемый коэффициент: 0.95–1.05x (может даже увеличиться из-за заголовков)
     */
    private byte[] generateBinaryRandom(int sizeBytes) {
        // Новый Random без seed — данные каждый раз разные (максимальная энтропия)
        byte[] data = new byte[sizeBytes];
        new Random().nextBytes(data);
        return data;
    }

    /**
     * Структурированные бинарные данные — имитация сетевых протоколов.
     *
     * Структура записи (64 байта):
     *  [0..3]   Magic bytes  (постоянные 0xDEADBEEF)   → хорошо сжимаются
     *  [4]      Version      (почти всегда 0x01)        → хорошо сжимаются
     *  [5]      Flags        (8 вариантов)              → хорошо сжимаются
     *  [6..7]   Packet type  (4 типа)                   → хорошо сжимаются
     *  [8..11]  Source IP    (192.168.X.Y — типичная локалка)
     *  [12..15] Dest IP      (10.0.X.Y)
     *  [16..19] Sequence num (монотонный счётчик)       → умеренно сжимается
     *  [20..21] Checksum     (случайный)
     *  [22..23] Reserved     (всегда 0x0000)            → хорошо сжимаются
     *  [24..31] Timestamp    (монотонный)               → умеренно сжимается
     *  [32..63] Payload      (случайный, 32 байта)      → плохо сжимается
     *
     * Итог: ~50% структуры хорошо сжимается → ratio 1.5–2.5x
     */
    private byte[] generateBinaryStructured(int sizeBytes) {
        byte[] data       = new byte[sizeBytes];
        int    recordSize = 64;
        int    offset     = 0;
        int    seqNum     = 0;
        long   timestamp  = 1_700_000_000_000L; // базовый timestamp (мс)

        Random rnd = new Random(SEED);

        while (offset + recordSize <= sizeBytes) {
            // ── Заголовок (32 байта — повторяемая структура) ──

            // Magic bytes — константа
            data[offset]     = (byte) 0xDE;
            data[offset + 1] = (byte) 0xAD;
            data[offset + 2] = (byte) 0xBE;
            data[offset + 3] = (byte) 0xEF;

            // Version — почти всегда 1
            data[offset + 4] = (byte) (rnd.nextInt(20) == 0 ? 2 : 1);

            // Flags — только 3 бита используются реально
            data[offset + 5] = (byte) (rnd.nextInt(8));

            // Packet type — 4 типа из 65536 возможных → повторяемость
            int packetType = rnd.nextInt(4);
            data[offset + 6] = (byte) (packetType & 0xFF);
            data[offset + 7] = 0x00;

            // Source IP: 192.168.[1-5].[1-50] — ограниченный диапазон
            data[offset + 8]  = (byte) 192;
            data[offset + 9]  = (byte) 168;
            data[offset + 10] = (byte) (rnd.nextInt(5) + 1);
            data[offset + 11] = (byte) (rnd.nextInt(50) + 1);

            // Dest IP: 10.0.[0-3].[1-20]
            data[offset + 12] = (byte) 10;
            data[offset + 13] = 0x00;
            data[offset + 14] = (byte) rnd.nextInt(4);
            data[offset + 15] = (byte) (rnd.nextInt(20) + 1);

            // Sequence number (little-endian, монотонный)
            data[offset + 16] = (byte) (seqNum & 0xFF);
            data[offset + 17] = (byte) ((seqNum >> 8) & 0xFF);
            data[offset + 18] = (byte) ((seqNum >> 16) & 0xFF);
            data[offset + 19] = (byte) ((seqNum >> 24) & 0xFF);
            seqNum++;

            // Checksum (случайный)
            data[offset + 20] = (byte) rnd.nextInt(256);
            data[offset + 21] = (byte) rnd.nextInt(256);

            // Reserved — всегда нули
            data[offset + 22] = 0x00;
            data[offset + 23] = 0x00;

            // Timestamp (монотонно растёт на 10–100 мс)
            timestamp += 10 + rnd.nextInt(90);
            data[offset + 24] = (byte) (timestamp & 0xFF);
            data[offset + 25] = (byte) ((timestamp >> 8)  & 0xFF);
            data[offset + 26] = (byte) ((timestamp >> 16) & 0xFF);
            data[offset + 27] = (byte) ((timestamp >> 24) & 0xFF);
            data[offset + 28] = (byte) ((timestamp >> 32) & 0xFF);
            data[offset + 29] = (byte) ((timestamp >> 40) & 0xFF);
            data[offset + 30] = 0x00;
            data[offset + 31] = 0x00;

            // ── Payload (32 байта случайных данных) ──
            for (int i = 32; i < recordSize; i++) {
                data[offset + i] = (byte) rnd.nextInt(256);
            }

            offset += recordSize;
        }

        // Дополняем хвост нулями (предсказуемы → сжимаются хорошо)
        Arrays.fill(data, offset, sizeBytes, (byte) 0x00);

        return data;
    }

    // ── Вспомогательные методы ────────────────────────────────────────────

    /**
     * Приводит массив к целевому размеру.
     * Больше targetSize → обрезаем.
     * Меньше targetSize → повторяем содержимое циклически.
     */
    private byte[] adjustToTargetSize(byte[] source, int targetSize) {
        if (source.length == targetSize) return source;
        if (source.length > targetSize)  return Arrays.copyOf(source, targetSize);

        byte[] result = new byte[targetSize];
        int copied = 0;
        while (copied < targetSize) {
            int toCopy = Math.min(source.length, targetSize - copied);
            System.arraycopy(source, 0, result, copied, toCopy);
            copied += toCopy;
        }
        return result;
    }

    private void validateSize(long sizeBytes) {
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException(
                    "Размер данных должен быть > 0, получено: " + sizeBytes
            );
        }
        if (sizeBytes > 512L * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "Размер данных превышает лимит 512 МБ: " + sizeBytes
            );
        }
    }

    // ── Исключение ────────────────────────────────────────────────────────

    public static class TestDataGeneratorException extends RuntimeException {
        public TestDataGeneratorException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}