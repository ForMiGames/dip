package com.example.telecomsim.service.compression;

import com.example.telecomsim.model.compression.CompressionAlgorithm;
import com.example.telecomsim.model.compression.CompressionSettings;

import java.util.Map;

public interface CompressionAlgorithmService {
    /**
     * @return идентификатор алгоритма
     */
    CompressionAlgorithm getAlgorithm();

    /**
     * Сжимает входные данные согласно настройкам.
     *
     * @param data     исходные данные
     * @param settings параметры сжатия
     * @return сжатые данные
     * @throws CompressionException если сжатие завершилось с ошибкой
     */
    byte[] compress(byte[] data, CompressionSettings settings);

    /**
     * Распаковывает ранее сжатые данные.
     *
     * @param data     сжатые данные
     * @param settings параметры сжатия (те же, что при сжатии)
     * @return восстановленные данные
     * @throws CompressionException если распаковка завершилась с ошибкой
     */
    byte[] decompress(byte[] data, CompressionSettings settings);

    /**
     * @return описание алгоритма на русском языке
     */
    String getDescription();

    /**
     * @return описания настраиваемых параметров алгоритма (имя → описание)
     */
    Map<String, String> getParameterDescriptions();
}
