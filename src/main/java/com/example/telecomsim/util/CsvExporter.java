package com.example.telecomsim.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Утилита для экспорта данных в CSV-формат.
 *
 * Решает проблему кракозябров в Excel:
 *  — Записывает UTF-8 BOM (EF BB BF) в начало файла
 *  — Excel автоматически определяет кодировку по BOM
 *  — Использует ; как разделитель (стандарт для русской локали)
 *  — Заменяет . на , в числах (русский числовой формат Excel)
 *  — Экранирует поля, содержащие ; или кавычки
 */
public final class CsvExporter {

    /**
     * Разделитель колонок.
     * Точка с запятой — стандарт для русской локали Excel.
     * В английской локали используется запятая — но тогда
     * числа с запятой нужно брать в кавычки.
     */
    public static final String DELIMITER = ";";

    /**
     * UTF-8 BOM — три байта в начале файла.
     * Excel использует BOM для определения кодировки UTF-8.
     */
    private static final byte[] UTF8_BOM = {
            (byte) 0xEF, (byte) 0xBB, (byte) 0xBF
    };

    private CsvExporter() {}

    // ── Публичный API ─────────────────────────────────────────────────────

    /**
     * Записывает CSV-файл с UTF-8 BOM.
     *
     * @param file    файл для записи
     * @param content готовое содержимое CSV (строки уже сформированы)
     */
    public static void write(File file, String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // BOM первым делом
            fos.write(UTF8_BOM);

            // Затем содержимое в UTF-8
            try (Writer writer = new BufferedWriter(
                    new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
                writer.write(content);
            }
        }
    }

    /**
     * Формирует строку CSV из массива значений.
     * Числа с плавающей точкой — заменяем . на , (русский формат).
     * Поля со спецсимволами — берём в кавычки.
     *
     * @param values значения колонок в порядке следования
     * @return строка CSV, оканчивающаяся на \n
     */
    public static String buildRow(Object... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(DELIMITER);
            sb.append(formatValue(values[i]));
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Формирует строку-заголовок CSV.
     *
     * @param headers названия колонок
     * @return строка заголовка, оканчивающаяся на \n
     */
    public static String buildHeader(String... headers) {
        return String.join(DELIMITER, headers) + "\n";
    }

    // ── Форматирование значений ───────────────────────────────────────────

    /**
     * Форматирует одно значение для CSV:
     *  — double/float: заменяем . на , (русский числовой формат)
     *  — String с ; или ": берём в кавычки, внутренние " удваиваем
     *  — null: пустая строка
     */
    public static String formatValue(Object value) {
        if (value == null) return "";

        if (value instanceof Double d) {
            return formatDouble(d);
        }
        if (value instanceof Float f) {
            return formatDouble(f.doubleValue());
        }

        String str = value.toString();
        return escapeField(str);
    }

    /**
     * Форматирует double для русской локали Excel:
     * заменяет разделитель дробной части . на ,
     */
    public static String formatDouble(double value) {
        // Бесконечность и NaN — пустая строка
        if (Double.isInfinite(value) || Double.isNaN(value)) return "";
        return String.format("%.4f", value).replace(".", ",");
    }

    /**
     * Форматирует double с заданной точностью.
     */
    public static String formatDouble(double value, int decimals) {
        if (Double.isInfinite(value) || Double.isNaN(value)) return "";
        return String.format("%." + decimals + "f", value).replace(".", ",");
    }

    /**
     * Экранирует строковое поле CSV:
     *  — если содержит ; или " или перенос строки — берём в кавычки
     *  — внутренние " удваиваем (стандарт RFC 4180)
     */
    public static String escapeField(String value) {
        if (value == null) return "";
        if (value.contains(DELIMITER) || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}