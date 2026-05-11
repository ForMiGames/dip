package com.example.telecomsim.service.compression;

import com.example.telecomsim.model.compression.CompressionAlgorithm;
import com.example.telecomsim.model.compression.CompressionSettings;

import java.io.*;
import java.util.Map;

/**
 * Сжатие методом LZ77 (Lempel–Ziv 1977).
 *
 * Формат токена (5 байт):
 *   [offset:  2 байта unsigned short] — смещение назад в окне (0 = нет совпадения)
 *   [length:  2 байта unsigned short] — длина совпадения     (0 = нет совпадения)
 *   [literal: 1 байт               ] — следующий символ после совпадения
 *
 * Крайний случай конца данных:
 *   Если данные заканчиваются ровно на совпадении (нет следующего символа),
 *   literal записывается как 0x00, а при декомпрессии он игнорируется
 *   благодаря счётчику originalLength в заголовке.
 *
 * Заголовок (4 байта в начале сжатого потока):
 *   [originalLength: 4 байта int] — исходный размер данных.
 *   Нужен декомпрессору, чтобы точно знать, сколько байт восстанавливать,
 *   и не записывать лишний literal 0x00 в конце.
 */
public class Lz77CompressionService extends AbstractCompressionService {

    private static final int DEFAULT_WINDOW_SIZE    = 4096;
    private static final int DEFAULT_LOOKAHEAD_SIZE = 18;
    private static final int TOKEN_SIZE_BYTES       = 5;    // offset(2) + length(2) + literal(1)
    private static final int HEADER_SIZE_BYTES      = 4;    // originalLength (int)

    @Override
    public CompressionAlgorithm getAlgorithm() {
        return CompressionAlgorithm.LZ77;
    }

    // ── Сжатие ───────────────────────────────────────────────────────────

    @Override
    public byte[] compress(byte[] data, CompressionSettings settings) {
        validateInput(data, "сжатие");

        int windowSize    = resolveWindowSize(settings);
        int lookaheadSize = resolveLookaheadSize(settings);

        ByteArrayOutputStream output = new ByteArrayOutputStream(
                HEADER_SIZE_BYTES + (data.length / 2) * TOKEN_SIZE_BYTES
        );
        DataOutputStream out = new DataOutputStream(output);

        try {
            // Заголовок: исходный размер — нужен декомпрессору
            out.writeInt(data.length);

            int pos = 0;
            while (pos < data.length) {
                int windowStart = Math.max(0, pos - windowSize);

                int bestOffset = 0;
                int bestLength = 0;

                // Ищем наибольшее совпадение в скользящем окне
                for (int i = windowStart; i < pos; i++) {
                    int matchLength = 0;

                    while (matchLength < lookaheadSize
                            && (pos + matchLength) < data.length
                            /*
                             * ИСПРАВЛЕНИЕ БАГА 1:
                             * Добавлена проверка (i + matchLength) < data.length.
                             * Без неё при i близком к pos и большом matchLength
                             * индекс data[i + matchLength] выходил за границу массива.
                             * Пример: pos=46821, i=46820, matchLength=18 → i+18=46838 > length
                             */
                            && (i + matchLength) < data.length
                            && data[i + matchLength] == data[pos + matchLength]) {
                        matchLength++;
                    }

                    if (matchLength > bestLength) {
                        bestLength = matchLength;
                        bestOffset = pos - i;
                    }
                }

                // Следующий literal-символ после совпадения
                byte literal = (pos + bestLength < data.length)
                        ? data[pos + bestLength]
                        : 0x00; // конец данных — literal не будет использован

                out.writeShort(bestOffset);
                out.writeShort(bestLength);
                out.writeByte(literal);

                pos += bestLength + 1;
            }

        } catch (IOException e) {
            throw new CompressionException(getAlgorithm(), "Ошибка записи при сжатии", e);
        }

        return output.toByteArray();
    }

    // ── Декомпрессия ─────────────────────────────────────────────────────

    @Override
    public byte[] decompress(byte[] data, CompressionSettings settings) {
        validateInput(data, "распаковка");

        if (data.length < HEADER_SIZE_BYTES) {
            throw new CompressionException(
                    getAlgorithm(),
                    "Повреждённые данные: недостаточно байт для заголовка (минимум %d байт)"
                            .formatted(HEADER_SIZE_BYTES)
            );
        }

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));

        try {
            // Читаем заголовок — исходный размер данных
            int originalLength = in.readInt();

            if (originalLength < 0) {
                throw new CompressionException(
                        getAlgorithm(),
                        "Повреждённые данные: некорректный исходный размер: " + originalLength
                );
            }

            /*
             * ИСПРАВЛЕНИЕ БАГА 2:
             * Вместо ByteArrayOutputStream используем byte[] фиксированного размера.
             * Это позволяет корректно обрабатывать ПЕРЕКРЫВАЮЩИЕСЯ совпадения (run-length),
             * когда offset < length — классический случай для LZ77.
             *
             * Пример перекрывающегося совпадения:
             *   Буфер: [A, B, C]
             *   Токен: offset=3, length=6 → нужно скопировать ABCABC
             *   С ByteArrayOutputStream + toByteArray() снимок делается один раз
             *   и не содержит только что записанных байт → результат неверный.
             *   С прямым доступом к массиву каждый байт читается по мере записи → верно.
             */
            byte[] result  = new byte[originalLength];
            int    writePos = 0;

            while (in.available() > 0 && writePos < originalLength) {
                int  offset  = in.readUnsignedShort();
                int  length  = in.readUnsignedShort();
                byte literal = in.readByte();

                if (offset == 0 && length == 0) {
                    // Литеральный токен — просто записываем символ
                    if (writePos < originalLength) {
                        result[writePos++] = literal;
                    }
                } else {
                    // Токен со ссылкой — копируем из уже декодированной части
                    int startPos = writePos - offset;

                    if (startPos < 0) {
                        throw new CompressionException(
                                getAlgorithm(),
                                ("Повреждённые данные: смещение %d больше " +
                                        "размера декодированного буфера %d").formatted(offset, writePos)
                        );
                    }

                    /*
                     * Копируем побайтово — это намеренно.
                     * При перекрывающихся ссылках (offset < length) каждый
                     * только что записанный байт сразу участвует в следующей итерации.
                     * System.arraycopy здесь дал бы неверный результат.
                     */
                    for (int i = 0; i < length && writePos < originalLength; i++) {
                        result[writePos] = result[startPos + i];
                        writePos++;
                    }

                    // Записываем literal (если не вышли за пределы)
                    if (writePos < originalLength) {
                        result[writePos++] = literal;
                    }
                }
            }

            // Проверка: восстановлено ровно столько байт, сколько ожидалось
            if (writePos != originalLength) {
                throw new CompressionException(
                        getAlgorithm(),
                        "Восстановлено %d байт, ожидалось %d — данные повреждены"
                                .formatted(writePos, originalLength)
                );
            }

            return result;

        } catch (CompressionException e) {
            throw e;
        } catch (IOException e) {
            throw new CompressionException(getAlgorithm(), "Ошибка чтения при распаковке", e);
        }
    }

    // ── Метаданные ────────────────────────────────────────────────────────

    @Override
    public String getDescription() {
        return """
               LZ77 (Lempel–Ziv 1977) — словарный алгоритм сжатия без потерь.
               Использует скользящее окно для поиска повторяющихся последовательностей
               и кодирует их ссылками (смещение + длина), а не копиями.
               Является основой для DEFLATE, ZIP, PNG и многих других форматов.
               """;
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of(
                "windowSize",
                "Размер скользящего окна (байт). Больше окно — лучше сжатие, больше памяти.",
                "lookaheadSize",
                "Размер буфера просмотра вперёд (байт). Определяет максимальную длину совпадения."
        );
    }

    // ── Вспомогательные методы ────────────────────────────────────────────

    private int resolveWindowSize(CompressionSettings settings) {
        if (settings == null || settings.getWindowSize() <= 0) {
            return DEFAULT_WINDOW_SIZE;
        }
        return settings.getWindowSize();
    }

    private int resolveLookaheadSize(CompressionSettings settings) {
        if (settings == null || settings.getLookaheadSize() <= 0) {
            return DEFAULT_LOOKAHEAD_SIZE;
        }
        return settings.getLookaheadSize();
    }
}