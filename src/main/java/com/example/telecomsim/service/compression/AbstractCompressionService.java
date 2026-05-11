package com.example.telecomsim.service.compression;

public abstract class AbstractCompressionService  implements CompressionAlgorithmService {
    /**
     * Проверяет, что входные данные не null и не пустые.
     *
     * @param data      проверяемые данные
     * @param operation "сжатие" или "распаковка" — для читаемого сообщения об ошибке
     */
    protected void validateInput(byte[] data, String operation) {
        if (data == null) {
            throw new CompressionException(
                    getAlgorithm(),
                    "Входные данные для операции «%s» не могут быть null".formatted(operation)
            );
        }
        if (data.length == 0) {
            throw new CompressionException(
                    getAlgorithm(),
                    "Входные данные для операции «%s» не могут быть пустыми".formatted(operation)
            );
        }
    }
}
