package com.example.telecomsim.service.channel;

import com.example.telecomsim.model.channel.ChannelParameters;
import com.example.telecomsim.model.metrics.TransmissionResult;
import com.example.telecomsim.model.simulation.TransmissionProtocol;
import com.example.telecomsim.service.simulation.Packet;
import com.example.telecomsim.service.simulation.PacketStatus;
import com.example.telecomsim.service.simulation.SimulatedPacket;

import java.util.*;

/**
 * Универсальная реализация симулятора канала передачи данных.
 *
 * Модель симуляции:
 *  1. Данные разбиваются на пакеты согласно MTU канала.
 *  2. Каждый пакет проходит через:
 *     а) Проверку потери пакета (packetLossRate)
 *     б) Внесение битовых ошибок (bitErrorRate)
 *     в) Применение задержки с джиттером
 *  3. TCP: потерянные/повреждённые пакеты перепередаются (до MAX_RETRANSMISSIONS).
 *     UDP: потерянные пакеты не восстанавливаются.
 *  4. Собирается статистика по всем пакетам.
 */
public class ChannelSimulatorImpl  implements ChannelSimulator {

    /** Максимальное количество попыток перепередачи одного пакета (TCP). */
    private static final int MAX_RETRANSMISSIONS = 3;

    /** Накладные расходы заголовка пакета (IP + TCP/UDP), байт. */
    private static final int PACKET_HEADER_OVERHEAD_BYTES = 40;

    /** Скорость света в оптоволокне (≈ 2/3 от скорости в вакууме), км/мс. */
    private static final double FIBER_PROPAGATION_SPEED_KM_PER_MS = 200.0;

    private final Random random = new Random();

    // ------------------------------------------------------------------ //
    //  Публичный API                                                       //
    // ------------------------------------------------------------------ //

    @Override
    public TransmissionResult simulate(byte[] data,
                                       ChannelParameters params,
                                       TransmissionProtocol protocol) {
        Objects.requireNonNull(data,     "Данные для передачи не могут быть null");
        Objects.requireNonNull(params,   "Параметры канала не могут быть null");
        Objects.requireNonNull(protocol, "Протокол передачи не может быть null");

        List<Packet> packets = packetize(data, params.getMtuBytes());
        List<SimulatedPacket> simulatedPackets = transmitPackets(packets, params, protocol);

        return buildTransmissionResult(data.length, params, protocol, simulatedPackets);
    }



    // ------------------------------------------------------------------ //
    //  Шаг 1: Разбивка данных на пакеты                                   //
    // ------------------------------------------------------------------ //

    /**
     * Разбивает массив байт на пакеты с учётом MTU и накладных расходов заголовка.
     */
    private List<Packet> packetize(byte[] data, int mtu) {
        int payloadSize = mtu - PACKET_HEADER_OVERHEAD_BYTES;
        if (payloadSize <= 0) {
            payloadSize = mtu; // Если MTU очень мал — игнорируем заголовок
        }

        List<Packet> packets = new ArrayList<>();
        int sequenceNumber = 0;
        int offset = 0;

        while (offset < data.length) {
            int chunkSize = Math.min(payloadSize, data.length - offset);
            byte[] payload = Arrays.copyOfRange(data, offset, offset + chunkSize);
            packets.add(new Packet(sequenceNumber++, payload));
            offset += chunkSize;
        }

        return packets;
    }

    // ------------------------------------------------------------------ //
    //  Шаг 2: Передача пакетов через канал                                //
    // ------------------------------------------------------------------ //

    /**
     * Передаёт все пакеты через симулируемый канал.
     * TCP: автоматически перепередаёт потерянные/повреждённые пакеты.
     * UDP: потери и ошибки не исправляются.
     */
    private List<SimulatedPacket> transmitPackets(List<Packet> packets,
                                                  ChannelParameters params,
                                                  TransmissionProtocol protocol) {
        List<SimulatedPacket> results = new ArrayList<>(packets.size());

        for (Packet packet : packets) {
            SimulatedPacket simulatedPacket = switch (protocol) {
                case TCP -> transmitWithTcp(packet, params);
                case UDP -> transmitWithUdp(packet, params);
            };
            results.add(simulatedPacket);
        }

        return results;
    }

    /**
     * TCP: пытается доставить пакет, при неудаче — перепередаёт.
     */
    private SimulatedPacket transmitWithTcp(Packet packet, ChannelParameters params) {
        int retransmissions = 0;
        long totalLatency   = 0;
        int  totalBitErrors = 0;

        SimulatedPacket result = null;

        while (retransmissions <= MAX_RETRANSMISSIONS) {
            result = applyChannelEffects(packet, params);
            totalLatency   += result.getActualLatencyMs();
            totalBitErrors += result.getBitErrorsIntroduced();

            if (result.isDeliveredSuccessfully()) {
                // Пакет доставлен — если это была перепередача, обновляем статус
                if (retransmissions > 0) {
                    return SimulatedPacket.builder(packet)
                            .status(PacketStatus.RETRANSMITTED)
                            .receivedPayload(result.getReceivedPayload())
                            .actualLatencyMs(totalLatency)
                            .bitErrorsIntroduced(totalBitErrors)
                            .retransmissionCount(retransmissions)
                            .build();
                }
                return result;
            }

            retransmissions++;

            // Добавляем задержку на ACK-таймаут перепередачи (RTT × 1.5)
            totalLatency += calculateRttMs(params) * 3 / 2;
        }

        // Исчерпаны все попытки — пакет окончательно потерян
        return SimulatedPacket.builder(packet)
                .status(PacketStatus.LOST)
                .receivedPayload(null)
                .actualLatencyMs(totalLatency)
                .bitErrorsIntroduced(totalBitErrors)
                .retransmissionCount(retransmissions)
                .build();
    }

    /**
     * UDP: одна попытка, без гарантий доставки.
     */
    private SimulatedPacket transmitWithUdp(Packet packet, ChannelParameters params) {
        return applyChannelEffects(packet, params);
    }

    // ------------------------------------------------------------------ //
    //  Шаг 3: Применение физических эффектов канала к пакету              //
    // ------------------------------------------------------------------ //

    /**
     * Применяет к пакету все физические эффекты канала:
     * потерю пакета → битовые ошибки → задержку.
     */
    private SimulatedPacket applyChannelEffects(Packet packet, ChannelParameters params) {
        // 1. Потеря пакета
        if (isPacketLost(params.getPacketLossRate())) {
            return SimulatedPacket.builder(packet)
                    .status(PacketStatus.LOST)
                    .receivedPayload(null)
                    .actualLatencyMs(calculateLatencyWithJitter(params))
                    .bitErrorsIntroduced(0)
                    .build();
        }

        // 2. Внесение битовых ошибок согласно BER
        byte[] receivedPayload = applyBitErrors(packet.getPayload(), params.getBitErrorRate());
        int bitErrors = countBitDifferences(packet.getPayload(), receivedPayload);

        // 3. Проверка: повреждён ли пакет
        PacketStatus status = (bitErrors > 0) ? PacketStatus.CORRUPTED : PacketStatus.DELIVERED;

        return SimulatedPacket.builder(packet)
                .status(status)
                .receivedPayload(receivedPayload)
                .actualLatencyMs(calculateLatencyWithJitter(params))
                .bitErrorsIntroduced(bitErrors)
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Физические модели                                                   //
    // ------------------------------------------------------------------ //

    /**
     * Определяет, потерян ли пакет, на основе вероятности потерь.
     */
    private boolean isPacketLost(double packetLossRate) {
        return random.nextDouble() < packetLossRate;
    }

    /**
     * Вносит битовые ошибки в данные согласно BER (Bit Error Rate).
     * Каждый бит инвертируется с вероятностью, равной BER.
     *
     * Оптимизация: при очень низком BER (< 10⁻⁹) пропускаем обработку —
     * статистически ни одного бита на типичных размерах пакета не затронет.
     */
    private byte[] applyBitErrors(byte[] data, double bitErrorRate) {
        if (bitErrorRate <= 0 || bitErrorRate < 1e-9) {
            return data.clone();
        }

        byte[] result = data.clone();

        for (int byteIndex = 0; byteIndex < result.length; byteIndex++) {
            for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
                if (random.nextDouble() < bitErrorRate) {
                    // Инвертируем бит
                    result[byteIndex] ^= (1 << bitIndex);
                }
            }
        }

        return result;
    }

    /**
     * Подсчитывает количество различающихся бит между двумя массивами.
     * Расстояние Хэмминга на уровне массивов байт.
     */
    private int countBitDifferences(byte[] original, byte[] received) {
        int differences = 0;
        for (int i = 0; i < Math.min(original.length, received.length); i++) {
            differences += Integer.bitCount((original[i] ^ received[i]) & 0xFF);
        }
        return differences;
    }

    /**
     * Вычисляет задержку пакета с учётом:
     *  - задержки распространения сигнала (зависит от расстояния)
     *  - базовой задержки обработки канала
     *  - случайного джиттера
     */
    private long calculateLatencyWithJitter(ChannelParameters params) {
        long propagationDelay = calculatePropagationDelayMs(params.getDistanceKm());
        long baseLatency      = params.getLatencyMs();
        long jitter           = calculateJitter(params.getJitterMs());

        return propagationDelay + baseLatency + jitter;
    }

    /**
     * Вычисляет задержку распространения сигнала по расстоянию.
     * Используется скорость света в оптоволокне (~200 000 км/с = 200 км/мс).
     */
    private long calculatePropagationDelayMs(double distanceKm) {
        if (distanceKm <= 0) return 0;
        return Math.round(distanceKm / FIBER_PROPAGATION_SPEED_KM_PER_MS);
    }

    /**
     * Генерирует случайный джиттер в диапазоне [-jitterMs; +jitterMs].
     * Использует гауссово распределение для более реалистичной симуляции.
     */
    private long calculateJitter(long jitterMs) {
        if (jitterMs <= 0) return 0;
        // Гауссов джиттер: σ = jitterMs/3, чтобы 99.7% значений попадали в ±jitterMs
        double gaussian = random.nextGaussian() * (jitterMs / 3.0);
        return (long) Math.max(-jitterMs, Math.min(jitterMs, gaussian));
    }

    /**
     * Вычисляет RTT (Round-Trip Time) для TCP-таймаутов перепередачи.
     * RTT ≈ 2 × задержка в одну сторону.
     */
    private long calculateRttMs(ChannelParameters params) {
        return (params.getLatencyMs() + calculatePropagationDelayMs(params.getDistanceKm())) * 2;
    }

    // ------------------------------------------------------------------ //
    //  Шаг 4: Сборка результатов                                           //
    // ------------------------------------------------------------------ //

    /**
     * Агрегирует результаты всех симулированных пакетов в TransmissionResult.
     */
    private TransmissionResult buildTransmissionResult(int originalDataSize,
                                                       ChannelParameters params,
                                                       TransmissionProtocol protocol,
                                                       List<SimulatedPacket> simulatedPackets) {
        // Подсчёт статистики по пакетам
        int totalPackets          = simulatedPackets.size();
        int deliveredPackets      = 0;
        int lostPackets           = 0;
        int corruptedPackets      = 0;
        int retransmittedPackets  = 0;
        long totalBitErrors       = 0;
        long totalLatencyMs       = 0;
        long totalRetransmissions = 0;
        long receivedBytes        = 0;

        for (SimulatedPacket sp : simulatedPackets) {
            totalLatencyMs       += sp.getActualLatencyMs();
            totalBitErrors       += sp.getBitErrorsIntroduced();
            totalRetransmissions += sp.getRetransmissionCount();

            switch (sp.getStatus()) {
                case DELIVERED     -> { deliveredPackets++;     receivedBytes += sp.getOriginalPacket().getPayloadSize(); }
                case RETRANSMITTED -> { retransmittedPackets++; receivedBytes += sp.getOriginalPacket().getPayloadSize(); }
                case CORRUPTED     -> { corruptedPackets++;     /* UDP: данные с ошибками */ }
                case LOST          -> { lostPackets++; }
            }
        }

        // Среднее время на пакет → общее время передачи
        long avgLatencyMs = totalPackets > 0 ? totalLatencyMs / totalPackets : 0;

        // Время передачи = размер данных / пропускная способность + суммарная задержка
        long transmissionTimeMs = calculateTransmissionTimeMs(
                originalDataSize, params.getBandwidthBps(), avgLatencyMs, (int) totalRetransmissions
        );

        // Эффективная пропускная способность с учётом накладных расходов
        double effectiveThroughputMbps = calculateEffectiveThroughput(
                receivedBytes, transmissionTimeMs
        );

        // Фактический BER в симуляции
        long totalBitsTransmitted = (long) originalDataSize * 8;
        double actualBer = totalBitsTransmitted > 0
                ? (double) totalBitErrors / totalBitsTransmitted
                : 0.0;

        return TransmissionResult.builder()
                .channelParameters(params)
                .protocol(protocol)
                .dataSentBytes(originalDataSize)
                .dataReceivedBytes(receivedBytes)
                .transmissionTimeMs(transmissionTimeMs)
                .effectiveThroughputMbps(effectiveThroughputMbps)
                .averageLatencyMs(avgLatencyMs)
                .totalPackets(totalPackets)
                .deliveredPackets(deliveredPackets)
                .lostPackets(lostPackets)
                .corruptedPackets(corruptedPackets)
                .retransmittedPackets(retransmittedPackets)
                .totalBitErrors(totalBitErrors)
                .actualBitErrorRate(actualBer)
                .build();
    }

    /**
     * Оценивает общее время передачи.
     *
     * Формула: T = (dataSize / bandwidth) + propagationDelay
     *           + retransmissionOverhead
     */
    private long calculateTransmissionTimeMs(long dataSizeBytes,
                                             long bandwidthBps,
                                             long avgLatencyMs,
                                             int retransmissions) {
        if (bandwidthBps <= 0) return avgLatencyMs;

        // Время передачи данных по каналу (мс)
        double transmissionMs = ((double) dataSizeBytes * 8 / bandwidthBps) * 1000.0;

        // Накладные расходы на перепередачу (каждая перепередача добавляет RTT)
        double retransmissionOverheadMs = retransmissions * avgLatencyMs * 2.0;

        return Math.round(transmissionMs + avgLatencyMs + retransmissionOverheadMs);
    }

    /**
     * Вычисляет эффективную пропускную способность (Мбит/с).
     */
    private double calculateEffectiveThroughput(long receivedBytes, long transmissionTimeMs) {
        if (transmissionTimeMs <= 0) return 0.0;
        return ((double) receivedBytes * 8) / transmissionTimeMs / 1000.0; // Мбит/с
    }
}
