package com.example.telecomsim.service.compression;

import com.example.telecomsim.model.compression.CompressionAlgorithm;

public class CompressionException  extends RuntimeException {
    private final CompressionAlgorithm algorithm;

    public CompressionException(CompressionAlgorithm algorithm,
                                String message,
                                Throwable cause) {
        super("[%s] %s".formatted(algorithm.getDisplayName(), message), cause);
        this.algorithm = algorithm;
    }

    public CompressionException(CompressionAlgorithm algorithm, String message) {
        this(algorithm, message, null);
    }

    public CompressionAlgorithm getAlgorithm() {
        return algorithm;
    }
}
