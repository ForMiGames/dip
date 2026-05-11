package com.example.telecomsim.service.simulation;

import com.example.telecomsim.model.compression.CompressionAlgorithm;
import com.example.telecomsim.service.compression.*;

public class CompressionServiceFactory {
    public static CompressionAlgorithmService create(CompressionAlgorithm algorithm) {
        return switch (algorithm) {
            case HUFFMAN -> new HuffmanCompressionService();
            case LZ77 -> new Lz77CompressionService();
            case DEFLATE -> new DeflateCompressionService();  // Java-стандарт (java.util.zip)
            case ZSTANDARD -> new ZstandardCompressionService();  // Требуется zstd-jni
            case LZ4 -> new Lz4CompressionService();              // Требуется lz4-java
            case SNAPPY -> new SnappyCompressionService();        // Требуется snappy-java
            case NONE -> new NoCompressionService();  // заглушка (возвращает исходные данные)
        };
    }
}
