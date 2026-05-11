package com.example.telecomsim.service.simulation;

import com.example.telecomsim.model.channel.ChannelParameters;
import com.example.telecomsim.model.compression.CompressionAlgorithm;
import com.example.telecomsim.model.compression.CompressionSettings;
import com.example.telecomsim.model.metrics.CompressionResult;
import com.example.telecomsim.model.metrics.SimulationResult;
import com.example.telecomsim.model.metrics.SimulationSession;
import com.example.telecomsim.model.metrics.TransmissionResult;
import com.example.telecomsim.model.simulation.SimulationConfig;
import com.example.telecomsim.model.simulation.TransmissionProtocol;
import com.example.telecomsim.service.channel.ChannelSimulator;
import com.example.telecomsim.service.channel.ChannelSimulatorFactory;
import com.example.telecomsim.service.compression.CompressionAlgorithmService;
import com.example.telecomsim.service.TestDataGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Реализация SimulationService.
 *
 * Алгоритм работы для каждого выбранного алгоритма сжатия:
 *  1. Генерируем/загружаем входные данные (TestDataGenerator)
 *  2. Измеряем сжатие: время + коэффициент + скорость
 *  3. Симулируем передачу сжатых данных по каналу (ChannelSimulator)
 *  4. Измеряем распаковку
 *  5. Проверяем целостность данных
 *  6. Собираем SimulationResult
 *  7. Усредняем по количеству повторений (config.getRepetitions())
 */
public class SimulationServiceImpl  implements SimulationService {

    private final TestDataGenerator dataGenerator;

    /** Флаг отмены — volatile для видимости между потоками. */
    private volatile boolean cancelRequested = false;

    /** Текущая задача — для отслеживания статуса. */
    private volatile Thread simulationThread = null;

    public SimulationServiceImpl(TestDataGenerator dataGenerator) {
        this.dataGenerator = Objects.requireNonNull(dataGenerator);
    }

    // ------------------------------------------------------------------ //
    //  Синхронный запуск                                                   //
    // ------------------------------------------------------------------ //

    @Override
    public SimulationSession runSimulation(SimulationConfig config) {
        validateConfig(config);
        cancelRequested = false;

        LocalDateTime startTime = LocalDateTime.now();

        // Генерируем входные данные один раз для всех алгоритмов (честное сравнение)
        byte[] inputData = dataGenerator.generate(config.getDataType(), config.getDataSizeBytes());

        List<SimulationResult> results = new ArrayList<>();

        List<CompressionAlgorithm> algorithms = config.getAlgorithmsToCompare();
        for (int i = 0; i < algorithms.size(); i++) {
            if (cancelRequested) break;

            CompressionAlgorithm algorithm = algorithms.get(i);
            SimulationResult result = runForAlgorithm(algorithm, inputData, config);
            results.add(result);
        }

        return SimulationSession.builder()
                .config(config)
                .results(results)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Асинхронный запуск                                                  //
    // ------------------------------------------------------------------ //

    @Override
    public void runSimulationAsync(SimulationConfig config, SimulationProgressCallback callback) {
        Objects.requireNonNull(callback, "Callback не может быть null");

        cancelRequested = false;

        simulationThread = new Thread(() -> {
            try {
                validateConfig(config);

                LocalDateTime startTime = LocalDateTime.now();
                byte[] inputData = dataGenerator.generate(
                        config.getDataType(), config.getDataSizeBytes()
                );

                List<SimulationResult> results = new ArrayList<>();
                List<CompressionAlgorithm> algorithms = config.getAlgorithmsToCompare();
                int total = algorithms.size();

                for (int i = 0; i < total; i++) {
                    if (cancelRequested) {
                        callback.onCancelled();
                        return;
                    }

                    CompressionAlgorithm algorithm = algorithms.get(i);

                    // Уведомляем UI о начале обработки алгоритма
                    double progress = (double) i / total;
                    callback.onProgress(
                            progress,
                            "Обработка алгоритма: %s (%d из %d)".formatted(
                                    algorithm.getDisplayName(), i + 1, total
                            )
                    );

                    SimulationResult result = runForAlgorithm(algorithm, inputData, config);
                    results.add(result);

                    // Уведомляем UI о завершении одного алгоритма
                    callback.onAlgorithmCompleted(result);
                }

                SimulationSession session = SimulationSession.builder()
                        .config(config)
                        .results(results)
                        .startTime(startTime)
                        .endTime(LocalDateTime.now())
                        .build();

                callback.onProgress(1.0, "Симуляция завершена");
                callback.onCompleted(session);

            } catch (Exception e) {
                callback.onError(e);
            }
        }, "simulation-thread");

        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    @Override
    public void cancelSimulation() {
        cancelRequested = true;
    }

    @Override
    public boolean isRunning() {
        return simulationThread != null
                && simulationThread.isAlive()
                && !cancelRequested;
    }

    // ------------------------------------------------------------------ //
    //  Основная логика: симуляция для одного алгоритма                    //
    // ------------------------------------------------------------------ //

    /**
     * Выполняет полный цикл симуляции для одного алгоритма сжатия.
     * Повторяет измерения config.getRepetitions() раз и усредняет результат.
     */
    private SimulationResult runForAlgorithm(CompressionAlgorithm algorithm,
                                             byte[] inputData,
                                             SimulationConfig config) {
        CompressionAlgorithmService compressionService =
                CompressionServiceFactory.create(algorithm);

        ChannelSimulator channelSimulator =
                ChannelSimulatorFactory.create(config.getChannelParameters().getChannelType());

        int repetitions = Math.max(1, config.getRepetitions());

        // Накапливаем результаты для усреднения
        List<CompressionResult>  compressionResults  = new ArrayList<>(repetitions);
        List<TransmissionResult> transmissionResults = new ArrayList<>(repetitions);

        for (int rep = 0; rep < repetitions; rep++) {
            if (cancelRequested) break;

            // --- Сжатие ---
            CompressionResult compressionResult = measureCompression(
                    compressionService, inputData, config.getCompressionSettings(algorithm)
            );
            compressionResults.add(compressionResult);

            // --- Передача по каналу ---
            TransmissionResult transmissionResult = channelSimulator.simulate(
                    compressionResult.getCompressedData(),
                    config.getChannelParameters(),
                    config.getProtocol()
            );
            transmissionResults.add(transmissionResult);
        }

        // Усредняем результаты по всем повторениям
        CompressionResult  avgCompression  = averageCompressionResults(compressionResults, algorithm);
        TransmissionResult avgTransmission = averageTransmissionResults(transmissionResults, config.getChannelParameters(), config.getProtocol());

        return buildSimulationResult(algorithm, avgCompression, avgTransmission);
    }

    // ------------------------------------------------------------------ //
    //  Измерение сжатия                                                   //
    // ------------------------------------------------------------------ //

    /**
     * Измеряет метрики сжатия и распаковки для одного прогона.
     */
    private CompressionResult measureCompression(CompressionAlgorithmService service,
                                                 byte[] inputData,
                                                 CompressionSettings settings) {
        // --- Сжатие ---
        long compressStartNs  = System.nanoTime();
        byte[] compressedData = service.compress(inputData, settings);
        long compressEndNs    = System.nanoTime();
        long compressionTimeNs = compressEndNs - compressStartNs;

        // --- Распаковка ---
        long decompressStartNs = System.nanoTime();
        byte[] decompressedData = service.decompress(compressedData, settings);
        long decompressEndNs    = System.nanoTime();
        long decompressionTimeNs = decompressEndNs - decompressStartNs;

        // --- Проверка целостности ---
        boolean integrityOk = Arrays.equals(inputData, decompressedData);

        // --- Вычисление метрик ---
        long originalSize    = inputData.length;
        long compressedSize  = compressedData.length;
        double ratio         = (double) originalSize / Math.max(compressedSize, 1);
        double spaceSaving   = (1.0 - (double) compressedSize / originalSize) * 100.0;
        double compSpeedMbps = calculateSpeedMbps(originalSize,   compressionTimeNs);
        double decSpeedMbps  = calculateSpeedMbps(compressedSize, decompressionTimeNs);

        return CompressionResult.builder()
                .algorithm(service.getAlgorithm())
                .originalSizeBytes(originalSize)
                .compressedSizeBytes(compressedSize)
                .compressedData(compressedData)
                .compressionRatio(ratio)
                .spaceSavingPercent(spaceSaving)
                .compressionTimeNs(compressionTimeNs)
                .decompressionTimeNs(decompressionTimeNs)
                .compressionSpeedMbps(compSpeedMbps)
                .decompressionSpeedMbps(decSpeedMbps)
                .dataIntegrityOk(integrityOk)
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Усреднение результатов                                              //
    // ------------------------------------------------------------------ //

    private CompressionResult averageCompressionResults(List<CompressionResult> results,
                                                        CompressionAlgorithm algorithm) {
        if (results.size() == 1) return results.get(0);

        long   avgCompressionTimeNs   = (long) results.stream().mapToLong(CompressionResult::getCompressionTimeNs).average().orElse(0);
        long   avgDecompressionTimeNs = (long) results.stream().mapToLong(CompressionResult::getDecompressionTimeNs).average().orElse(0);
        double avgCompressionSpeed    = results.stream().mapToDouble(CompressionResult::getCompressionSpeedMbps).average().orElse(0);
        double avgDecompressionSpeed  = results.stream().mapToDouble(CompressionResult::getDecompressionSpeedMbps).average().orElse(0);

        // Размеры не усредняем — детерминированы для одних данных
        CompressionResult first = results.get(0);

        return CompressionResult.builder()
                .algorithm(algorithm)
                .originalSizeBytes(first.getOriginalSizeBytes())
                .compressedSizeBytes(first.getCompressedSizeBytes())
                .compressedData(first.getCompressedData())
                .compressionRatio(first.getCompressionRatio())
                .spaceSavingPercent(first.getSpaceSavingPercent())
                .compressionTimeNs(avgCompressionTimeNs)
                .decompressionTimeNs(avgDecompressionTimeNs)
                .compressionSpeedMbps(avgCompressionSpeed)
                .decompressionSpeedMbps(avgDecompressionSpeed)
                .dataIntegrityOk(first.isDataIntegrityOk())
                .build();
    }

    private TransmissionResult averageTransmissionResults(List<TransmissionResult> results,
                                                          ChannelParameters params,
                                                          TransmissionProtocol protocol) {
        if (results.size() == 1) return results.get(0);

        return TransmissionResult.builder()
                .channelParameters(params)
                .protocol(protocol)
                .dataSentBytes((long) results.stream().mapToLong(TransmissionResult::getDataSentBytes).average().orElse(0))
                .dataReceivedBytes((long) results.stream().mapToLong(TransmissionResult::getDataReceivedBytes).average().orElse(0))
                .transmissionTimeMs((long) results.stream().mapToLong(TransmissionResult::getTransmissionTimeMs).average().orElse(0))
                .effectiveThroughputMbps(results.stream().mapToDouble(TransmissionResult::getEffectiveThroughputMbps).average().orElse(0))
                .averageLatencyMs((long) results.stream().mapToLong(TransmissionResult::getAverageLatencyMs).average().orElse(0))
                .totalPackets((int) results.stream().mapToInt(TransmissionResult::getTotalPackets).average().orElse(0))
                .deliveredPackets((int) results.stream().mapToInt(TransmissionResult::getDeliveredPackets).average().orElse(0))
                .lostPackets((int) results.stream().mapToInt(TransmissionResult::getLostPackets).average().orElse(0))
                .corruptedPackets((int) results.stream().mapToInt(TransmissionResult::getCorruptedPackets).average().orElse(0))
                .retransmittedPackets((int) results.stream().mapToInt(TransmissionResult::getRetransmittedPackets).average().orElse(0))
                .totalBitErrors((long) results.stream().mapToLong(TransmissionResult::getTotalBitErrors).average().orElse(0))
                .actualBitErrorRate(results.stream().mapToDouble(TransmissionResult::getActualBitErrorRate).average().orElse(0))
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Сборка итогового SimulationResult                                   //
    // ------------------------------------------------------------------ //

    private SimulationResult buildSimulationResult(CompressionAlgorithm algorithm,
                                                   CompressionResult compression,
                                                   TransmissionResult transmission) {
        // Интегральная оценка эффективности:
        // учитываем степень сжатия, скорость и качество доставки
        double efficiencyScore = calculateEfficiencyScore(compression, transmission);

        // Итоговая скорость передачи с учётом сжатия и канала
        double effectiveTransferRate = calculateEffectiveTransferRate(compression, transmission);

        return SimulationResult.builder()
                .algorithm(algorithm)
                .compressionResult(compression)
                .transmissionResult(transmission)
                .overallEfficiencyScore(efficiencyScore)
                .effectiveTransferRateMbps(effectiveTransferRate)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Интегральная оценка [0..100]:
     * 40% — степень сжатия (нормализованная)
     * 30% — скорость сжатия (нормализованная)
     * 30% — качество доставки (delivery rate)
     */
    private double calculateEfficiencyScore(CompressionResult compression,
                                            TransmissionResult transmission) {
        double maxExpectedRatio     = 10.0; // Лучшие алгоритмы дают ~8-10x на тексте
        double maxExpectedSpeedMbps = 5000.0; // LZ4/Snappy: тысячи МБ/с

        double compressionScore = Math.min(compression.getCompressionRatio() / maxExpectedRatio, 1.0) * 40;
        double speedScore       = Math.min(compression.getCompressionSpeedMbps() / maxExpectedSpeedMbps, 1.0) * 30;
        double deliveryScore    = (transmission.getDeliveryRatePercent() / 100.0) * 30;

        return compressionScore + speedScore + deliveryScore;
    }

    /**
     * Эффективная скорость передачи с учётом сжатия:
     * фактически переданные данные / общее время (сжатие + передача + распаковка).
     */
    private double calculateEffectiveTransferRate(CompressionResult compression,
                                                  TransmissionResult transmission) {
        long originalBytes     = compression.getOriginalSizeBytes();
        long totalTimeMs       = transmission.getTransmissionTimeMs()
                + compression.getCompressionTimeNs() / 1_000_000
                + compression.getDecompressionTimeNs() / 1_000_000;

        if (totalTimeMs <= 0) return 0.0;
        return ((double) originalBytes * 8) / totalTimeMs / 1000.0; // Мбит/с
    }

    // ------------------------------------------------------------------ //
    //  Утилиты                                                             //
    // ------------------------------------------------------------------ //

    private double calculateSpeedMbps(long dataBytes, long timeNs) {
        if (timeNs <= 0) return 0.0;
        return ((double) dataBytes * 8) / timeNs * 1000.0; // нс → Мбит/с
    }

    private void validateConfig(SimulationConfig config) {
        Objects.requireNonNull(config, "Конфигурация симуляции не может быть null");
        if (config.getAlgorithmsToCompare() == null || config.getAlgorithmsToCompare().isEmpty()) {
            throw new SimulationException("Не выбран ни один алгоритм сжатия для симуляции");
        }
        if (config.getChannelParameters() == null) {
            throw new SimulationException("Параметры канала не заданы");
        }
        if (config.getDataSizeBytes() <= 0) {
            throw new SimulationException("Размер данных должен быть больше нуля");
        }
    }
}
