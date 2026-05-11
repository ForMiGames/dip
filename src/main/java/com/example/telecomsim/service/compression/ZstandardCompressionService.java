package com.example.telecomsim.service.compression;

import com.example.telecomsim.model.compression.CompressionAlgorithm;
import com.example.telecomsim.model.compression.CompressionSettings;
import com.github.luben.zstd.Zstd;

import java.util.Map;

public class ZstandardCompressionService extends AbstractCompressionService {
    private static final int DEFAULT_LEVEL = 3; // Баланс скорость/степень сжатия

    @Override
    public CompressionAlgorithm getAlgorithm() {
        return CompressionAlgorithm.ZSTANDARD;
    }

    @Override
    public byte[] compress(byte[] data, CompressionSettings settings) {
        validateInput(data, "сжатие");

        int level = resolveCompressionLevel(settings);
        try {
            return Zstd.compress(data, level);
        } catch (Exception e) {
            throw new CompressionException(getAlgorithm(), "Ошибка Zstd-сжатия", e);
        }
    }

    @Override
    public byte[] decompress(byte[] data, CompressionSettings settings) {
        validateInput(data, "распаковка");

        try {
            // Zstd хранит размер оригинала в заголовке — используем для выделения буфера
            long originalSize = Zstd.decompressedSize(data);
            if (originalSize <= 0) {
                throw new CompressionException(
                        getAlgorithm(), "Не удалось определить исходный размер из заголовка Zstd"
                );
            }
            return Zstd.decompress(data, (int) originalSize);
        } catch (CompressionException e) {
            throw e;
        } catch (Exception e) {
            throw new CompressionException(getAlgorithm(), "Ошибка Zstd-распаковки", e);
        }
    }

    @Override
    public String getDescription() {
        return """
               Zstandard (Zstd) — современный алгоритм сжатия без потерь от Facebook (Meta).
               Обеспечивает скорость сжатия, сравнимую с LZ4, при степени сжатия уровня DEFLATE.
               Поддерживает широкий диапазон уровней: от ультрабыстрого (-5) до максимального (22).
               Применяется в ядре Linux, Facebook, Hadoop и других высоконагруженных системах.
               """;
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of(
                "compressionLevel",
                "Уровень сжатия от 1 до 22. Отрицательные значения (-5..0) — ультрабыстрый режим. По умолчанию: 3."
        );
    }

    // ------------------------------------------------------------------ //

    private int resolveCompressionLevel(CompressionSettings settings) {
        if (settings == null || settings.getCompressionLevel() == 0) {
            return DEFAULT_LEVEL;
        }
        return settings.getCompressionLevel();
    }
}
