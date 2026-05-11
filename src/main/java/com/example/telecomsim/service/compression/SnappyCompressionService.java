package com.example.telecomsim.service.compression;

import com.example.telecomsim.model.compression.CompressionAlgorithm;
import com.example.telecomsim.model.compression.CompressionSettings;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.util.Map;

public class SnappyCompressionService extends AbstractCompressionService {
    @Override
    public CompressionAlgorithm getAlgorithm() {
        return CompressionAlgorithm.SNAPPY;
    }

    @Override
    public byte[] compress(byte[] data, CompressionSettings settings) {
        validateInput(data, "сжатие");
        try {
            return Snappy.compress(data);
        } catch (IOException e) {
            throw new CompressionException(getAlgorithm(), "Ошибка Snappy-сжатия", e);
        }
    }

    @Override
    public byte[] decompress(byte[] data, CompressionSettings settings) {
        validateInput(data, "распаковка");
        try {
            if (!Snappy.isValidCompressedBuffer(data)) {
                throw new CompressionException(
                        getAlgorithm(), "Данные не являются корректным Snappy-потоком"
                );
            }
            return Snappy.uncompress(data);
        } catch (CompressionException e) {
            throw e;
        } catch (IOException e) {
            throw new CompressionException(getAlgorithm(), "Ошибка Snappy-распаковки", e);
        }
    }

    @Override
    public String getDescription() {
        return """
               Snappy — алгоритм сжатия без потерь от Google.
               Разработан для систем, где скорость важнее степени сжатия.
               Не имеет настраиваемых уровней — всегда работает в оптимальном режиме.
               Применяется в Google BigTable, Cassandra, Hadoop и протоколе gRPC.
               """;
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return Map.of(); // Snappy не имеет настраиваемых параметров
    }
}
