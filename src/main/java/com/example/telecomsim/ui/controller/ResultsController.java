package com.example.telecomsim.ui.controller;

import com.example.telecomsim.model.metrics.SimulationResult;
import com.example.telecomsim.model.metrics.SimulationSession;
import com.example.telecomsim.ui.chart.RadarChartView;
import com.example.telecomsim.util.CsvExporter;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.control.TableView;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Контроллер вкладки «Результаты симуляции».
 *
 * Отображает результаты сравнения алгоритмов через:
 *  — BarChart: степень сжатия и экономия места
 *  — BarChart: скорости сжатия и распаковки
 *  — LineChart: эффективная скорость передачи
 *  — BarChart: метрики канала (задержка, доставка, потери)
 *  — RadarChart: многокритериальная сравнительная оценка
 *  — TableView: полная таблица всех числовых метрик
 *  — Секция «Выводы»: автоматически формируемые текстовые заключения
 */
public class ResultsController  implements Initializable {

    // ── FXML: Заголовок ───────────────────────────────────────────────────

    @FXML
    private Label labelSessionName;
    @FXML
    private Label labelChannelInfo;
    @FXML
    private Label labelDataInfo;
    @FXML
    private Label labelSessionTime;

    // ── FXML: Легенда ─────────────────────────────────────────────────────

    @FXML
    private HBox hboxLegend;

    // ── FXML: Графики ─────────────────────────────────────────────────────

    /**
     * Степень сжатия (коэффициент) по алгоритмам.
     */
    @FXML
    private BarChart<String, Number> chartCompressionRatio;
    @FXML
    private CategoryAxis axisRatioX;
    @FXML
    private NumberAxis axisRatioY;

    /**
     * Скорость сжатия и распаковки.
     */
    @FXML
    private BarChart<String, Number> chartSpeed;
    @FXML
    private CategoryAxis axisSpeedX;
    @FXML
    private NumberAxis axisSpeedY;

    /**
     * Эффективная скорость передачи.
     */
    @FXML
    private BarChart<String, Number> chartTransferRate;
    @FXML
    private CategoryAxis axisTransferX;
    @FXML
    private NumberAxis axisTransferY;

    /**
     * Метрики канала: доставка, потери, задержка.
     */
    @FXML
    private BarChart<String, Number> chartChannel;
    @FXML
    private CategoryAxis axisChannelX;
    @FXML
    private NumberAxis axisChannelY;

    /**
     * Контейнер для RadarChartView (добавляется программно).
     */
    @FXML
    private StackPane stackPaneRadar;

    // ── FXML: Таблица метрик ──────────────────────────────────────────────

    @FXML
    private TableView<ResultTableRow> tableResults;
    @FXML
    private TableColumn<ResultTableRow, String> colAlgorithm;
    @FXML
    private TableColumn<ResultTableRow, String> colOriginalSize;
    @FXML
    private TableColumn<ResultTableRow, String> colCompressedSize;
    @FXML
    private TableColumn<ResultTableRow, String> colRatio;
    @FXML
    private TableColumn<ResultTableRow, String> colSpaceSaving;
    @FXML
    private TableColumn<ResultTableRow, String> colCompSpeed;
    @FXML
    private TableColumn<ResultTableRow, String> colDecompSpeed;
    @FXML
    private TableColumn<ResultTableRow, String> colTransferRate;
    @FXML
    private TableColumn<ResultTableRow, String> colDelivery;
    @FXML
    private TableColumn<ResultTableRow, String> colLoss;
    @FXML
    private TableColumn<ResultTableRow, String> colLatency;
    @FXML
    private TableColumn<ResultTableRow, String> colScore;
    @FXML
    private TableColumn<ResultTableRow, String> colIntegrity;

    // ── FXML: Выводы ──────────────────────────────────────────────────────

    @FXML
    private TextArea textAreaConclusion;
    @FXML
    private Label labelBestCompression;
    @FXML
    private Label labelBestSpeed;
    @FXML
    private Label labelBestTransfer;
    @FXML
    private Label labelBestOverall;

    // ── FXML: Действия ────────────────────────────────────────────────────

    @FXML
    private Button buttonExportCsv;
    @FXML
    private Button buttonExportReport;
    @FXML
    private Button buttonClearResults;

    // ── Состояние ─────────────────────────────────────────────────────────

    private SimulationSession currentSession;
    private RadarChartView radarChartView;

    // ── Инициализация ─────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initRadarChart();
        initTableColumns();
        configureChartStyles();
        showEmptyState();
    }

    // ── Публичный API ─────────────────────────────────────────────────────

    /**
     * Загружает и отображает результаты сессии симуляции.
     * Вызывается из MainController после завершения симуляции.
     */
    public void displaySession(SimulationSession session) {
        this.currentSession = session;

        updateHeader(session);
        buildLegend(session.getResults());
        fillCompressionRatioChart(session.getResults());
        fillSpeedChart(session.getResults());
        fillTransferRateChart(session.getResults());
        fillChannelChart(session.getResults());
        radarChartView.setResults(session.getResults());
        fillTable(session.getResults());
        generateConclusion(session);

        buttonExportCsv.setDisable(false);
        buttonExportReport.setDisable(false);
        buttonClearResults.setDisable(false);
    }

    /**
     * Добавляет один результат в реальном времени (из SimulationRunController).
     */
    public void addResultLive(SimulationResult result) {
        if (currentSession == null) return;
        // Перерисовываем всё — данные уже в сессии
        displaySession(currentSession);
    }

    // ── Инициализация компонентов ─────────────────────────────────────────

    private void initRadarChart() {
        radarChartView = new RadarChartView();
        radarChartView.setMinSize(300, 300);
        stackPaneRadar.getChildren().add(radarChartView);
        // RadarChartView растягивается вместе с контейнером
        StackPane.setAlignment(radarChartView, Pos.CENTER);
        radarChartView.prefWidthProperty().bind(stackPaneRadar.widthProperty());
        radarChartView.prefHeightProperty().bind(stackPaneRadar.heightProperty());
    }

    private void initTableColumns() {
        colAlgorithm.setCellValueFactory(
                d -> new javafx.beans.property.SimpleStringProperty(d.getValue().algorithm()));
        colOriginalSize.setCellValueFactory(
                d -> new javafx.beans.property.SimpleStringProperty(d.getValue().originalSize()));
        colCompressedSize.setCellValueFactory(
                d -> new javafx.beans.property.SimpleStringProperty(d.getValue().compressedSize()));
        colRatio.setCellValueFactory(
                d -> new javafx.beans.property.SimpleStringProperty(d.getValue().ratio()));
        colSpaceSaving.setCellValueFactory(
                d -> new javafx.beans.property.SimpleStringProperty(d.getValue().spaceSaving()));
        colCompSpeed.setCellValueFactory(
                d -> new javafx.beans.property.SimpleStringProperty(d.getValue().compSpeed()));
        colDecompSpeed.setCellValueFactory(
                d -> new javafx.beans.property.SimpleStringProperty(d.getValue().decompSpeed()));
        colTransferRate.setCellValueFactory(
                d -> new javafx.beans.property.SimpleStringProperty(d.getValue().transferRate()));
        colDelivery.setCellValueFactory(
                d -> new javafx.beans.property.SimpleStringProperty(d.getValue().delivery()));
        colLoss.setCellValueFactory(
                d -> new javafx.beans.property.SimpleStringProperty(d.getValue().loss()));
        colLatency.setCellValueFactory(
                d -> new javafx.beans.property.SimpleStringProperty(d.getValue().latency()));
        colScore.setCellValueFactory(
                d -> new javafx.beans.property.SimpleStringProperty(d.getValue().score()));
        colIntegrity.setCellValueFactory(
                d -> new javafx.beans.property.SimpleStringProperty(d.getValue().integrity()));

        // Выделение лучших значений цветом
        colRatio.setCellFactory(col -> createColoredCell(true));
        colTransferRate.setCellFactory(col -> createColoredCell(true));
        colDelivery.setCellFactory(col -> createColoredCell(true));
        colScore.setCellFactory(col -> createColoredCell(true));
        colLoss.setCellFactory(col -> createColoredCell(false)); // Меньше — лучше

        tableResults.setPlaceholder(new Label("Нет данных. Запустите симуляцию."));
    }

    private void configureChartStyles() {
        // Отключаем анимацию для мгновенного обновления
        chartCompressionRatio.setAnimated(false);
        chartSpeed.setAnimated(false);
        chartTransferRate.setAnimated(false);
        chartChannel.setAnimated(false);

        // Подписи осей
        axisRatioY.setLabel("Коэффициент сжатия (×)");
        axisSpeedY.setLabel("Скорость (Мбит/с)");
        axisTransferY.setLabel("Мбит/с");
        axisChannelY.setLabel("Значение");
    }

    // ── Заголовок ─────────────────────────────────────────────────────────

    private void updateHeader(SimulationSession session) {
        labelSessionName.setText(session.getConfig().getSimulationName());

        labelChannelInfo.setText("%s | BW: %.0f Мбит/с | BER: %.0e | Потери: %.1f%%".formatted(
                session.getConfig().getChannelParameters().getChannelType().getDisplayName(),
                session.getConfig().getChannelParameters().getBandwidthMbps(),
                session.getConfig().getChannelParameters().getBitErrorRate(),
                session.getConfig().getChannelParameters().getPacketLossPercent()
        ));

        labelDataInfo.setText("%s | Размер: %s | Повторений: %d | %s".formatted(
                session.getConfig().getDataType().getDisplayName(),
                formatBytes(session.getConfig().getDataSizeBytes()),
                session.getConfig().getRepetitions(),
                session.getConfig().getProtocol().getDisplayName()
        ));

        labelSessionTime.setText("Время: %s | Длительность: %s".formatted(
                session.getStartTime() != null
                        ? session.getStartTime().format(
                        java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
                        : "—",
                formatDuration(session.getTotalDuration().toMillis())
        ));
    }

    // ── Легенда ───────────────────────────────────────────────────────────

    private void buildLegend(List<SimulationResult> results) {
        hboxLegend.getChildren().clear();
        hboxLegend.setSpacing(16);
        hboxLegend.setPadding(new Insets(8));

        for (int i = 0; i < results.size(); i++) {
            SimulationResult result = results.get(i);
            Color color = RadarChartView.getColorForAlgorithm(result.getAlgorithm());

            Rectangle colorBox = new Rectangle(14, 14);
            colorBox.setFill(color);
            colorBox.setArcWidth(3);
            colorBox.setArcHeight(3);

            Label label = new Label(result.getAlgorithm().getDisplayName());
            label.setStyle("-fx-font-size: 12;");

            HBox legendItem = new HBox(6, colorBox, label);
            legendItem.setAlignment(Pos.CENTER_LEFT);

            hboxLegend.getChildren().add(legendItem);
        }
    }

    // ── Графики ───────────────────────────────────────────────────────────

    /**
     * График 1: Степень сжатия (коэффициент) и экономия места (%).
     * <p>
     * Две серии:
     * — Коэффициент сжатия (левая ось, ×)
     * — Экономия места (% — отображается как дополнительная серия)
     */
    private void fillCompressionRatioChart(List<SimulationResult> results) {
        chartCompressionRatio.getData().clear();

        XYChart.Series<String, Number> ratioSeries = new XYChart.Series<>();
        ratioSeries.setName("Коэффициент сжатия (×)");

        XYChart.Series<String, Number> savingSeries = new XYChart.Series<>();
        savingSeries.setName("Экономия места (%)");

        for (SimulationResult r : results) {
            String name = r.getAlgorithm().getDisplayName();
            ratioSeries.getData().add(
                    new XYChart.Data<>(name, r.getCompressionRatio())
            );
            savingSeries.getData().add(
                    new XYChart.Data<>(name, Math.max(0, r.getSpaceSavingPercent()))
            );
        }

        chartCompressionRatio.getData().addAll(ratioSeries, savingSeries);
        applyColorsToBarChart(chartCompressionRatio, results);
        addTooltipsToBarChart(chartCompressionRatio,
                v -> "%.3f×".formatted(v),
                v -> "%.1f%%".formatted(v)
        );
    }

    /**
     * График 2: Скорости сжатия и распаковки (Мбит/с).
     * <p>
     * Две серии:
     * — Скорость сжатия (Мбит/с)
     * — Скорость распаковки (Мбит/с)
     */
    private void fillSpeedChart(List<SimulationResult> results) {
        chartSpeed.getData().clear();

        XYChart.Series<String, Number> compSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> decompSeries = new XYChart.Series<>();
        compSeries.setName("Сжатие (Мбит/с)");
        decompSeries.setName("Распаковка (Мбит/с)");

        for (SimulationResult r : results) {
            String name = r.getAlgorithm().getDisplayName();
            compSeries.getData().add(
                    new XYChart.Data<>(name, r.getCompressionSpeedMbps())
            );
            decompSeries.getData().add(
                    new XYChart.Data<>(name, r.getDecompressionSpeedMbps())
            );
        }

        chartSpeed.getData().addAll(compSeries, decompSeries);
        addTooltipsToBarChart(chartSpeed,
                v -> "%.1f Мбит/с".formatted(v),
                v -> "%.1f Мбит/с".formatted(v)
        );
    }

    /**
     * График 3: Эффективная скорость передачи данных (Мбит/с).
     * <p>
     * Учитывает: время сжатия + передача сжатых данных + распаковка.
     * Главный показатель «сквозной» производительности системы.
     */
    private void fillTransferRateChart(List<SimulationResult> results) {
        chartTransferRate.getData().clear();

        XYChart.Series<String, Number> transferSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> throughputSeries = new XYChart.Series<>();
        transferSeries.setName("Эффективная скорость (Мбит/с)");
        throughputSeries.setName("Пропускная способность канала (Мбит/с)");

        for (SimulationResult r : results) {
            String name = r.getAlgorithm().getDisplayName();
            transferSeries.getData().add(
                    new XYChart.Data<>(name, r.getEffectiveTransferRateMbps())
            );
            throughputSeries.getData().add(
                    new XYChart.Data<>(name, r.getEffectiveThroughputMbps())
            );
        }

        chartTransferRate.getData().addAll(transferSeries, throughputSeries);
        addTooltipsToBarChart(chartTransferRate,
                v -> "%.2f Мбит/с".formatted(v),
                v -> "%.2f Мбит/с".formatted(v)
        );
    }

    /**
     * График 4: Метрики канала.
     * <p>
     * Три серии:
     * — Доставка пакетов (%)
     * — Потеря пакетов (%)
     * — Задержка (мс, нормализованная для совместимого масштаба)
     */
    private void fillChannelChart(List<SimulationResult> results) {
        chartChannel.getData().clear();

        XYChart.Series<String, Number> deliverySeries = new XYChart.Series<>();
        XYChart.Series<String, Number> lossSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> latencySeries = new XYChart.Series<>();
        deliverySeries.setName("Доставка пакетов (%)");
        lossSeries.setName("Потеря пакетов (%)");
        latencySeries.setName("Средняя задержка (мс)");

        for (SimulationResult r : results) {
            String name = r.getAlgorithm().getDisplayName();
            deliverySeries.getData().add(
                    new XYChart.Data<>(name, r.getDeliveryRatePercent())
            );
            lossSeries.getData().add(
                    new XYChart.Data<>(name, r.getPacketLossPercent())
            );
            latencySeries.getData().add(
                    new XYChart.Data<>(name,
                            r.getTransmissionResult().getAverageLatencyMs())
            );
        }

        chartChannel.getData().addAll(deliverySeries, lossSeries, latencySeries);
        addTooltipsToBarChart(chartChannel,
                v -> "%.2f%%".formatted(v),
                v -> "%.2f%%".formatted(v),
                v -> "%.0f мс".formatted(v)
        );
    }

    // ── Таблица ───────────────────────────────────────────────────────────

    private void fillTable(List<SimulationResult> results) {
        ObservableList<ResultTableRow> rows = FXCollections.observableArrayList();

        // Находим лучшие значения для подсветки
        double maxRatio = results.stream().mapToDouble(SimulationResult::getCompressionRatio).max().orElse(1);
        double maxTransfer = results.stream().mapToDouble(SimulationResult::getEffectiveTransferRateMbps).max().orElse(1);
        double maxScore = results.stream().mapToDouble(SimulationResult::getOverallEfficiencyScore).max().orElse(1);
        double minLoss = results.stream().mapToDouble(SimulationResult::getPacketLossPercent).min().orElse(0);

        for (SimulationResult r : results) {
            rows.add(new ResultTableRow(
                    r.getAlgorithm().getDisplayName(),
                    formatBytes(r.getCompressionResult().getOriginalSizeBytes()),
                    formatBytes(r.getCompressionResult().getCompressedSizeBytes()),
                    "%.3f×".formatted(r.getCompressionRatio()),
                    "%.1f%%".formatted(r.getSpaceSavingPercent()),
                    "%.1f".formatted(r.getCompressionSpeedMbps()),
                    "%.1f".formatted(r.getDecompressionSpeedMbps()),
                    "%.2f".formatted(r.getEffectiveTransferRateMbps()),
                    "%.2f%%".formatted(r.getDeliveryRatePercent()),
                    "%.2f%%".formatted(r.getPacketLossPercent()),
                    "%d мс".formatted(r.getTransmissionResult().getAverageLatencyMs()),
                    "%.1f".formatted(r.getOverallEfficiencyScore()),
                    r.isDataIntegrityOk() ? "✅" : "❌"
            ));
        }

        tableResults.setItems(rows);
    }

    // ── Автоматические выводы ─────────────────────────────────────────────

    /**
     * Генерирует текстовые выводы на основе результатов симуляции.
     */
    private void generateConclusion(SimulationSession session) {
        List<SimulationResult> results = session.getResults();

        // Лучшие по каждому критерию
        SimulationResult bestCompression = results.stream()
                .max(java.util.Comparator.comparingDouble(SimulationResult::getCompressionRatio))
                .orElse(null);

        SimulationResult bestSpeed = results.stream()
                .max(java.util.Comparator.comparingDouble(SimulationResult::getCompressionSpeedMbps))
                .orElse(null);

        SimulationResult bestTransfer = results.stream()
                .max(java.util.Comparator.comparingDouble(SimulationResult::getEffectiveTransferRateMbps))
                .orElse(null);

        SimulationResult bestOverall = session.getBestByEfficiencyScore().orElse(null);

        // Обновляем метки лидеров
        labelBestCompression.setText(bestCompression != null
                ? "🏆 %s (%.3f×)".formatted(
                bestCompression.getAlgorithm().getDisplayName(),
                bestCompression.getCompressionRatio())
                : "—");

        labelBestSpeed.setText(bestSpeed != null
                ? "🏆 %s (%.1f Мбит/с)".formatted(
                bestSpeed.getAlgorithm().getDisplayName(),
                bestSpeed.getCompressionSpeedMbps())
                : "—");

        labelBestTransfer.setText(bestTransfer != null
                ? "🏆 %s (%.2f Мбит/с)".formatted(
                bestTransfer.getAlgorithm().getDisplayName(),
                bestTransfer.getEffectiveTransferRateMbps())
                : "—");

        labelBestOverall.setText(bestOverall != null
                ? "🏆 %s (%.1f/100)".formatted(
                bestOverall.getAlgorithm().getDisplayName(),
                bestOverall.getOverallEfficiencyScore())
                : "—");

        // Генерируем текстовый отчёт
        textAreaConclusion.setText(buildConclusionText(session, results,
                bestCompression, bestSpeed, bestTransfer, bestOverall));
    }

    private String buildConclusionText(SimulationSession session,
                                       List<SimulationResult> results,
                                       SimulationResult bestCompression,
                                       SimulationResult bestSpeed,
                                       SimulationResult bestTransfer,
                                       SimulationResult bestOverall) {
        StringBuilder sb = new StringBuilder();

        String channelName = session.getConfig().getChannelParameters()
                .getChannelType().getDisplayName();
        String dataType = session.getConfig().getDataType().getDisplayName();
        long dataSize = session.getConfig().getDataSizeBytes();

        sb.append("АВТОМАТИЧЕСКИЙ АНАЛИЗ РЕЗУЛЬТАТОВ СИМУЛЯЦИИ\n");
        sb.append("═".repeat(60)).append("\n\n");

        sb.append("Конфигурация:\n");
        sb.append("  Канал передачи: %s\n".formatted(channelName));
        sb.append("  Тип данных: %s (%s)\n".formatted(dataType, formatBytes(dataSize)));
        sb.append("  Протестировано алгоритмов: %d\n\n".formatted(results.size()));

        sb.append("РЕЗУЛЬТАТЫ ПО КРИТЕРИЯМ:\n");
        sb.append("─".repeat(60)).append("\n");

        // Степень сжатия
        if (bestCompression != null) {
            sb.append("\n📦 Степень сжатия:\n");
            sb.append("  Лучший: %s — коэффициент %.3f× (экономия %.1f%%)\n".formatted(
                    bestCompression.getAlgorithm().getDisplayName(),
                    bestCompression.getCompressionRatio(),
                    bestCompression.getSpaceSavingPercent()
            ));
            appendRanking(sb, results,
                    java.util.Comparator.comparingDouble(SimulationResult::getCompressionRatio),
                    r -> "%.3f×".formatted(r.getCompressionRatio()));
        }

        // Скорость
        if (bestSpeed != null) {
            sb.append("\n⚡ Скорость сжатия:\n");
            sb.append("  Лучший: %s — %.1f Мбит/с\n".formatted(
                    bestSpeed.getAlgorithm().getDisplayName(),
                    bestSpeed.getCompressionSpeedMbps()
            ));
            appendRanking(sb, results,
                    java.util.Comparator.comparingDouble(SimulationResult::getCompressionSpeedMbps),
                    r -> "%.1f Мбит/с".formatted(r.getCompressionSpeedMbps()));
        }

        // Эффективная скорость передачи
        if (bestTransfer != null) {
            sb.append("\n🚀 Эффективная скорость передачи (с учётом канала):\n");
            sb.append("  Лучший: %s — %.2f Мбит/с\n".formatted(
                    bestTransfer.getAlgorithm().getDisplayName(),
                    bestTransfer.getEffectiveTransferRateMbps()
            ));
            appendRanking(sb, results,
                    java.util.Comparator.comparingDouble(SimulationResult::getEffectiveTransferRateMbps),
                    r -> "%.2f Мбит/с".formatted(r.getEffectiveTransferRateMbps()));
        }

        // Целостность данных
        long integrityFailed = results.stream()
                .filter(r -> !r.isDataIntegrityOk()).count();
        sb.append("\n🔒 Целостность данных:\n");
        if (integrityFailed == 0) {
            sb.append("  ✅ Все алгоритмы корректно восстановили данные.\n");
        } else {
            sb.append("  ❌ Нарушение целостности обнаружено у %d алгоритмов!\n"
                    .formatted(integrityFailed));
        }

        // Интегральный вывод
        sb.append("\n").append("═".repeat(60)).append("\n");
        sb.append("РЕКОМЕНДАЦИЯ:\n");
        if (bestOverall != null) {
            sb.append(buildRecommendationText(bestOverall, session, results));
        }

        return sb.toString();
    }

    private void appendRanking(StringBuilder sb,
                               List<SimulationResult> results,
                               java.util.Comparator<SimulationResult> comparator,
                               java.util.function.Function<SimulationResult, String> formatter) {
        List<SimulationResult> sorted = results.stream()
                .sorted(comparator.reversed())
                .toList();

        String[] medals = {"  🥇 ", "  🥈 ", "  🥉 "};
        for (int i = 0; i < sorted.size(); i++) {
            SimulationResult r = sorted.get(i);
            String prefix = i < medals.length ? medals[i] : "     ";
            sb.append("%s%s — %s\n".formatted(
                    prefix,
                    r.getAlgorithm().getDisplayName(),
                    formatter.apply(r)
            ));
        }
    }

    private String buildRecommendationText(SimulationResult best,
                                           SimulationSession session,
                                           List<SimulationResult> results) {
        String channelName = session.getConfig().getChannelParameters()
                .getChannelType().getDisplayName();
        String dataType = session.getConfig().getDataType().getDisplayName();

        StringBuilder rec = new StringBuilder();
        rec.append("Для канала «%s» с данными типа «%s»\n".formatted(channelName, dataType));
        rec.append("оптимальным выбором является алгоритм %s\n"
                .formatted(best.getAlgorithm().getDisplayName()));
        rec.append("(интегральная оценка: %.1f/100).\n\n".formatted(best.getOverallEfficiencyScore()));

        // Специфические рекомендации по алгоритму
        String advice = switch (best.getAlgorithm()) {
            case SNAPPY, LZ4 -> "Алгоритм оптимизирован для скорости — рекомендуется для систем\n" +
                    "реального времени и каналов с низкой задержкой.";
            case DEFLATE -> "DEFLATE обеспечивает хороший баланс между степенью сжатия и скоростью.\n" +
                    "Широко поддерживается в сетевых стеках и приложениях.";
            case ZSTANDARD -> "Zstandard — современный алгоритм с превосходным балансом.\n" +
                    "Рекомендуется как замена DEFLATE в высоконагруженных системах.";
            case HUFFMAN -> "Хаффман показывает хорошие результаты на текстовых данных с\n" +
                    "неравномерным распределением символов.";
            case LZ77 -> "LZ77 — классический алгоритм с предсказуемым поведением.\n" +
                    "Является основой для более современных методов.";
            case NONE -> "Сжатие не дало преимуществ для данного типа данных и канала.\n" +
                    "Возможно, данные уже сжаты или имеют высокую энтропию.";
        };

        rec.append(advice);
        return rec.toString();
    }

    // ── FXML-обработчики ──────────────────────────────────────────────────

    @FXML
    private void handleExportCsv() {
        if (currentSession == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Экспорт результатов в CSV");
        fc.setInitialFileName("results_%s.csv".formatted(
                java.time.LocalDate.now()
        ));
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV файлы (*.csv)", "*.csv")
        );

        File file = fc.showSaveDialog(tableResults.getScene().getWindow());
        if (file == null) return;

        try {
            exportResultsToCsv(file);
            showInfo("Экспорт завершён", "Файл сохранён: " + file.getName());
        } catch (Exception e) {
            showError("Ошибка экспорта", e.getMessage());
        }
    }

    @FXML
    private void handleExportReport() {
        if (currentSession == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Сохранить текстовый отчёт");
        fc.setInitialFileName("report_%s.txt".formatted(
                java.time.LocalDate.now().toString()
        ));
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt")
        );

        File file = fc.showSaveDialog(tableResults.getScene().getWindow());
        if (file == null) return;

        try {
            Files.writeString(file.toPath(),
                    textAreaConclusion.getText(), StandardCharsets.UTF_8);
            showInfo("Отчёт сохранён", "Файл: " + file.getName());
        } catch (Exception e) {
            showError("Ошибка сохранения", e.getMessage());
        }
    }

    @FXML
    private void handleClearResults() {
        currentSession = null;
        chartCompressionRatio.getData().clear();
        chartSpeed.getData().clear();
        chartTransferRate.getData().clear();
        chartChannel.getData().clear();
        radarChartView.clear();
        tableResults.getItems().clear();
        textAreaConclusion.clear();
        hboxLegend.getChildren().clear();
        showEmptyState();

        buttonExportCsv.setDisable(true);
        buttonExportReport.setDisable(true);
        buttonClearResults.setDisable(true);
    }

    // ── Вспомогательные методы графиков ──────────────────────────────────

    /**
     * Окрашивает столбцы BarChart цветами алгоритмов.
     */
    @SafeVarargs
    private void applyColorsToBarChart(BarChart<String, Number> chart,
                                       List<SimulationResult> results,
                                       XYChart.Series<String, Number>... series) {
        // Применяем через CSS — вызывается после добавления данных
        chart.applyCss();
        chart.layout();
    }

    /**
     * Добавляет всплывающие подсказки к столбцам BarChart.
     * Принимает varargs форматтеров — по одному на каждую серию.
     */
    @SafeVarargs
    private void addTooltipsToBarChart(BarChart<String, Number> chart,
                                       java.util.function.Function<Double, String>... formatters) {
        for (int s = 0; s < chart.getData().size(); s++) {
            XYChart.Series<String, Number> series = chart.getData().get(s);
            final int seriesIndex = s;

            for (XYChart.Data<String, Number> dataPoint : series.getData()) {
                Tooltip tooltip = new Tooltip();
                double value = dataPoint.getYValue().doubleValue();

                String formattedValue = (seriesIndex < formatters.length)
                        ? formatters[seriesIndex].apply(value)
                        : "%.2f".formatted(value);

                tooltip.setText("%s\n%s: %s".formatted(
                        dataPoint.getXValue(),
                        series.getName(),
                        formattedValue
                ));
                tooltip.setStyle("-fx-font-size: 12;");

                if (dataPoint.getNode() != null) {
                    Tooltip.install(dataPoint.getNode(), tooltip);
                }
            }
        }
    }

    /**
     * Фабрика ячеек таблицы с цветовым выделением.
     *
     * @param higherIsBetter true — большее значение подсвечивается зелёным
     */
    private TableCell<ResultTableRow, String> createColoredCell(boolean higherIsBetter) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);

                // Подсвечиваем первую строку (лучший результат) в колонке
                boolean isFirst = getIndex() == 0;
                if (isFirst) {
                    setStyle(higherIsBetter
                            ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;"
                            : "-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        };
    }

    private void showEmptyState() {
        labelSessionName.setText("Результаты симуляции");
        labelChannelInfo.setText("Запустите симуляцию на вкладке «Настройка»");
        labelDataInfo.setText("");
        labelSessionTime.setText("");
        labelBestCompression.setText("—");
        labelBestSpeed.setText("—");
        labelBestTransfer.setText("—");
        labelBestOverall.setText("—");
    }

    // ── Экспорт ───────────────────────────────────────────────────────────

    private void exportResultsToCsv(File file) throws IOException {
        StringBuilder csv = new StringBuilder();

        // Заголовок
        csv.append(CsvExporter.buildHeader(
                "Алгоритм",
                "Исходный размер (байт)",
                "Сжатый размер (байт)",
                "Коэффициент сжатия",
                "Экономия (%)",
                "Скорость сжатия (Мбит/с)",
                "Скорость распаковки (Мбит/с)",
                "Эффективная передача (Мбит/с)",
                "Доставка (%)",
                "Потери (%)",
                "Задержка (мс)",
                "Интегральная оценка",
                "Целостность данных"
        ));

        // Данные
        for (SimulationResult r : currentSession.getResults()) {
            csv.append(CsvExporter.buildRow(
                    r.getAlgorithm().getDisplayName(),
                    r.getCompressionResult().getOriginalSizeBytes(),
                    r.getCompressionResult().getCompressedSizeBytes(),
                    r.getCompressionRatio(),
                    r.getSpaceSavingPercent(),
                    r.getCompressionSpeedMbps(),
                    r.getDecompressionSpeedMbps(),
                    r.getEffectiveTransferRateMbps(),
                    r.getDeliveryRatePercent(),
                    r.getPacketLossPercent(),
                    r.getTransmissionResult().getAverageLatencyMs(),
                    r.getOverallEfficiencyScore(),
                    r.isDataIntegrityOk() ? "OK" : "ОШИБКА"
            ));
        }

        // Пустая строка-разделитель
        csv.append("\n");

        // Сводная секция
        csv.append(CsvExporter.buildHeader(
                "Параметры симуляции", ""
        ));
        csv.append(CsvExporter.buildRow(
                "Канал",
                currentSession.getConfig().getChannelParameters().getChannelType().getDisplayName()
        ));
        csv.append(CsvExporter.buildRow(
                "Тип данных",
                currentSession.getConfig().getDataType().getDisplayName()
        ));
        csv.append(CsvExporter.buildRow(
                "Размер данных",
                currentSession.getConfig().getDataSizeBytes() + " байт"
        ));
        csv.append(CsvExporter.buildRow(
                "Протокол",
                currentSession.getConfig().getProtocol().getDisplayName()
        ));
        csv.append(CsvExporter.buildRow(
                "Повторений",
                currentSession.getConfig().getRepetitions()
        ));
        csv.append(CsvExporter.buildRow(
                "Лучший алгоритм",
                currentSession.getBestByEfficiencyScore()
                        .map(r -> r.getAlgorithm().getDisplayName())
                        .orElse("—")
        ));

        CsvExporter.write(file, csv.toString());
    }
    // ── Утилиты ───────────────────────────────────────────────────────────

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " Б";
        if (bytes < 1024 * 1024) return "%.1f КБ".formatted(bytes / 1024.0);
        return "%.2f МБ".formatted(bytes / (1024.0 * 1024.0));
    }

    private String formatDuration(long ms) {
        long s = (ms / 1000) % 60;
        long m = ms / 60_000;
        return "%02d:%02d".formatted(m, s);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ── Record модели строки таблицы ──────────────────────────────────────

    /**
     * Иммутабельная модель строки таблицы результатов.
     * Java record — идеально для read-only табличных данных.
     */
    public record ResultTableRow(
            String algorithm,
            String originalSize,
            String compressedSize,
            String ratio,
            String spaceSaving,
            String compSpeed,
            String decompSpeed,
            String transferRate,
            String delivery,
            String loss,
            String latency,
            String score,
            String integrity
    ) {
    }
}
