package com.example.telecomsim.service.simulation;

import com.example.telecomsim.model.metrics.SimulationSession;
import com.example.telecomsim.model.simulation.SimulationConfig;


/**
 * Основной сервис симуляции передачи данных.
 * Оркестрирует: генерацию данных → сжатие → симуляцию канала → сбор метрик.
 */
public interface SimulationService {
    /**
     * Запускает симуляцию синхронно (блокирует поток вызова).
     * Подходит для тестов и CLI-режима.
     *
     * @param config конфигурация симуляции
     * @return сессия с результатами по всем выбранным алгоритмам
     * @throws SimulationException при любой ошибке в процессе симуляции
     */
    SimulationSession runSimulation(SimulationConfig config);

    /**
     * Запускает симуляцию асинхронно в фоновом потоке.
     * Прогресс и результаты сообщаются через callback.
     * Используется из JavaFX UI.
     *
     * @param config   конфигурация симуляции
     * @param callback обработчик событий прогресса
     */
    void runSimulationAsync(SimulationConfig config, SimulationProgressCallback callback);

    /**
     * Запрашивает отмену текущей асинхронной симуляции.
     * Отмена произойдёт на ближайшей контрольной точке.
     */
    void cancelSimulation();

    /**
     * @return true, если симуляция выполняется в данный момент
     */
    boolean isRunning();
}
