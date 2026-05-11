package com.example.telecomsim.model.channel;

import lombok.Builder;
import lombok.Getter;

/**
 * Предустановленный профиль канала связи.
 * Содержит готовые параметры для типичных сценариев — пользователь
 * может выбрать пресет и при необходимости скорректировать параметры.
 */
@Getter
@Builder
public class ChannelPreset {
    /** Отображаемое название пресета. */
    private final String name;

    /** Описание сценария использования. */
    private final String description;

    /** Иконка-эмодзи для отображения в списке (опционально). */
    private final String icon;

    /** Готовые параметры канала. */
    private final ChannelParameters parameters;

    /**
     * Признак пользовательского пресета.
     * false — встроенный (нельзя удалить), true — создан пользователем.
     */
    @Builder.Default
    private final boolean custom = false;

    @Override
    public String toString() {
        return icon != null ? icon + "  " + name : name;
    }
}
