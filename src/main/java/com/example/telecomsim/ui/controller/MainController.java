package com.example.telecomsim.ui.controller;

import com.example.telecomsim.model.metrics.SimulationSession;
import com.example.telecomsim.model.simulation.SimulationConfig;
import com.example.telecomsim.service.TestDataGenerator;
import com.example.telecomsim.service.history.SimulationHistoryService;
import com.example.telecomsim.service.simulation.SimulationService;
import com.example.telecomsim.service.simulation.SimulationServiceImpl;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Главный контроллер приложения.
 *
 * Единственная ответственность — оркестрация вкладок:
 *  — Получает SimulationConfig от SetupController через колбэк
 *  — Передаёт его в RunController
 *  — Получает SimulationSession от RunController
 *  — Раздаёт результат в Results и History
 *
 * НЕ содержит логики настройки симуляции — она полностью
 * инкапсулирована в SimulationSetupController.
 */
public class MainController implements Initializable {

    // ── FXML: Вкладки ────────────────────────────────────────────────────

    @FXML private TabPane tabPaneMain;
    @FXML private Tab     tabSetup;
    @FXML private Tab     tabRun;
    @FXML private Tab     tabResults;
    @FXML private Tab     tabHistory;

    // ── FXML: Статусная строка ────────────────────────────────────────────

    @FXML private Label labelAppStatus;
    @FXML private Label labelVersion;

    // ── FXML: Вложенные контроллеры ───────────────────────────────────────
    //
    // JavaFX внедряет контроллеры вложенных FXML автоматически,
    // если имя поля = fx:id тега <fx:include> + суффикс "Controller".
    //
    // Пример: <fx:include fx:id="setupTab" .../> → поле setupTabController
    //
    // ВАЖНО: fx:id в MainView.fxml должны точно совпадать с именами ниже
    // (без суффикса "Controller").

    @FXML private SimulationSetupController setupTabController;
    @FXML private SimulationRunController   runTabController;
    @FXML private ResultsController         resultsTabController;
    @FXML private HistoryController         historyTabController;

    // ── Сервисы ───────────────────────────────────────────────────────────

    private SimulationService        simulationService;
    private SimulationHistoryService historyService;

    // ── Инициализация ─────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initServices();
        injectDependencies();
        configureTabSwitching();

        labelVersion.setText("v1.0");
        setStatus("Готов к работе");
    }

    // ─────────────────────────────────────────────────────────────────────

    private void initServices() {
        simulationService = new SimulationServiceImpl(new TestDataGenerator());
        historyService    = new SimulationHistoryService();
    }

    /**
     * Передаём зависимости во вложенные контроллеры и подписываемся
     * на их колбэки. Все контроллеры к этому моменту уже созданы
     * FXMLLoader-ом и прошли свой initialize().
     */
    private void injectDependencies() {

        // ── Setup: передаём колбэк запуска ──
        if (setupTabController != null) {
            setupTabController.setStartCallback(this::handleStartRequested);
        }

        // ── Run: передаём сервис и колбэк завершения ──
        if (runTabController != null) {
            runTabController.setSimulationService(simulationService);
            runTabController.setSessionCompletedCallback(this::handleSessionCompleted);
        }

        // ── History: передаём сервис истории и колбэки ──
        if (historyTabController != null) {
            historyTabController.setHistoryService(historyService);
            historyTabController.setRerunCallback(this::handleRerun);
            historyTabController.setViewDetailsCallback(this::handleViewDetails);
        }
    }

    private void configureTabSwitching() {
        // Run и Results недоступны до первого запуска симуляции
        tabRun.setDisable(true);
        tabResults.setDisable(true);
    }

    // ── Колбэки от вложенных контроллеров ────────────────────────────────

    /**
     * SetupController собрал и провалидировал конфиг — запускаем симуляцию.
     */
    private void handleStartRequested(SimulationConfig config) {
        tabRun.setDisable(false);
        tabPaneMain.getSelectionModel().select(tabRun);
        setStatus("Симуляция выполняется: " + config.getSimulationName());

        if (runTabController != null) {
            runTabController.startSimulation(config);
        }
    }

    /**
     * RunController завершил сессию — раздаём результаты.
     */
    private void handleSessionCompleted(SimulationSession session) {
        tabResults.setDisable(false);
        tabPaneMain.getSelectionModel().select(tabResults);

        if (resultsTabController != null) {
            resultsTabController.displaySession(session);
        }
        if (historyTabController != null) {
            historyTabController.addSession(session);
        }

        // Разблокируем возможность нового запуска
        if (setupTabController != null) {
            setupTabController.setRunning(false);
        }

        setStatus("Завершено: " + session.getConfig().getSimulationName());
    }

    /**
     * HistoryController запросил повтор симуляции с сохранёнными настройками.
     */
    private void handleRerun(SimulationConfig config) {
        if (setupTabController != null) {
            setupTabController.applyConfig(config);
        }
        tabPaneMain.getSelectionModel().select(tabSetup);
        setStatus("Настройки загружены из истории. Нажмите «Запустить».");
    }

    /**
     * HistoryController запросил просмотр деталей сессии.
     */
    private void handleViewDetails(SimulationSession session) {
        if (resultsTabController != null) {
            resultsTabController.displaySession(session);
            tabResults.setDisable(false);
            tabPaneMain.getSelectionModel().select(tabResults);
        }
    }

    // ── FXML-обработчики меню ─────────────────────────────────────────────

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("О программе");
        alert.setHeaderText("Инструмент анализа алгоритмов сжатия");
        alert.setContentText("""
                Версия: 1.0

                Приложение для сравнения и анализа алгоритмов
                сжатия данных в телекоммуникационных системах.

                Алгоритмы: Huffman, LZ77, DEFLATE, Zstandard, LZ4, Snappy

                Разработано в рамках ВКР.
                """);
        alert.showAndWait();
    }

    @FXML
    private void handleExit() {
        if (simulationService != null && simulationService.isRunning()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Выход");
            confirm.setHeaderText("Симуляция ещё выполняется");
            confirm.setContentText("Прервать симуляцию и выйти из приложения?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    simulationService.cancelSimulation();
                    Platform.exit();
                }
            });
        } else {
            Platform.exit();
        }
    }

    // ── Утилиты ───────────────────────────────────────────────────────────

    private void setStatus(String message) {
        if (labelAppStatus != null) {
            labelAppStatus.setText(message);
        }
    }
}