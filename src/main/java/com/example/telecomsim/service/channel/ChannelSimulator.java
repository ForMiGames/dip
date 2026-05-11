package com.example.telecomsim.service.channel;

import com.example.telecomsim.model.channel.ChannelParameters;
import com.example.telecomsim.model.metrics.TransmissionResult;
import com.example.telecomsim.model.simulation.TransmissionProtocol;


/**
 * Симулятор канала передачи данных.
 * Принимает массив байт (сжатые данные), разбивает на пакеты,
 * применяет характеристики канала (BER, задержку, потери),
 * возвращает статистику передачи.
 */
public interface ChannelSimulator {
    /**
     * Симулирует передачу данных по каналу.
     *
     * @param data     сжатые данные для передачи
     * @param params   параметры канала (BER, bandwidth, latency, ...)
     * @param protocol протокол передачи (TCP / UDP)
     * @return результаты симуляции канала
     */
    TransmissionResult simulate(byte[] data,
                                ChannelParameters params,
                                TransmissionProtocol protocol);
}
