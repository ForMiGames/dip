package com.example.telecomsim.ui.controller;

import com.example.telecomsim.model.channel.ChannelParameters;
import com.example.telecomsim.model.channel.ChannelPreset;
import com.example.telecomsim.model.channel.ChannelPresetRegistry;
import com.example.telecomsim.model.channel.ChannelType;
import com.example.telecomsim.model.compression.CompressionAlgorithm;
import com.example.telecomsim.model.compression.CompressionSettings;
import com.example.telecomsim.model.simulation.DataType;
import com.example.telecomsim.model.simulation.SimulationConfig;
import com.example.telecomsim.model.simulation.TransmissionProtocol;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Контроллер вкладки «Настройка симуляции».
 *
 * Отвечает за:
 *  — Выбор алгоритмов сжатия
 *  — Настройку параметров канала (пресеты + ручная настройка)
 *  — Выбор типа и размера входных данных
 *  — Выбор протокола и числа повторений
 *  — Валидацию и сборку SimulationConfig
 *  — Передачу конфига в MainController через колбэк
 */
public class SimulationSetupController implements Initializable {

    // ── FXML: Алгоритмы ──────────────────────────────────────────────────

    @FXML
    private CheckBox chkHuffman;
    @FXML private CheckBox chkLZ77;
    @FXML private CheckBox chkDeflate;
    @FXML private CheckBox chkZstandard;
    @FXML private CheckBox chkLZ4;
    @FXML private CheckBox chkSnappy;
    @FXML private CheckBox chkNone;

    // ── FXML: Канал ───────────────────────────────────────────────────────

    @FXML private ComboBox<ChannelPreset> cmbPreset;
    @FXML private ComboBox<ChannelType>   cmbChannelType;

    @FXML private Slider sldBandwidth;
    @FXML private Label  lblBandwidthValue;

    @FXML private Slider sldLatency;
    @FXML private Label  lblLatencyValue;

    @FXML private Slider sldJitter;
    @FXML private Label  lblJitterValue;

    @FXML private Slider sldBer;
    @FXML private Label  lblBerValue;

    @FXML private Slider sldPacketLoss;
    @FXML private Label  lblPacketLossValue;

    @FXML private TextField txtMtu;
    @FXML private TextField txtDistance;

    @FXML private Slider sldSnr;
    @FXML private Label  lblSnrValue;

    // ── FXML: Входные данные ──────────────────────────────────────────────

    @FXML private ComboBox<DataType> cmbDataType;
    @FXML private ComboBox<String>   cmbDataSize;
    @FXML private TextField          txtCustomSize;
    @FXML private Button             btnSelectFile;
    @FXML private Label              lblSelectedFile;

    // ── FXML: Параметры симуляции ─────────────────────────────────────────

    @FXML private RadioButton      radioTcp;
    @FXML private RadioButton      radioUdp;
    @FXML private ToggleGroup      tglProtocol;
    @FXML private Spinner<Integer> spnRepetitions;
    @FXML private TextField        txtSimulationName;

    // ── FXML: Управление ─────────────────────────────────────────────────

    @FXML private Button btnStart;
    @FXML private Label  lblValidationError;

    // ── Состояние ─────────────────────────────────────────────────────────

    /** Путь к файлу пользователя — заполняется при выборе файла. */
    private Path selectedFilePath;

    /**
     * Колбэк — вызывается при нажатии «Запустить».
     * MainController подписывается на него при инициализации.
     */
    private StartSimulationCallback startCallback;

    // ── Инициализация ─────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureAlgorithmCheckboxes();
        configureChannelPresets();
        configureChannelTypeCombo();
        configureChannelSliders();
        configureDataTypeCombo();
        configureDataSizeCombo();
        configureProtocol();
        configureRepetitionsSpinner();
        setDefaultSelections();
    }

    // ── Публичный API для MainController ─────────────────────────────────

    /**
     * Устанавливает колбэк, который будет вызван при нажатии «Запустить».
     */
    public void setStartCallback(StartSimulationCallback callback) {
        this.startCallback = callback;
    }

    /**
     * Восстанавливает UI из существующего конфига.
     * Используется при повторном запуске из истории.
     */
    public void applyConfig(SimulationConfig config) {
        // Алгоритмы
        List<CompressionAlgorithm> algs = config.getAlgorithmsToCompare();
        chkHuffman.setSelected(algs.contains(CompressionAlgorithm.HUFFMAN));
        chkLZ77.setSelected(algs.contains(CompressionAlgorithm.LZ77));
        chkDeflate.setSelected(algs.contains(CompressionAlgorithm.DEFLATE));
        chkZstandard.setSelected(algs.contains(CompressionAlgorithm.ZSTANDARD));
        chkLZ4.setSelected(algs.contains(CompressionAlgorithm.LZ4));
        chkSnappy.setSelected(algs.contains(CompressionAlgorithm.SNAPPY));
        chkNone.setSelected(algs.contains(CompressionAlgorithm.NONE));

        // Данные
        cmbDataType.setValue(config.getDataType());
        spnRepetitions.getValueFactory().setValue(config.getRepetitions());

        // Протокол
        if (config.getProtocol() == TransmissionProtocol.TCP) {
            radioTcp.setSelected(true);
        } else {
            radioUdp.setSelected(true);
        }

        // Канал
        applyChannelParametersToSliders(config.getChannelParameters());

        validateAndRefreshStartButton();
    }

    // ── Конфигурация компонентов ──────────────────────────────────────────

    private void configureAlgorithmCheckboxes() {
        // При любом изменении пересчитываем валидность кнопки «Запустить»
        List.of(chkHuffman, chkLZ77, chkDeflate, chkZstandard, chkLZ4, chkSnappy, chkNone)
                .forEach(chk -> chk.selectedProperty()
                        .addListener((obs, o, n) -> validateAndRefreshStartButton()));
    }

    private void configureChannelPresets() {
        List<ChannelPreset> presets = ChannelPresetRegistry.getAllPresets();
        cmbPreset.getItems().addAll(presets);

        cmbPreset.setConverter(new StringConverter<>() {
            @Override public String toString(ChannelPreset p) {
                return p == null ? "" : p.toString();
            }
            @Override public ChannelPreset fromString(String s) { return null; }
        });

        // Применяем параметры пресета при выборе
        cmbPreset.valueProperty().addListener((obs, old, preset) -> {
            if (preset != null) {
                applyChannelParametersToSliders(preset.getParameters());
            }
        });

        // Первый пресет по умолчанию
        cmbPreset.setValue(presets.get(0));
    }

    private void configureChannelTypeCombo() {
        cmbChannelType.getItems().addAll(ChannelType.values());

        cmbChannelType.setConverter(new StringConverter<>() {
            @Override public String toString(ChannelType t) {
                return t == null ? "" : t.getDisplayName();
            }
            @Override public ChannelType fromString(String s) { return null; }
        });
    }

    private void configureChannelSliders() {
        // ── Пропускная способность ──
        // Позиция 1..100 → кусочно-линейное преобразование в bps
        sldBandwidth.setMin(1);
        sldBandwidth.setMax(100);
        sldBandwidth.valueProperty().addListener((obs, o, n) ->
                lblBandwidthValue.setText(
                        formatBandwidth(sliderToBandwidthBps(n.doubleValue()))
                )
        );

        // ── Задержка: 0..600 мс ──
        sldLatency.setMin(0);
        sldLatency.setMax(600);
        sldLatency.valueProperty().addListener((obs, o, n) ->
                lblLatencyValue.setText((int) n.doubleValue() + " мс")
        );

        // ── Джиттер: 0..100 мс ──
        sldJitter.setMin(0);
        sldJitter.setMax(100);
        sldJitter.valueProperty().addListener((obs, o, n) ->
                lblJitterValue.setText((int) n.doubleValue() + " мс")
        );

        // ── BER: логарифмическая шкала ──
        // Позиция 0..12 → BER = 10^(-position)
        // 0 → BER=1e0 (очень плохой), 12 → BER=1e-12 (отличный)
        sldBer.setMin(0);
        sldBer.setMax(12);
        sldBer.valueProperty().addListener((obs, o, n) ->
                lblBerValue.setText("1e-%.0f".formatted(n.doubleValue()))
        );

        // ── Потеря пакетов: 0..20 % ──
        sldPacketLoss.setMin(0);
        sldPacketLoss.setMax(20);
        sldPacketLoss.valueProperty().addListener((obs, o, n) ->
                lblPacketLossValue.setText("%.2f%%".formatted(n.doubleValue()))
        );

        // ── SNR: 0..60 дБ ──
        sldSnr.setMin(0);
        sldSnr.setMax(60);
        sldSnr.valueProperty().addListener((obs, o, n) ->
                lblSnrValue.setText("%.1f дБ".formatted(n.doubleValue()))
        );
    }

    private void configureDataTypeCombo() {
        cmbDataType.getItems().addAll(DataType.values());

        cmbDataType.setConverter(new StringConverter<>() {
            @Override public String toString(DataType dt) {
                return dt == null ? "" : dt.getDisplayName();
            }
            @Override public DataType fromString(String s) { return null; }
        });

        // Показываем кнопку выбора файла только для REAL_FILE
        cmbDataType.valueProperty().addListener((obs, old, newType) -> {
            boolean isFile = newType == DataType.REAL_FILE;
            btnSelectFile.setVisible(isFile);
            btnSelectFile.setManaged(isFile);
            lblSelectedFile.setVisible(isFile);
            lblSelectedFile.setManaged(isFile);

            // Сбрасываем путь при смене типа
            if (!isFile) {
                selectedFilePath = null;
                lblSelectedFile.setText("Файл не выбран");
            }

            validateAndRefreshStartButton();
        });
    }

    private void configureDataSizeCombo() {
        cmbDataSize.getItems().addAll(
                "1 КБ", "10 КБ", "100 КБ", "1 МБ", "10 МБ", "50 МБ", "Свой размер"
        );

        cmbDataSize.valueProperty().addListener((obs, o, n) -> {
            boolean isCustom = "Свой размер".equals(n);
            txtCustomSize.setVisible(isCustom);
            txtCustomSize.setManaged(isCustom);
        });

        txtCustomSize.setVisible(false);
        txtCustomSize.setManaged(false);
        txtCustomSize.setPromptText("Введите размер в байтах");
    }

    private void configureProtocol() {
        // tglProtocol связывается с RadioButton через FXML toggleGroup
        radioTcp.setSelected(true);
    }

    private void configureRepetitionsSpinner() {
        spnRepetitions.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 3)
        );
        spnRepetitions.setEditable(true);
    }

    private void setDefaultSelections() {
        // Алгоритмы по умолчанию
        chkDeflate.setSelected(true);
        chkZstandard.setSelected(true);
        chkLZ4.setSelected(true);
        chkSnappy.setSelected(true);

        // Тип данных
        cmbDataType.setValue(DataType.TEXT_UTF8);
        cmbDataSize.setValue("1 МБ");

        // Применяем первый пресет (он уже выбран — сработает listener)
        // Но lblValue-labels надо инициализировать вручную
        // (listener не срабатывает при setValue до первого изменения)
        refreshAllSliderLabels();

        validateAndRefreshStartButton();
    }

    // ── FXML-обработчики ──────────────────────────────────────────────────

    @FXML
    private void handleStartSimulation() {
        List<String> errors = validate();
        if (!errors.isEmpty()) {
            showValidationError(String.join("\n", errors));
            return;
        }

        lblValidationError.setText("");
        lblValidationError.setVisible(false);

        if (startCallback != null) {
            startCallback.onStart(buildConfig());
        }
    }

    @FXML
    private void handleSelectFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Выбрать файл для симуляции");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );

        File file = fc.showOpenDialog(btnSelectFile.getScene().getWindow());
        if (file != null) {
            selectedFilePath = file.toPath();
            lblSelectedFile.setText(
                    "%s  (%s)".formatted(file.getName(), formatBytes(file.length()))
            );
            validateAndRefreshStartButton();
        }
    }

    @FXML
    private void handleResetSettings() {
        // Алгоритмы
        chkHuffman.setSelected(false);
        chkLZ77.setSelected(false);
        chkDeflate.setSelected(true);
        chkZstandard.setSelected(true);
        chkLZ4.setSelected(true);
        chkSnappy.setSelected(true);
        chkNone.setSelected(false);

        // Данные
        cmbDataType.setValue(DataType.TEXT_UTF8);
        cmbDataSize.setValue("1 МБ");
        txtCustomSize.clear();
        txtSimulationName.clear();
        selectedFilePath = null;
        lblSelectedFile.setText("Файл не выбран");

        // Протокол
        radioTcp.setSelected(true);
        spnRepetitions.getValueFactory().setValue(3);

        // Канал — первый пресет
        ChannelPreset first = ChannelPresetRegistry.getAllPresets().get(0);
        cmbPreset.setValue(first);
        // listener уже вызовет applyChannelParametersToSliders

        lblValidationError.setText("");
        lblValidationError.setVisible(false);
    }

    // ── Применение параметров канала к слайдерам ──────────────────────────

    /**
     * Заполняет все слайдеры и поля значениями из ChannelParameters.
     * Вызывается при выборе пресета или восстановлении конфига.
     */
    private void applyChannelParametersToSliders(ChannelParameters p) {
        cmbChannelType.setValue(p.getChannelType());
        sldBandwidth.setValue(bandwidthBpsToSlider(p.getBandwidthBps()));
        sldLatency.setValue(clamp(p.getLatencyMs(), sldLatency));
        sldJitter.setValue(clamp(p.getJitterMs(), sldJitter));
        sldBer.setValue(berToSlider(p.getBitErrorRate()));
        sldPacketLoss.setValue(clamp(p.getPacketLossPercent(), sldPacketLoss));
        txtMtu.setText(String.valueOf(p.getMtuBytes()));
        txtDistance.setText("%.1f".formatted(p.getDistanceKm()));
        sldSnr.setValue(clamp(p.getSignalToNoiseRatioDb(), sldSnr));

        refreshAllSliderLabels();
    }

    /**
     * Принудительно обновляет все Label-значения слайдеров.
     * Нужно при первом запуске — до первого изменения слайдера listener не срабатывает.
     */
    private void refreshAllSliderLabels() {
        lblBandwidthValue.setText(
                formatBandwidth(sliderToBandwidthBps(sldBandwidth.getValue()))
        );
        lblLatencyValue.setText((int) sldLatency.getValue() + " мс");
        lblJitterValue.setText((int) sldJitter.getValue() + " мс");
        lblBerValue.setText("1e-%.0f".formatted(sldBer.getValue()));
        lblPacketLossValue.setText("%.2f%%".formatted(sldPacketLoss.getValue()));
        lblSnrValue.setText("%.1f дБ".formatted(sldSnr.getValue()));
    }

    // ── Сборка конфига ────────────────────────────────────────────────────

    /**
     * Собирает SimulationConfig из текущего состояния UI.
     * Вызывается только после успешной валидации.
     */
    private SimulationConfig buildConfig() {
        SimulationConfig.SimulationConfigBuilder builder = SimulationConfig.builder()
                .channelParameters(buildChannelParameters())
                .dataType(cmbDataType.getValue())
                .dataSizeBytes(parseDataSize())
                .repetitions(spnRepetitions.getValue())
                .protocol(radioTcp.isSelected()
                        ? TransmissionProtocol.TCP
                        : TransmissionProtocol.UDP);

        // Название симуляции
        String name = txtSimulationName.getText().trim();
        if (!name.isEmpty()) {
            builder.simulationName(name);
        }

        // Файл пользователя
        if (cmbDataType.getValue() == DataType.REAL_FILE && selectedFilePath != null) {
            builder.userFilePath(selectedFilePath);
        }

        // Алгоритмы
        if (chkHuffman.isSelected())   builder.algorithm(CompressionAlgorithm.HUFFMAN);
        if (chkLZ77.isSelected())      builder.algorithm(CompressionAlgorithm.LZ77);
        if (chkDeflate.isSelected())   builder.algorithm(CompressionAlgorithm.DEFLATE);
        if (chkZstandard.isSelected()) builder.algorithm(CompressionAlgorithm.ZSTANDARD);
        if (chkLZ4.isSelected())       builder.algorithm(CompressionAlgorithm.LZ4);
        if (chkSnappy.isSelected())    builder.algorithm(CompressionAlgorithm.SNAPPY);
        if (chkNone.isSelected())      builder.algorithm(CompressionAlgorithm.NONE);

        // Настройки сжатия по умолчанию
        builder.compressionSetting(CompressionAlgorithm.DEFLATE,   CompressionSettings.defaults());
        builder.compressionSetting(CompressionAlgorithm.ZSTANDARD, CompressionSettings.defaults());
        builder.compressionSetting(CompressionAlgorithm.LZ4,       CompressionSettings.defaults());

        return builder.build();
    }

    /**
     * Собирает ChannelParameters из текущего состояния слайдеров.
     */
    private ChannelParameters buildChannelParameters() {
        return ChannelParameters.builder()
                .channelType(cmbChannelType.getValue())
                .bandwidthBps(sliderToBandwidthBps(sldBandwidth.getValue()))
                .latencyMs((long) sldLatency.getValue())
                .jitterMs((long) sldJitter.getValue())
                .bitErrorRate(sliderToBer(sldBer.getValue()))
                .packetLossRate(sldPacketLoss.getValue() / 100.0)
                .mtuBytes(parseMtu())
                .distanceKm(parseDistance())
                .signalToNoiseRatioDb(sldSnr.getValue())
                .build();
    }

    // ── Валидация ─────────────────────────────────────────────────────────

    /**
     * Проверяет корректность всех настроек.
     *
     * @return список ошибок (пустой — если всё корректно)
     */
    private List<String> validate() {
        List<String> errors = new ArrayList<>();

        // Хотя бы один алгоритм
        if (!anyAlgorithmSelected()) {
            errors.add("Выберите хотя бы один алгоритм сжатия");
        }

        // Файл для REAL_FILE
        if (cmbDataType.getValue() == DataType.REAL_FILE) {
            if (selectedFilePath == null) {
                errors.add("Выберите файл для симуляции");
            } else if (!selectedFilePath.toFile().exists()) {
                errors.add("Выбранный файл не существует");
            }
        }

        // Свой размер данных
        if ("Свой размер".equals(cmbDataSize.getValue())) {
            try {
                long size = Long.parseLong(txtCustomSize.getText().trim());
                if (size <= 0) {
                    errors.add("Размер данных должен быть больше нуля");
                } else if (size > 512L * 1024 * 1024) {
                    errors.add("Размер данных не должен превышать 512 МБ");
                }
            } catch (NumberFormatException e) {
                errors.add("Введите корректный размер данных (целое число байт)");
            }
        }

        // MTU
        try {
            int mtu = Integer.parseInt(txtMtu.getText().trim());
            if (mtu < 64 || mtu > 65535) {
                errors.add("MTU должен быть от 64 до 65535 байт");
            }
        } catch (NumberFormatException e) {
            errors.add("Некорректное значение MTU");
        }

        // Расстояние
        try {
            double dist = Double.parseDouble(
                    txtDistance.getText().trim().replace(",", ".")
            );
            if (dist < 0) {
                errors.add("Расстояние не может быть отрицательным");
            }
        } catch (NumberFormatException e) {
            errors.add("Некорректное значение расстояния");
        }

        return errors;
    }

    /**
     * Обновляет доступность кнопки «Запустить» без показа сообщений об ошибке.
     * Вызывается при любом изменении UI.
     */
    private void validateAndRefreshStartButton() {
        boolean canStart = anyAlgorithmSelected()
                && (cmbDataType.getValue() != DataType.REAL_FILE || selectedFilePath != null);

        btnStart.setDisable(!canStart);

        if (!canStart && !anyAlgorithmSelected()) {
            showValidationError("Выберите хотя бы один алгоритм");
        } else {
            lblValidationError.setText("");
            lblValidationError.setVisible(false);
        }
    }

    public void setRunning(boolean running) {
        btnStart.setDisable(running);
        if (!running) {
            // После завершения перепроверяем валидность
            validateAndRefreshStartButton();
        }
    }

    private boolean anyAlgorithmSelected() {
        return chkHuffman.isSelected()   || chkLZ77.isSelected()
                || chkDeflate.isSelected()   || chkZstandard.isSelected()
                || chkLZ4.isSelected()       || chkSnappy.isSelected()
                || chkNone.isSelected();
    }

    private void showValidationError(String message) {
        lblValidationError.setText("⚠  " + message);
        lblValidationError.setVisible(true);
        lblValidationError.setManaged(true);
    }

    // ── Преобразования шкал ───────────────────────────────────────────────

    /**
     * Слайдер [1..100] → пропускная способность (bps).
     * Кусочно-линейная шкала охватывает диапазон 1 Кбит/с — 10 Гбит/с.
     *
     *  [1..25]  →  0 – 10 Мбит/с
     *  [25..50] →  10 – 100 Мбит/с
     *  [50..75] →  100 Мбит/с – 1 Гбит/с
     *  [75..100]→  1 – 10 Гбит/с
     */
    private long sliderToBandwidthBps(double sliderVal) {
        if (sliderVal <= 25)
            return (long) (sliderVal / 25.0 * 10_000_000L);
        if (sliderVal <= 50)
            return (long) ((sliderVal - 25) / 25.0 * 90_000_000L + 10_000_000L);
        if (sliderVal <= 75)
            return (long) ((sliderVal - 50) / 25.0 * 900_000_000L + 100_000_000L);
        return (long) ((sliderVal - 75) / 25.0 * 9_000_000_000L + 1_000_000_000L);
    }

    /**
     * Пропускная способность (bps) → позиция слайдера [1..100].
     */
    private double bandwidthBpsToSlider(long bps) {
        if (bps <= 10_000_000L)
            return bps / 10_000_000.0 * 25;
        if (bps <= 100_000_000L)
            return (bps - 10_000_000.0) / 90_000_000.0 * 25 + 25;
        if (bps <= 1_000_000_000L)
            return (bps - 100_000_000.0) / 900_000_000.0 * 25 + 50;
        return Math.min((bps - 1_000_000_000.0) / 9_000_000_000.0 * 25 + 75, 100);
    }

    /**
     * Слайдер [0..12] → BER = 10^(-sliderVal).
     * 0 → 1e0 (очень плохой канал), 12 → 1e-12 (отличный).
     */
    private double sliderToBer(double sliderVal) {
        if (sliderVal <= 0) return 1.0;
        return Math.pow(10, -sliderVal);
    }

    /**
     * BER → позиция слайдера.
     */
    private double berToSlider(double ber) {
        if (ber <= 0) return sldBer.getMax();
        double val = -Math.log10(ber);
        return clamp(val, sldBer.getMin(), sldBer.getMax());
    }

    // ── Парсеры полей ─────────────────────────────────────────────────────

    private long parseDataSize() {
        return switch (cmbDataSize.getValue()) {
            case "1 КБ"        -> 1024L;
            case "10 КБ"       -> 10L * 1024;
            case "100 КБ"      -> 100L * 1024;
            case "1 МБ"        -> 1024L * 1024;
            case "10 МБ"       -> 10L * 1024 * 1024;
            case "50 МБ"       -> 50L * 1024 * 1024;
            case "Свой размер" -> {
                try {
                    yield Long.parseLong(txtCustomSize.getText().trim());
                } catch (NumberFormatException e) {
                    yield 1024L * 1024; // fallback: 1 МБ
                }
            }
            default -> 1024L * 1024;
        };
    }

    private int parseMtu() {
        try {
            return Integer.parseInt(txtMtu.getText().trim());
        } catch (NumberFormatException e) {
            return 1500; // стандартное значение
        }
    }

    private double parseDistance() {
        try {
            return Double.parseDouble(txtDistance.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // ── Утилиты ───────────────────────────────────────────────────────────

    private String formatBandwidth(long bps) {
        if (bps >= 1_000_000_000L)
            return "%.1f Гбит/с".formatted(bps / 1_000_000_000.0);
        if (bps >= 1_000_000L)
            return "%.0f Мбит/с".formatted(bps / 1_000_000.0);
        return "%.0f Кбит/с".formatted(bps / 1_000.0);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024)          return bytes + " Б";
        if (bytes < 1024 * 1024L)  return "%.1f КБ".formatted(bytes / 1024.0);
        return "%.1f МБ".formatted(bytes / (1024.0 * 1024));
    }

    /** Ограничивает значение диапазоном слайдера. */
    private double clamp(double value, Slider slider) {
        return clamp(value, slider.getMin(), slider.getMax());
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // ── Колбэк ───────────────────────────────────────────────────────────

    /**
     * Функциональный интерфейс для передачи конфига в MainController.
     */
    @FunctionalInterface
    public interface StartSimulationCallback {
        void onStart(SimulationConfig config);
    }
}
