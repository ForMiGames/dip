package com.example.telecomsim.service.simulation;

/**
 * Пакет после прохождения через симулируемый канал —
 * содержит исходный пакет и результаты воздействия канала.
 */
public class SimulatedPacket {
    private final Packet originalPacket;
    private final PacketStatus status;
    private final byte[] receivedPayload;    // null если пакет потерян
    private final long actualLatencyMs;      // Фактическая задержка с джиттером
    private final int bitErrorsIntroduced;   // Количество внесённых битовых ошибок
    private final int retransmissionCount;   // Сколько раз пакет перепередавался

    private SimulatedPacket(Builder builder) {
        this.originalPacket       = builder.originalPacket;
        this.status               = builder.status;
        this.receivedPayload      = builder.receivedPayload;
        this.actualLatencyMs      = builder.actualLatencyMs;
        this.bitErrorsIntroduced  = builder.bitErrorsIntroduced;
        this.retransmissionCount  = builder.retransmissionCount;
    }

    public Packet getOriginalPacket()       { return originalPacket; }
    public PacketStatus getStatus()         { return status; }
    public byte[] getReceivedPayload()      { return receivedPayload != null ? receivedPayload.clone() : null; }
    public long getActualLatencyMs()        { return actualLatencyMs; }
    public int getBitErrorsIntroduced()     { return bitErrorsIntroduced; }
    public int getRetransmissionCount()     { return retransmissionCount; }
    public boolean isDeliveredSuccessfully(){ return status == PacketStatus.DELIVERED || status == PacketStatus.RETRANSMITTED; }

    // ------------------------------------------------------------------ //
    //  Builder                                                             //
    // ------------------------------------------------------------------ //

    public static Builder builder(Packet originalPacket) {
        return new Builder(originalPacket);
    }

    public static class Builder {
        private final Packet originalPacket;
        private PacketStatus status         = PacketStatus.DELIVERED;
        private byte[] receivedPayload;
        private long actualLatencyMs        = 0;
        private int bitErrorsIntroduced     = 0;
        private int retransmissionCount     = 0;

        private Builder(Packet originalPacket) {
            this.originalPacket  = originalPacket;
            this.receivedPayload = originalPacket.getPayload();
        }

        public Builder status(PacketStatus status)                      { this.status = status; return this; }
        public Builder receivedPayload(byte[] payload)                  { this.receivedPayload = payload; return this; }
        public Builder actualLatencyMs(long latencyMs)                  { this.actualLatencyMs = latencyMs; return this; }
        public Builder bitErrorsIntroduced(int errors)                  { this.bitErrorsIntroduced = errors; return this; }
        public Builder retransmissionCount(int count)                   { this.retransmissionCount = count; return this; }

        public SimulatedPacket build() {
            return new SimulatedPacket(this);
        }
    }
}
