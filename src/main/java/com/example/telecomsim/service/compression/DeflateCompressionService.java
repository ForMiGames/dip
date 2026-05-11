package com.example.telecomsim.service.compression;

import com.example.telecomsim.model.compression.CompressionAlgorithm;
import com.example.telecomsim.model.compression.CompressionSettings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class DeflateCompressionService extends AbstractCompressionService {
    @Override
    public CompressionAlgorithm getAlgorithm() {
        return CompressionAlgorithm.DEFLATE;
    }

    @Override
    public byte[] compress(byte[] data, CompressionSettings settings) {
        validateInput(data, "сжатие");

        int level = resolveCompressionLevel(settings);

        ByteArrayOutputStream output = new ByteArrayOutputStream(data.length);
        try (GZIPOutputStream gzip = new GZIPOutputStream(output) {{
            // Устанавливаем уровень сжатия через рефлексию к внутреннему Deflater
            def.setLevel(level);
        }}) {
            gzip.write(data);
        } catch (IOException e) {
            throw new CompressionException(getAlgorithm(), "Ошибка GZIP-сжатия", e);
        }

        return output.toByteArray();
    }

    @Override
    public byte[] decompress(byte[] data, CompressionSettings settings) {
        validateInput(data, "распаковка");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data))) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = gzip.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new CompressionException(getAlgorithm(), "Ошибка GZIP-распаковки", e);
        }

        return output.toByteArray();
    }

    @Override
    public String getDescription() {
        return """
               DEFLATE — комбинированный алгоритм сжатия без потерь (LZ77 + Хаффман).
               Реализован через GZIP-обёртку с CRC-32 контрольной суммой.
               Широко применяется в ZIP-архивах, HTTP-сжатии и формате PNG.
               Уровень сжатия настраивается от 1 (быстро) до 9 (максимальное сжатие).
               """;
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of(
                "compressionLevel",
                "Уровень сжатия от 1 (быстро, слабее сжатие) до 9 (медленно, лучше сжатие). По умолчанию: 6."
        );
    }

    // ------------------------------------------------------------------ //

    private int resolveCompressionLevel(CompressionSettings settings) {
        if (settings == null || settings.getCompressionLevel() <= 0) {
            return Deflater.DEFAULT_COMPRESSION; // = -1, JDK выберет оптимальный
        }
        int level = settings.getCompressionLevel();
        if (level < 1 || level > 9) {
            throw new CompressionException(
                    getAlgorithm(),
                    "Уровень сжатия должен быть от 1 до 9, получено: " + level
            );
        }
        return level;
    }
}
