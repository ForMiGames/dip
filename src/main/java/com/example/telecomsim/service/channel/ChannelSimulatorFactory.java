package com.example.telecomsim.service.channel;

import com.example.telecomsim.model.channel.ChannelType;

import java.util.Objects;

/**
 * Фабрика симуляторов канала.
 * Создаёт единственный универсальный симулятор —
 * расширяемо для добавления специализированных симуляторов под конкретный ChannelType.
 */
public class ChannelSimulatorFactory {
    private ChannelSimulatorFactory() {}

    /**
     * Создаёт симулятор для указанного типа канала.
     * Сейчас используется единая реализация для всех типов —
     * специфика канала задаётся через ChannelParameters.
     */
    public static ChannelSimulator create(ChannelType channelType) {
        Objects.requireNonNull(channelType, "Тип канала не может быть null");
        // Все каналы используют универсальную реализацию.
        // ChannelParameters.channelType определяет типичные значения параметров.
        return new ChannelSimulatorImpl();
    }
}
