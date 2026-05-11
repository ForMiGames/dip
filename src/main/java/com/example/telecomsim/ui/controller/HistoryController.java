package com.example.telecomsim.ui.controller;

import com.example.telecomsim.model.channel.ChannelType;
import com.example.telecomsim.model.compression.CompressionAlgorithm;
import com.example.telecomsim.model.metrics.SimulationResult;
import com.example.telecomsim.model.metrics.SimulationSession;
import com.example.telecomsim.model.simulation.SimulationConfig;
import com.example.telecomsim.service.history.SimulationHistoryService;
import com.example.telecomsim.util.CsvExporter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;

/**
 * Контроллер вкладки «История симуляций».
 *
 * Функции:
 *  — Отображение всех сохранённых сессий симуляции
 *  — Фильтрация по названию, типу канала, алгоритму
 *  — Просмотр детального результата выбранной сессии
 *  — Экспорт истории в CSV
 *  — Удаление отдельных записей и очистка истории
 *  — Повторный запуск симуляции с сохранёнными параметрами
 */
public class HistoryController  implements Initializable {
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    // ── FXML-компоненты ───────────────────────────────────────────────────

    // -- Панель фильтров --

    @FXML
    private TextField fieldSearch;
    @FXML private ComboBox<String> comboFilterChannel;
    @FXML private ComboBox<String> comboFilterAlgorithm;
    @FXML private Button buttonClearFilters;

    // -- Таблица сессий --

    @FXML private TableView<SessionRow> tableHistory;
    @FXML private TableColumn<SessionRow, String> colDate;
    @FXML private TableColumn<SessionRow, String> colName;
    @FXML private TableColumn<SessionRow, String> colChannel;
    @FXML private TableColumn<SessionRow, String> colAlgorithms;
    @FXML private TableColumn<SessionRow, String> colDataType;
    @FXML private TableColumn<SessionRow, String> colDataSize;
    @FXML private TableColumn<SessionRow, String> colDuration;
    @FXML private TableColumn<SessionRow, String> colBestAlgorithm;

    // -- Панель действий --

    @FXML private Button buttonViewDetails;
    @FXML private Button buttonRerun;
    @FXML private Button buttonDeleteSelected;
    @FXML private Button buttonExportCsv;
    @FXML private Button buttonClearHistory;

    // -- Детали выбранной сессии --

    @FXML private TitledPane titledPaneDetails;
    @FXML private TableView<AlgorithmResultRow> tableDetails;
    @FXML private TableColumn<AlgorithmResultRow, String> colDetailAlgorithm;
    @FXML private TableColumn<AlgorithmResultRow, String> colDetailRatio;
    @FXML private TableColumn<AlgorithmResultRow, String> colDetailCompSpeed;
    @FXML private TableColumn<AlgorithmResultRow, String> colDetailDecompSpeed;
    @FXML private TableColumn<AlgorithmResultRow, String> colDetailTransferRate;
    @FXML private TableColumn<AlgorithmResultRow, String> colDetailDelivery;
    @FXML private TableColumn<AlgorithmResultRow, String> colDetailScore;
    @FXML private TableColumn<AlgorithmResultRow, String> colDetailIntegrity;

    // -- Статусная строка --

    @FXML private Label labelTotalSessions;
    @FXML private Label labelStatusMessage;

    // ── Зависимости ───────────────────────────────────────────────────────

    private SimulationHistoryService historyService;

    /** Колбэк для повторного запуска симуляции через MainController. */
    private RerunCallback rerunCallback;

    /** Колбэк для открытия деталей в ResultsController. */
    private ViewDetailsCallback viewDetailsCallback;

    // ── Данные ────────────────────────────────────────────────────────────

    private final ObservableList<SessionRow> allRows  = FXCollections.observableArrayList();
    private FilteredList<SessionRow> filteredRows;

    // ── Инициализация ─────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureTable();
        configureDetailTable();
        configureFilters();
        configureSelectionListener();
        setInitialButtonStates();
    }

    public void setHistoryService(SimulationHistoryService historyService) {
        this.historyService = historyService;
        loadHistory();
    }

    public void setRerunCallback(RerunCallback callback) {
        this.rerunCallback = callback;
    }

    public void setViewDetailsCallback(ViewDetailsCallback callback) {
        this.viewDetailsCallback = callback;
    }

    // ── Публичный API ─────────────────────────────────────────────────────

    /**
     * Добавляет новую сессию в историю и обновляет таблицу.
     * Вызывается из MainController после завершения симуляции.
     */
    public void addSession(SimulationSession session) {
        if (historyService != null) {
            historyService.save(session);
        }
        allRows.add(0, new SessionRow(session)); // Новые сверху
        updateStatusLabel();
        setStatusMessage("Добавлена новая запись: " + session.getConfig().getSimulationName());
    }

    /**
     * Перезагружает историю из сервиса.
     */
    public void refresh() {
        loadHistory();
    }

    // ── Конфигурация таблицы ──────────────────────────────────────────────

    private void configureTable() {
        colDate.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStartTime()));

        colName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        colChannel.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getChannelType()));

        colAlgorithms.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getAlgorithmsList()));

        colDataType.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDataType()));

        colDataSize.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDataSize()));

        colDuration.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDuration()));

        colBestAlgorithm.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getBestAlgorithm()));

        // Выделение лучшего алгоритма цветом
        colBestAlgorithm.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                }
            }
        });

        // Подключаем фильтрованный список
        filteredRows = new FilteredList<>(allRows, p -> true);
        tableHistory.setItems(filteredRows);
        tableHistory.setPlaceholder(new Label("История симуляций пуста"));

        // Множественный выбор
        tableHistory.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void configureDetailTable() {
        colDetailAlgorithm.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getAlgorithmName()));

        colDetailRatio.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCompressionRatio()));

        colDetailCompSpeed.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCompressionSpeed()));

        colDetailDecompSpeed.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDecompressionSpeed()));

        colDetailTransferRate.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTransferRate()));

        colDetailDelivery.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDeliveryRate()));

        colDetailScore.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEfficiencyScore()));

        colDetailIntegrity.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getIntegrity()));

        // Цвет для колонки целостности
        colDetailIntegrity.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle(item.equals("✅")
                            ? "-fx-text-fill: #27ae60;"
                            : "-fx-text-fill: #e74c3c;");
                }
            }
        });

        tableDetails.setPlaceholder(new Label("Выберите сессию для просмотра деталей"));
    }

    private void configureFilters() {
        // Поиск по названию
        fieldSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Фильтр по каналу
        comboFilterChannel.getItems().add("Все каналы");
        for (ChannelType type :
                ChannelType.values()) {
            comboFilterChannel.getItems().add(type.getDisplayName());
        }
        comboFilterChannel.setValue("Все каналы");
        comboFilterChannel.valueProperty().addListener((obs, o, n) -> applyFilters());

        // Фильтр по алгоритму
        comboFilterAlgorithm.getItems().add("Все алгоритмы");
        for (CompressionAlgorithm alg : CompressionAlgorithm.values()) {
            comboFilterAlgorithm.getItems().add(alg.getDisplayName());
        }
        comboFilterAlgorithm.setValue("Все алгоритмы");
        comboFilterAlgorithm.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void configureSelectionListener() {
        tableHistory.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    boolean hasSelection = newSelection != null;

                    buttonViewDetails.setDisable(!hasSelection);
                    buttonRerun.setDisable(!hasSelection);
                    buttonDeleteSelected.setDisable(
                            tableHistory.getSelectionModel().getSelectedItems().isEmpty()
                    );

                    if (hasSelection) {
                        showSessionDetails(newSelection.getSession());
                    } else {
                        tableDetails.getItems().clear();
                        titledPaneDetails.setText("Детали сессии");
                    }
                }
        );
    }

    // ── FXML-обработчики ──────────────────────────────────────────────────

    @FXML
    private void handleViewDetails() {
        SessionRow selected = tableHistory.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (viewDetailsCallback != null) {
            viewDetailsCallback.onViewDetails(selected.getSession());
        }
    }

    @FXML
    private void handleRerun() {
        SessionRow selected = tableHistory.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (rerunCallback != null) {
            rerunCallback.onRerun(selected.getSession().getConfig());
        }
    }

    @FXML
    private void handleDeleteSelected() {
        List<SessionRow> selected = List.copyOf(
                tableHistory.getSelectionModel().getSelectedItems()
        );
        if (selected.isEmpty()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Удаление записей");
        confirm.setHeaderText("Удалить выбранные записи (%d)?".formatted(selected.size()));
        confirm.setContentText("Это действие нельзя отменить.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                selected.forEach(row -> {
                    allRows.remove(row);
                    if (historyService != null) {
                        historyService.delete(row.getSession().getSessionId());
                    }
                });
                tableDetails.getItems().clear();
                updateStatusLabel();
                setStatusMessage("Удалено записей: " + selected.size());
            }
        });
    }

    @FXML
    private void handleExportCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Экспорт истории в CSV");
        fileChooser.setInitialFileName("simulation_history.csv");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV файлы", "*.csv")
        );

        File file = fileChooser.showSaveDialog(tableHistory.getScene().getWindow());
        if (file == null) return;

        try {
            exportToCsv(file);
            setStatusMessage("История экспортирована: " + file.getName());
        } catch (Exception e) {
            showError("Ошибка экспорта", e.getMessage());
        }
    }

    @FXML
    private void handleClearHistory() {
        if (allRows.isEmpty()) {
            setStatusMessage("История уже пуста");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Очистка истории");
        confirm.setHeaderText("Очистить всю историю симуляций?");
        confirm.setContentText("Все %d записей будут удалены. Это действие нельзя отменить."
                .formatted(allRows.size()));

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                allRows.clear();
                tableDetails.getItems().clear();
                if (historyService != null) {
                    historyService.deleteAll();
                }
                updateStatusLabel();
                setStatusMessage("История очищена");
            }
        });
    }

    @FXML
    private void handleClearFilters() {
        fieldSearch.clear();
        comboFilterChannel.setValue("Все каналы");
        comboFilterAlgorithm.setValue("Все алгоритмы");
        setStatusMessage("Фильтры сброшены");
    }

    // ── Фильтрация ────────────────────────────────────────────────────────

    private void applyFilters() {
        String searchText     = fieldSearch.getText().toLowerCase().trim();
        String channelFilter  = comboFilterChannel.getValue();
        String algorithmFilter = comboFilterAlgorithm.getValue();

        filteredRows.setPredicate(buildPredicate(searchText, channelFilter, algorithmFilter));
        updateStatusLabel();
    }

    private Predicate<SessionRow> buildPredicate(String search,
                                                 String channel,
                                                 String algorithm) {
        return row -> {
            // Фильтр по поиску
            if (!search.isEmpty()) {
                boolean matchesName    = row.getName().toLowerCase().contains(search);
                boolean matchesChannel = row.getChannelType().toLowerCase().contains(search);
                boolean matchesAlg     = row.getAlgorithmsList().toLowerCase().contains(search);
                if (!matchesName && !matchesChannel && !matchesAlg) return false;
            }

            // Фильтр по каналу
            if (!"Все каналы".equals(channel)) {
                if (!row.getChannelType().equals(channel)) return false;
            }

            // Фильтр по алгоритму
            if (!"Все алгоритмы".equals(algorithm)) {
                if (!row.getAlgorithmsList().contains(algorithm)) return false;
            }

            return true;
        };
    }

    // ── Детали сессии ─────────────────────────────────────────────────────

    private void showSessionDetails(SimulationSession session) {
        titledPaneDetails.setText("Детали: " + session.getConfig().getSimulationName());

        ObservableList<AlgorithmResultRow> detailRows = FXCollections.observableArrayList();
        for (SimulationResult result : session.getResults()) {
            detailRows.add(new AlgorithmResultRow(result));
        }
        tableDetails.setItems(detailRows);
    }

    // ── Загрузка и экспорт ────────────────────────────────────────────────

    private void loadHistory() {
        allRows.clear();
        if (historyService != null) {
            historyService.loadAll().stream()
                    .map(SessionRow::new)
                    .forEach(allRows::add);
        }
        updateStatusLabel();
    }

    private void exportToCsv(File file) throws IOException {
        StringBuilder csv = new StringBuilder();

        // Заголовок
        csv.append(CsvExporter.buildHeader(
                "Дата и время",
                "Название симуляции",
                "Канал",
                "Алгоритмы",
                "Тип данных",
                "Размер данных",
                "Длительность",
                "Лучший алгоритм"
        ));

        // Данные
        for (SessionRow row : filteredRows) {
            csv.append(CsvExporter.buildRow(
                    row.getStartTime(),
                    row.getName(),
                    row.getChannelType(),
                    row.getAlgorithmsList(),
                    row.getDataType(),
                    row.getDataSize(),
                    row.getDuration(),
                    row.getBestAlgorithm()
            ));
        }

        CsvExporter.write(file, csv.toString());
    }

    // ── Утилиты ───────────────────────────────────────────────────────────

    private void setInitialButtonStates() {
        buttonViewDetails.setDisable(true);
        buttonRerun.setDisable(true);
        buttonDeleteSelected.setDisable(true);
    }

    private void updateStatusLabel() {
        int total    = allRows.size();
        int filtered = filteredRows.size();
        if (total == filtered) {
            labelTotalSessions.setText("Всего записей: " + total);
        } else {
            labelTotalSessions.setText("Показано: %d из %d".formatted(filtered, total));
        }
    }

    private void setStatusMessage(String message) {
        labelStatusMessage.setText(message);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024)         return bytes + " Б";
        if (bytes < 1024 * 1024)  return "%.1f КБ".formatted(bytes / 1024.0);
        return "%.1f МБ".formatted(bytes / (1024.0 * 1024.0));
    }

    // ── Вложенные модели строк таблицы ────────────────────────────────────

    /**
     * Модель строки таблицы истории.
     * Конвертирует SimulationSession в строки для отображения.
     */
    public static class SessionRow {

        private final SimulationSession session;

        SessionRow(SimulationSession session) {
            this.session = session;
        }

        public SimulationSession getSession() { return session; }

        public String getStartTime() {
            return session.getStartTime() != null
                    ? session.getStartTime().format(DISPLAY_FORMAT)
                    : "—";
        }

        public String getName() {
            return session.getConfig().getSimulationName();
        }

        public String getChannelType() {
            return session.getConfig().getChannelParameters()
                    .getChannelType().getDisplayName();
        }

        public String getAlgorithmsList() {
            return session.getConfig().getAlgorithmsToCompare().stream()
                    .map(CompressionAlgorithm::getDisplayName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("—");
        }

        public String getDataType() {
            return session.getConfig().getDataType().getDisplayName();
        }

        public String getDataSize() {
            long bytes = session.getConfig().getDataSizeBytes();
            if (bytes < 1024)         return bytes + " Б";
            if (bytes < 1024 * 1024)  return "%.0f КБ".formatted(bytes / 1024.0);
            return "%.1f МБ".formatted(bytes / (1024.0 * 1024));
        }

        public String getDuration() {
            long ms = session.getTotalDuration().toMillis();
            long s  = (ms / 1000) % 60;
            long m  = ms / 60_000;
            return "%02d:%02d".formatted(m, s);
        }

        public String getBestAlgorithm() {
            return session.getBestByEfficiencyScore()
                    .map(r -> r.getAlgorithm().getDisplayName())
                    .orElse("—");
        }
    }

    /**
     * Модель строки таблицы деталей.
     */
    public static class AlgorithmResultRow {

        private final SimulationResult result;

        AlgorithmResultRow(SimulationResult result) {
            this.result = result;
        }

        public String getAlgorithmName()    { return result.getAlgorithm().getDisplayName(); }
        public String getCompressionRatio() { return "%.3f×".formatted(result.getCompressionRatio()); }
        public String getCompressionSpeed() { return "%.1f Мбит/с".formatted(result.getCompressionSpeedMbps()); }
        public String getDecompressionSpeed() { return "%.1f Мбит/с".formatted(result.getDecompressionSpeedMbps()); }
        public String getTransferRate()     { return "%.2f Мбит/с".formatted(result.getEffectiveTransferRateMbps()); }
        public String getDeliveryRate()     { return "%.2f%%".formatted(result.getDeliveryRatePercent()); }
        public String getEfficiencyScore()  { return "%.1f/100".formatted(result.getOverallEfficiencyScore()); }
        public String getIntegrity()        { return result.isDataIntegrityOk() ? "✅" : "❌"; }
    }

    // ── Колбэки ───────────────────────────────────────────────────────────

    @FunctionalInterface
    public interface RerunCallback {
        void onRerun(SimulationConfig config);
    }

    @FunctionalInterface
    public interface ViewDetailsCallback {
        void onViewDetails(SimulationSession session);
    }
}
