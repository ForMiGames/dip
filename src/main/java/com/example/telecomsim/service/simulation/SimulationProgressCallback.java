package com.example.telecomsim.service.simulation;

import com.example.telecomsim.model.metrics.SimulationResult;
import com.example.telecomsim.model.metrics.SimulationSession;

public interface SimulationProgressCallback {
    /**
     * Вызывается при изменении общего прогресса симуляции.
     *
     * @param progressFraction прогресс от 0.0 до 1.0
     * @param stepDescription  описание текущего шага (напр. "Сжатие: Huffman...")
     */
    void onProgress(double progressFraction, String stepDescription);

    /**
     * Вызывается после завершения обработки одного алгоритма.
     *
     * @param result результат симуляции для конкретного алгоритма
     */
    void onAlgorithmCompleted(SimulationResult result);

    /**
     * Вызывается после успешного завершения всей сессии симуляции.
     *
     * @param session итоговая сессия со всеми результатами
     */
    void onCompleted(SimulationSession session);

    /**
     * Вызывается при возникновении ошибки.
     *
     * @param exception исключение с описанием причины
     */
    void onError(Exception exception);

    /**
     * Вызывается при отмене симуляции пользователем.
     */
    void onCancelled();
}
