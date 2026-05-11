package com.example.telecomsim.model.channel;

public enum ChannelType {
    FIBER_OPTIC("Оптоволокно"),
    ETHERNET("Ethernet (медь)"),
    WIFI("Wi-Fi"),
    LTE_4G("4G/LTE"),
    MOBILE_5G("5G"),
    SATELLITE("Спутниковый канал"),
    DSL("DSL");

    private final String displayName;

    ChannelType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
