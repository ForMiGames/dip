package com.example.telecomsim.service.simulation;

/**
 * Пакет данных, сформированный перед отправкой по каналу.
 */
public class Packet {
    private final int sequenceNumber;   // Порядковый номер пакета
    private final byte[] payload;       // Полезная нагрузка (данные)
    private final int payloadSize;      // Размер полезной нагрузки (байт)
    private final long createdAtMs;     // Метка времени создания

    public Packet(int sequenceNumber, byte[] payload) {
        this.sequenceNumber = sequenceNumber;
        this.payload        = payload.clone();
        this.payloadSize    = payload.length;
        this.createdAtMs    = System.currentTimeMillis();
    }

    public int getSequenceNumber()  { return sequenceNumber; }
    public byte[] getPayload()      { return payload.clone(); }
    public int getPayloadSize()     { return payloadSize; }
    public long getCreatedAtMs()    { return createdAtMs; }
}
