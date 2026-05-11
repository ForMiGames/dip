package com.example.telecomsim.model.compression;

public enum CompressionAlgorithm {
    HUFFMAN("Хаффман", CompressionType.LOSSLESS),
    LZ77("LZ77", CompressionType.LOSSLESS),
    DEFLATE("DEFLATE (GZIP/ZIP)", CompressionType.LOSSLESS),

    // Современные алгоритмы высокой производительности
    ZSTANDARD("Zstandard (Zstd)", CompressionType.LOSSLESS),
    LZ4("LZ4", CompressionType.LOSSLESS),
    SNAPPY("Snappy (Google)", CompressionType.LOSSLESS),
    NONE("Без сжатия", CompressionType.NONE);

    private final String displayName;
    private final CompressionType type;

    // Конструктор enum (всегда private или package-private, public не разрешён)
    CompressionAlgorithm(String displayName, CompressionType type) {
        this.displayName = displayName;
        this.type = type;
    }

    // Геттеры (опционально, но обычно нужны)
    public String getDisplayName() {
        return displayName;
    }

    public CompressionType getType() {
        return type;
    }
}
