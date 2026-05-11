package com.example.telecomsim.service.simulation;

/**
 * Статус пакета после прохождения через симулируемый канал.
 */
public enum PacketStatus {
    DELIVERED("Доставлен"),
    LOST("Потерян"),
    CORRUPTED("Повреждён (BER)"),
    RETRANSMITTED("Повторно передан");

    private final String displayName;

    PacketStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
