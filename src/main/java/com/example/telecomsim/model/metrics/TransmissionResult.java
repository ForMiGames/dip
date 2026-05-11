package com.example.telecomsim.model.metrics;

import com.example.telecomsim.model.channel.ChannelParameters;
import com.example.telecomsim.model.simulation.TransmissionProtocol;


public class TransmissionResult {
    private final ChannelParameters channelParameters;
    private final TransmissionProtocol protocol;

    // Объём данных
    private final long dataSentBytes;
    private final long dataReceivedBytes;

    // Временны́е метрики
    private final long transmissionTimeMs;
    private final long averageLatencyMs;

    // Метрики производительности
    private final double effectiveThroughputMbps;

    // Статистика пакетов
    private final int totalPackets;
    private final int deliveredPackets;
    private final int lostPackets;
    private final int corruptedPackets;
    private final int retransmittedPackets;

    // Метрики ошибок
    private final long totalBitErrors;
    private final double actualBitErrorRate;

    private TransmissionResult(Builder builder) {
        this.channelParameters    = builder.channelParameters;
        this.protocol             = builder.protocol;
        this.dataSentBytes        = builder.dataSentBytes;
        this.dataReceivedBytes    = builder.dataReceivedBytes;
        this.transmissionTimeMs   = builder.transmissionTimeMs;
        this.averageLatencyMs     = builder.averageLatencyMs;
        this.effectiveThroughputMbps = builder.effectiveThroughputMbps;
        this.totalPackets         = builder.totalPackets;
        this.deliveredPackets     = builder.deliveredPackets;
        this.lostPackets          = builder.lostPackets;
        this.corruptedPackets     = builder.corruptedPackets;
        this.retransmittedPackets = builder.retransmittedPackets;
        this.totalBitErrors       = builder.totalBitErrors;
        this.actualBitErrorRate   = builder.actualBitErrorRate;
    }

    // ------------------------------------------------------------------ //
    //  Вычисляемые свойства                                               //
    // ------------------------------------------------------------------ //

    /** Процент успешно доставленных пакетов. */
    public double getDeliveryRatePercent() {
        if (totalPackets == 0) return 100.0;
        return (double) (deliveredPackets + retransmittedPackets) / totalPackets * 100.0;
    }

    /** Процент потерянных пакетов. */
    public double getPacketLossPercent() {
        if (totalPackets == 0) return 0.0;
        return (double) lostPackets / totalPackets * 100.0;
    }

    /** КПД канала: отношение полученных данных к отправленным. */
    public double getChannelEfficiencyPercent() {
        if (dataSentBytes == 0) return 0.0;
        return (double) dataReceivedBytes / dataSentBytes * 100.0;
    }

    // Геттеры
    public ChannelParameters getChannelParameters()    { return channelParameters; }
    public TransmissionProtocol getProtocol()          { return protocol; }
    public long getDataSentBytes()                     { return dataSentBytes; }
    public long getDataReceivedBytes()                 { return dataReceivedBytes; }
    public long getTransmissionTimeMs()                { return transmissionTimeMs; }
    public long getAverageLatencyMs()                  { return averageLatencyMs; }
    public double getEffectiveThroughputMbps()         { return effectiveThroughputMbps; }
    public int getTotalPackets()                       { return totalPackets; }
    public int getDeliveredPackets()                   { return deliveredPackets; }
    public int getLostPackets()                        { return lostPackets; }
    public int getCorruptedPackets()                   { return corruptedPackets; }
    public int getRetransmittedPackets()               { return retransmittedPackets; }
    public long getTotalBitErrors()                    { return totalBitErrors; }
    public double getActualBitErrorRate()              { return actualBitErrorRate; }

    // ------------------------------------------------------------------ //
    //  Builder                                                             //
    // ------------------------------------------------------------------ //

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ChannelParameters channelParameters;
        private TransmissionProtocol protocol;
        private long dataSentBytes;
        private long dataReceivedBytes;
        private long transmissionTimeMs;
        private long averageLatencyMs;
        private double effectiveThroughputMbps;
        private int totalPackets;
        private int deliveredPackets;
        private int lostPackets;
        private int corruptedPackets;
        private int retransmittedPackets;
        private long totalBitErrors;
        private double actualBitErrorRate;

        public Builder channelParameters(ChannelParameters v)      { this.channelParameters = v; return this; }
        public Builder protocol(TransmissionProtocol v)            { this.protocol = v; return this; }
        public Builder dataSentBytes(long v)                       { this.dataSentBytes = v; return this; }
        public Builder dataReceivedBytes(long v)                   { this.dataReceivedBytes = v; return this; }
        public Builder transmissionTimeMs(long v)                  { this.transmissionTimeMs = v; return this; }
        public Builder averageLatencyMs(long v)                    { this.averageLatencyMs = v; return this; }
        public Builder effectiveThroughputMbps(double v)           { this.effectiveThroughputMbps = v; return this; }
        public Builder totalPackets(int v)                         { this.totalPackets = v; return this; }
        public Builder deliveredPackets(int v)                     { this.deliveredPackets = v; return this; }
        public Builder lostPackets(int v)                          { this.lostPackets = v; return this; }
        public Builder corruptedPackets(int v)                     { this.corruptedPackets = v; return this; }
        public Builder retransmittedPackets(int v)                 { this.retransmittedPackets = v; return this; }
        public Builder totalBitErrors(long v)                      { this.totalBitErrors = v; return this; }
        public Builder actualBitErrorRate(double v)                { this.actualBitErrorRate = v; return this; }

        public TransmissionResult build() {
            return new TransmissionResult(this);
        }
    }
}
