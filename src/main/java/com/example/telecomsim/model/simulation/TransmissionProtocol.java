package com.example.telecomsim.model.simulation;

public enum TransmissionProtocol {
    TCP("TCP (с подтверждением)"),
    UDP("UDP (без подтверждения)");

    private final String displayName;

    TransmissionProtocol(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
