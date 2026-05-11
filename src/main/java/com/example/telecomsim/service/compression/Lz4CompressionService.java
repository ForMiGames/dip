package com.example.telecomsim.service.compression;

import com.example.telecomsim.model.compression.CompressionAlgorithm;
import com.example.telecomsim.model.compression.CompressionSettings;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.nio.ByteBuffer;
import java.util.Map;

public class Lz4CompressionService  extends AbstractCompressionService {
    private static final LZ4Factory FACTORY = LZ4Factory.fastestInstance();

    @Override
    public CompressionAlgorithm getAlgorithm() {
        return CompressionAlgorithm.LZ4;
    }

    @Override
    public byte[] compress(byte[] data, CompressionSettings settings) {
        validateInput(data, "сжатие");

        LZ4Compressor compressor = resolveCompressor(settings);

        int maxCompressedLength = compressor.maxCompressedLength(data.length);
        byte[] compressed = new byte[maxCompressedLength];
        int compressedLength = compressor.compress(data, 0, data.length, compressed, 0, maxCompressedLength);

        // Записываем исходный размер + фактически сжатые байты
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + compressedLength);
        buffer.putInt(data.length);
        buffer.put(compressed, 0, compressedLength);
        return buffer.array();
    }

    @Override
    public byte[] decompress(byte[] data, CompressionSettings settings) {
        validateInput(data, "распаковка");

        ByteBuffer buffer = ByteBuffer.wrap(data);
        int originalSize = buffer.getInt();

        if (originalSize <= 0) {
            throw new CompressionException(
                    getAlgorithm(),
                    "Повреждённые данные: некорректный исходный размер в заголовке: " + originalSize
            );
        }

        byte[] compressedData = new byte[buffer.remaining()];
        buffer.get(compressedData);

        LZ4FastDecompressor decompressor = FACTORY.fastDecompressor();
        byte[] restored = new byte[originalSize];
        try {
            decompressor.decompress(compressedData, 0, restored, 0, originalSize);
        } catch (Exception e) {
            throw new CompressionException(getAlgorithm(), "Ошибка LZ4-распаковки", e);
        }
        return restored;
    }

    @Override
    public String getDescription() {
        return """
               LZ4 — экстремально быстрый алгоритм сжатия без потерь.
               Приоритет — скорость: декомпрессия достигает скорости чтения ОЗУ.
               Степень сжатия уступает DEFLATE и Zstd, но скорость несравнима.
               Применяется в системах реального времени, базах данных (ClickHouse, Kafka).
               High Compression режим (HC) даёт лучшее сжатие ценой скорости кодирования.
               """;
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of(
                "compressionLevel",
                "0 — стандартный быстрый режим; 1–17 — High Compression (HC), лучше сжатие, медленнее."
        );
    }

    // ------------------------------------------------------------------ //

    private LZ4Compressor resolveCompressor(CompressionSettings settings) {
        if (settings != null && settings.getCompressionLevel() > 0) {
            return FACTORY.highCompressor(settings.getCompressionLevel());
        }
        return FACTORY.fastCompressor();
    }
}
