package com.example.telecomsim.model.compression;

public enum CompressionType {
    LOSSLESS("Без потерь"),
    LOSSY("С потерями"),
    NONE("Без сжатия");

    CompressionType(String displayName) {
        this.displayName = displayName;
    }
    public final String displayName;
}
