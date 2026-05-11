package com.example.telecomsim.service.compression;

import com.example.telecomsim.model.compression.CompressionAlgorithm;
import com.example.telecomsim.model.compression.CompressionSettings;

import java.util.Map;


//COMPLETED
public class NoCompressionService  implements CompressionAlgorithmService {
    @Override
    public CompressionAlgorithm getAlgorithm() {
        return CompressionAlgorithm.NONE;
    }

    @Override
    public byte[] compress(byte[] data, CompressionSettings settings) {
        // Без сжатия возвращаем копию исходных данных (чтобы не изменять исходный массив)
        return data.clone();
    }

    @Override
    public byte[] decompress(byte[] data, CompressionSettings settings) {
        // Данные не были сжаты, просто возвращаем копию
        return data.clone();
    }

    @Override
    public String getDescription() {
        return "No compression algorithm (passthrough) – returns original data without any transformation.";
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        // У алгоритма нет настраиваемых параметров
        return Map.of(); // или Collections.emptyMap()
    }
}
