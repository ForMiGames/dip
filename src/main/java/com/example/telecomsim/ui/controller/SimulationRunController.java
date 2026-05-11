package com.example.telecomsim.ui.controller;

import com.example.telecomsim.model.compression.CompressionAlgorithm;
import com.example.telecomsim.model.metrics.SimulationResult;
import com.example.telecomsim.model.metrics.SimulationSession;
import com.example.telecomsim.model.simulation.SimulationConfig;
import com.example.telecomsim.service.simulation.SimulationProgressCallback;
import com.example.telecomsim.service.simulation.SimulationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Контроллер вкладки «Выполнение симуляции».
 *
 * Отображает:
 *  — Прогресс выполнения текущей симуляции
 *  — Статус каждого алгоритма (ожидание / выполняется / завершён / ошибка)
 *  — Лог событий симуляции в реальном времени
 *  — Предварительные результаты по мере завершения каждого алгоритма
 *
 * Взаимодействие:
 *  — Получает SimulationConfig от SimulationSetupController через MainController
 *  — Передаёт SimulationSession в ResultsController после завершения
 */
public class SimulationRunController implements Initializable {

    // ── FXML-компоненты ───────────────────────────────────────────────────

    /**
     * Общий прогресс симуляции (0.0 – 1.0).
     */
    @FXML
    private ProgressBar progressBarOverall;

    /**
     * Текстовое описание текущего шага.
     */
    @FXML
    private Label labelCurrentStep;

    /**
     * Процент выполнения (напр. "67%").
     */
    @FXML
    private Label labelProgressPercent;

    /**
     * Кнопка отмены симуляции.
     */
    @FXML
    private Button buttonCancel;

    /**
     * Контейнер карточек статуса алгоритмов.
     */
    @FXML
    private VBox vboxAlgorithmStatuses;

    /**
     * Текстовая область лога событий.
     */
    @FXML
    private TextArea textAreaLog;

    /**
     * Метка суммарного времени выполнения.
     */
    @FXML
    private Label labelElapsedTime;

    /**
     * Метка оставшегося расчётного времени.
     */
    @FXML
    private Label labelEstimatedTime;

    /**
     * Контейнер предварительных результатов.
     */
    @FXML
    private VBox vboxPartialResults;

    /**
     * Разделитель — появляется когда есть хотя бы один результат.
     */
    @FXML
    private Separator separatorPartialResults;

    /**
     * Заголовок секции предварительных результатов.
     */
    @FXML
    private Label labelPartialResultsHeader;

    // ── Зависимости ───────────────────────────────────────────────────────

    private SimulationService simulationService;

    /**
     * Колбэк для передачи финального результата в MainController.
     */
    private SessionCompletedCallback sessionCompletedCallback;

    // ── Состояние ─────────────────────────────────────────────────────────

    /**
     * Индикаторы статуса по алгоритмам (алгоритм → строка-карточка).
     */
    private final Map<CompressionAlgorithm, AlgorithmStatusRow> statusRows = new HashMap<>();

    /**
     * Время старта симуляции (мс).
     */
    private long simulationStartMs;

    /**
     * Сколько алгоритмов завершено.
     */
    private int completedCount = 0;

    /**
     * Сколько алгоритмов запущено всего.
     */
    private int totalAlgorithms = 0;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── Инициализация ─────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureInitialState();
    }

    /**
     * Внедрение зависимостей — вызывается из MainController.
     */
    public void setSimulationService(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    public void setSessionCompletedCallback(SessionCompletedCallback callback) {
        this.sessionCompletedCallback = callback;
    }

    // ── Запуск симуляции ──────────────────────────────────────────────────

    /**
     * Запускает симуляцию с переданной конфигурацией.
     * Вызывается из MainController при нажатии «Запустить симуляцию».
     */
    public void startSimulation(SimulationConfig config) {
        prepareUiForStart(config);

        simulationService.runSimulationAsync(config, new SimulationProgressCallback() {

            @Override
            public void onProgress(double progress, String stepDescription) {
                Platform.runLater(() -> updateProgress(progress, stepDescription));
            }

            @Override
            public void onAlgorithmCompleted(SimulationResult result) {
                Platform.runLater(() -> handleAlgorithmCompleted(result));
            }

            @Override
            public void onCompleted(SimulationSession session) {
                Platform.runLater(() -> handleSimulationCompleted(session));
            }

            @Override
            public void onError(Exception exception) {
                Platform.runLater(() -> handleSimulationError(exception));
            }

            @Override
            public void onCancelled() {
                Platform.runLater(() -> handleSimulationCancelled());
            }
        });
    }

    // ── Обработчики событий симуляции ─────────────────────────────────────

    /**
     * Обновляет прогресс-бар и описание текущего шага.
     */
    private void updateProgress(double progress, String stepDescription) {
        progressBarOverall.setProgress(progress);
        labelCurrentStep.setText(stepDescription);
        labelProgressPercent.setText("%.0f%%".formatted(progress * 100));

        // Обновляем elapsed time
        long elapsedMs = System.currentTimeMillis() - simulationStartMs;
        labelElapsedTime.setText(formatDurationMs(elapsedMs));

        // Расчёт оставшегося времени
        if (progress > 0.05) {
            long estimatedTotalMs = (long) (elapsedMs / progress);
            long remainingMs = estimatedTotalMs - elapsedMs;
            labelEstimatedTime.setText("~" + formatDurationMs(remainingMs));
        }

        // Обновляем статус текущего алгоритма
        updateCurrentAlgorithmStatus(stepDescription);
    }

    /**
     * Обрабатывает завершение одного алгоритма:
     * — помечает строку как завершённую (зелёный индикатор)
     * — добавляет предварительный результат в таблицу
     */
    private void handleAlgorithmCompleted(SimulationResult result) {
        completedCount++;

        // Обновляем статус строки алгоритма
        AlgorithmStatusRow row = statusRows.get(result.getAlgorithm());
        if (row != null) {
            row.setStatus(AlgorithmStatus.COMPLETED);
            row.setSubtitle("Сжатие: %.2fx | Скорость: %.1f Мбит/с | Доставка: %.1f%%"
                    .formatted(
                            result.getCompressionRatio(),
                            result.getEffectiveTransferRateMbps(),
                            result.getDeliveryRatePercent()
                    )
            );
        }

        // Добавляем краткий результат в секцию предварительных данных
        addPartialResultRow(result);

        // Записываем в лог
        appendLog("✅ %s — завершён (коэф. сжатия: %.2fx, доставка: %.1f%%)".formatted(
                result.getAlgorithm().getDisplayName(),
                result.getCompressionRatio(),
                result.getDeliveryRatePercent()
        ));
    }

    /**
     * Обрабатывает успешное завершение всей симуляции.
     */
    private void handleSimulationCompleted(SimulationSession session) {
        // Финальное состояние прогресс-бара
        progressBarOverall.setProgress(1.0);
        labelProgressPercent.setText("100%");
        labelCurrentStep.setText("Симуляция успешно завершена");

        // Подсвечиваем лучший алгоритм
        session.getBestByEfficiencyScore().ifPresent(best -> {
            AlgorithmStatusRow row = statusRows.get(best.getAlgorithm());
            if (row != null) {
                row.markAsBest();
            }
        });

        appendLog("═══════════════════════════════════════");
        appendLog("🏁 Симуляция завершена!");
        appendLog("Лучший по интегральной оценке: %s (%.1f/100)".formatted(
                session.getBestByEfficiencyScore()
                        .map(r -> r.getAlgorithm().getDisplayName())
                        .orElse("—"),
                session.getBestByEfficiencyScore()
                        .map(SimulationResult::getOverallEfficiencyScore)
                        .orElse(0.0)
        ));
        appendLog("Всего времени: %s".formatted(
                formatDurationMs(session.getTotalDuration().toMillis())
        ));

        // Переключаем кнопку
        buttonCancel.setText("Закрыть");
        buttonCancel.setOnAction(e -> handleCloseAfterCompletion());
        buttonCancel.setDisable(false);

        // Передаём результаты в MainController → ResultsController
        if (sessionCompletedCallback != null) {
            sessionCompletedCallback.onSessionCompleted(session);
        }
    }

    /**
     * Обрабатывает ошибку симуляции.
     */
    private void handleSimulationError(Exception exception) {
        labelCurrentStep.setText("Ошибка: " + exception.getMessage());
        progressBarOverall.setStyle("-fx-accent: #e74c3c;"); // Красный

        appendLog("❌ ОШИБКА: " + exception.getMessage());

        buttonCancel.setText("Закрыть");
        buttonCancel.setDisable(false);

        showErrorAlert(exception);
    }

    /**
     * Обрабатывает отмену симуляции пользователем.
     */
    private void handleSimulationCancelled() {
        labelCurrentStep.setText("Симуляция отменена пользователем");
        progressBarOverall.setStyle("-fx-accent: #f39c12;"); // Оранжевый

        appendLog("⚠️ Симуляция отменена.");

        buttonCancel.setText("Закрыть");
        buttonCancel.setOnAction(e -> handleCloseAfterCompletion());
        buttonCancel.setDisable(false);
    }

    // ── FXML-обработчики ──────────────────────────────────────────────────

    @FXML
    private void handleCancelButtonClick() {
        if (simulationService.isRunning()) {
            // Запрашиваем подтверждение отмены
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Отмена симуляции");
            alert.setHeaderText("Прервать текущую симуляцию?");
            alert.setContentText("Все несохранённые результаты будут потеряны.");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    simulationService.cancelSimulation();
                    buttonCancel.setDisable(true);
                    buttonCancel.setText("Отмена...");
                    appendLog("⏹ Запрос на отмену отправлен...");
                }
            });
        }
    }

    @FXML
    private void handleClearLog() {
        textAreaLog.clear();
    }

    // ── Формирование UI ───────────────────────────────────────────────────

    /**
     * Подготавливает UI к запуску симуляции:
     * создаёт строки статуса для каждого выбранного алгоритма,
     * сбрасывает прогресс и лог.
     */
    private void prepareUiForStart(SimulationConfig config) {
        // Сброс состояния
        statusRows.clear();
        vboxAlgorithmStatuses.getChildren().clear();
        vboxPartialResults.getChildren().clear();
        textAreaLog.clear();
        completedCount = 0;
        simulationStartMs = System.currentTimeMillis();

        totalAlgorithms = config.getAlgorithmsToCompare().size();

        // Сброс прогресса
        progressBarOverall.setProgress(0.0);
        progressBarOverall.setStyle(""); // Сбрасываем кастомные стили (ошибка/отмена)
        labelProgressPercent.setText("0%");
        labelCurrentStep.setText("Подготовка...");
        labelElapsedTime.setText("00:00");
        labelEstimatedTime.setText("—");

        // Скрываем секцию предварительных результатов
        separatorPartialResults.setVisible(false);
        labelPartialResultsHeader.setVisible(false);

        // Создаём строки статуса для каждого алгоритма
        for (CompressionAlgorithm algorithm : config.getAlgorithmsToCompare()) {
            AlgorithmStatusRow row = new AlgorithmStatusRow(algorithm);
            statusRows.put(algorithm, row);
            vboxAlgorithmStatuses.getChildren().add(row.getNode());
        }

        // Кнопка отмены активна
        buttonCancel.setText("Отменить");
        buttonCancel.setDisable(false);
        buttonCancel.setOnAction(e -> handleCancelButtonClick());

        // Лог
        appendLog("🚀 Симуляция запущена: " + config.getSimulationName());
        appendLog("Алгоритмов: %d | Данные: %s (%s) | Канал: %s | Протокол: %s".formatted(
                totalAlgorithms,
                config.getDataType().getDisplayName(),
                formatBytes(config.getDataSizeBytes()),
                config.getChannelParameters().getChannelType().getDisplayName(),
                config.getProtocol().getDisplayName()
        ));
        appendLog("───────────────────────────────────────");
    }

    /**
     * Пытается определить, какой алгоритм сейчас обрабатывается,
     * и устанавливает ему статус RUNNING.
     */
    private void updateCurrentAlgorithmStatus(String stepDescription) {
        for (Map.Entry<CompressionAlgorithm, AlgorithmStatusRow> entry : statusRows.entrySet()) {
            CompressionAlgorithm algorithm = entry.getKey();
            AlgorithmStatusRow row = entry.getValue();

            if (stepDescription.contains(algorithm.getDisplayName())
                    && row.getStatus() == AlgorithmStatus.WAITING) {
                row.setStatus(AlgorithmStatus.RUNNING);
                appendLog("▶ Обработка: %s...".formatted(algorithm.getDisplayName()));
                break;
            }
        }
    }

    /**
     * Добавляет строку предварительного результата после завершения алгоритма.
     */
    private void addPartialResultRow(SimulationResult result) {
        // Показываем секцию при первом результате
        if (!separatorPartialResults.isVisible()) {
            separatorPartialResults.setVisible(true);
            labelPartialResultsHeader.setVisible(true);
        }

        HBox row = new HBox(12);
        row.setPadding(new Insets(4, 8, 4, 8));
        row.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 4;");

        Label lblAlgorithm = new Label(result.getAlgorithm().getDisplayName());
        lblAlgorithm.setMinWidth(140);
        lblAlgorithm.setStyle("-fx-font-weight: bold;");

        Label lblRatio = new Label("%.2fx".formatted(result.getCompressionRatio()));
        lblRatio.setMinWidth(60);
        lblRatio.setStyle("-fx-text-fill: #2980b9;");

        Label lblSpeed = new Label("%.1f Мбит/с".formatted(result.getEffectiveTransferRateMbps()));
        lblSpeed.setMinWidth(100);
        lblSpeed.setStyle("-fx-text-fill: #27ae60;");

        Label lblDelivery = new Label("%.1f%%".formatted(result.getDeliveryRatePercent()));
        lblDelivery.setMinWidth(60);
        boolean goodDelivery = result.getDeliveryRatePercent() >= 95.0;
        lblDelivery.setStyle("-fx-text-fill: " + (goodDelivery ? "#27ae60" : "#e74c3c") + ";");

        Label lblIntegrity = new Label(result.isDataIntegrityOk() ? "✅" : "❌");

        row.getChildren().addAll(lblAlgorithm, lblRatio, lblSpeed, lblDelivery, lblIntegrity);
        vboxPartialResults.getChildren().add(row);
    }

    private void handleCloseAfterCompletion() {
        // Логика закрытия / перехода на вкладку результатов
        // реализуется через MainController
    }

    // ── Утилиты ───────────────────────────────────────────────────────────

    private void configureInitialState() {
        progressBarOverall.setProgress(0.0);
        labelCurrentStep.setText("Ожидание запуска...");
        labelProgressPercent.setText("0%");
        labelElapsedTime.setText("—");
        labelEstimatedTime.setText("—");
        separatorPartialResults.setVisible(false);
        labelPartialResultsHeader.setVisible(false);
    }

    private void appendLog(String message) {
        String timestamp = java.time.LocalTime.now().format(TIME_FORMAT);
        textAreaLog.appendText("[%s] %s\n".formatted(timestamp, message));
    }

    private void showErrorAlert(Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка симуляции");
        alert.setHeaderText("Симуляция завершилась с ошибкой");
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }

    private String formatDurationMs(long durationMs) {
        if (durationMs < 0) durationMs = 0;
        long seconds = (durationMs / 1000) % 60;
        long minutes = durationMs / 60_000;
        return "%02d:%02d".formatted(minutes, seconds);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " Б";
        if (bytes < 1024 * 1024) return "%.1f КБ".formatted(bytes / 1024.0);
        return "%.1f МБ".formatted(bytes / (1024.0 * 1024.0));
    }

    // ── Вложенные типы ────────────────────────────────────────────────────

    /**
     * Статус алгоритма в процессе симуляции.
     */
    private enum AlgorithmStatus {
        WAITING, RUNNING, COMPLETED, ERROR
    }

    /**
     * Визуальная строка-карточка статуса одного алгоритма.
     * Содержит цветной индикатор, название и подпись с метриками.
     */
    private static class AlgorithmStatusRow {

        private final CompressionAlgorithm algorithm;
        private AlgorithmStatus status = AlgorithmStatus.WAITING;

        private final Circle indicator;
        private final Label labelTitle;
        private final Label labelSubtitle;
        private final HBox node;

        AlgorithmStatusRow(CompressionAlgorithm algorithm) {
            this.algorithm = algorithm;

            indicator = new Circle(7);
            indicator.setFill(Color.LIGHTGRAY); // WAITING

            labelTitle = new Label(algorithm.getDisplayName());
            labelTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");

            labelSubtitle = new Label("Ожидание...");
            labelSubtitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11;");

            VBox textBlock = new VBox(2, labelTitle, labelSubtitle);

            node = new HBox(10, indicator, textBlock);
            node.setPadding(new Insets(6, 12, 6, 12));
            node.setStyle("""
                -fx-background-color: white;
                -fx-background-radius: 6;
                -fx-border-color: #ecf0f1;
                -fx-border-radius: 6;
                -fx-border-width: 1;
                """);
        }

        void setStatus(AlgorithmStatus newStatus) {
            this.status = newStatus;
            switch (newStatus) {
                case WAITING   -> {
                    indicator.setFill(Color.LIGHTGRAY);
                    labelSubtitle.setText("Ожидание...");
                }
                case RUNNING   -> {
                    indicator.setFill(Color.ORANGE);
                    labelSubtitle.setText("Выполняется...");
                    node.setStyle("""
                        -fx-background-color: #fffde7;
                        -fx-background-radius: 6;
                        -fx-border-color: #f39c12;
                        -fx-border-radius: 6;
                        -fx-border-width: 1;
                        """);
                }
                case COMPLETED -> {
                    indicator.setFill(Color.web("#27ae60"));
                    node.setStyle("""
                        -fx-background-color: #f0fff4;
                        -fx-background-radius: 6;
                        -fx-border-color: #27ae60;
                        -fx-border-radius: 6;
                        -fx-border-width: 1;
                        """);
                }
                case ERROR     -> {
                    indicator.setFill(Color.web("#e74c3c"));
                    labelSubtitle.setText("Ошибка");
                    node.setStyle("""
                        -fx-background-color: #fff5f5;
                        -fx-background-radius: 6;
                        -fx-border-color: #e74c3c;
                        -fx-border-radius: 6;
                        -fx-border-width: 1;
                        """);
                }
            }
        }

        void setSubtitle(String text) {
            labelSubtitle.setText(text);
        }

        /**
         * Добавляет визуальное выделение лучшего алгоритма.
         */
        void markAsBest() {
            node.setStyle("""
                -fx-background-color: #e8f5e9;
                -fx-background-radius: 6;
                -fx-border-color: #2ecc71;
                -fx-border-radius: 6;
                -fx-border-width: 2;
                """);
            labelTitle.setText("🏆 " + algorithm.getDisplayName());
        }

        HBox getNode()             { return node; }
        AlgorithmStatus getStatus(){ return status; }
    }

    // ── Колбэк завершения сессии ──────────────────────────────────────────

    /**
     * Колбэк для передачи готовой сессии в MainController.
     */
    @FunctionalInterface
    public interface SessionCompletedCallback {
        void onSessionCompleted(SimulationSession session);
    }

}
