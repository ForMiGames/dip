package com.example.telecomsim.ui.chart;

import com.example.telecomsim.model.compression.CompressionAlgorithm;
import com.example.telecomsim.model.metrics.SimulationResult;
import javafx.scene.layout.Pane;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

/**
 * Кастомный компонент — Radar Chart (паутинная диаграмма).
 *
 * Отображает многокритериальное сравнение алгоритмов сжатия по осям:
 *  — Степень сжатия
 *  — Скорость сжатия
 *  — Скорость распаковки
 *  — Эффективная скорость передачи
 *  — Процент доставки пакетов
 *  — Интегральная оценка
 *
 * Каждый алгоритм отображается своим цветом.
 * Значения нормализуются к диапазону [0..1] относительно максимума по выборке.
 */
public class RadarChartView  extends Pane {

    // ── Оси радара ────────────────────────────────────────────────────────

    private static final String[] AXIS_LABELS = {
            "Степень\nсжатия",
            "Скорость\nсжатия",
            "Скорость\nраспаковки",
            "Скорость\nпередачи",
            "Доставка\nпакетов",
            "Интегральная\nоценка"
    };

    private static final int AXIS_COUNT = AXIS_LABELS.length;

    // ── Цвета алгоритмов ──────────────────────────────────────────────────

    private static final Color[] ALGORITHM_COLORS = {
            Color.web("#3498db"),   // Синий      — Huffman
            Color.web("#e74c3c"),   // Красный    — LZ77
            Color.web("#2ecc71"),   // Зелёный    — DEFLATE
            Color.web("#f39c12"),   // Оранжевый  — Zstandard
            Color.web("#9b59b6"),   // Фиолетовый — LZ4
            Color.web("#1abc9c"),   // Бирюзовый  — Snappy
            Color.web("#34495e"),   // Серый      — None
    };

    // ── Геометрия ─────────────────────────────────────────────────────────

    private static final int GRID_LEVELS    = 5;   // Количество концентрических сеток
    private static final double LABEL_OFFSET = 28;  // Отступ подписей осей от края
    private static final double MARGIN       = 70;  // Отступ от края Pane до сетки

    // ── Данные ────────────────────────────────────────────────────────────

    private final List<SimulationResult> results = new ArrayList<>();
    private Canvas canvas;

    // ── Конструктор ───────────────────────────────────────────────────────

    public RadarChartView() {
        canvas = new Canvas();
        getChildren().add(canvas);

        // Перерисовываем при изменении размеров Pane
        widthProperty().addListener((obs, o, n)  -> redraw());
        heightProperty().addListener((obs, o, n) -> redraw());
    }

    // ── Публичный API ─────────────────────────────────────────────────────

    /**
     * Устанавливает данные для отображения и перерисовывает диаграмму.
     */
    public void setResults(List<SimulationResult> results) {
        this.results.clear();
        this.results.addAll(results);
        redraw();
    }

    /** Очищает диаграмму. */
    public void clear() {
        results.clear();
        redraw();
    }

    // ── Отрисовка ─────────────────────────────────────────────────────────

    private void redraw() {
        double width  = getWidth();
        double height = getHeight();

        if (width <= 0 || height <= 0) return;

        canvas.setWidth(width);
        canvas.setHeight(height);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        double cx      = width  / 2.0; // Центр X
        double cy      = height / 2.0; // Центр Y
        double radius  = Math.min(cx, cy) - MARGIN;

        if (radius <= 0) return;

        drawBackground(gc, cx, cy, radius);
        drawGrid(gc, cx, cy, radius);
        drawAxes(gc, cx, cy, radius);
        drawAxisLabels(gc, cx, cy, radius);

        if (!results.isEmpty()) {
            double[][] normalized = normalizeValues();
            drawDataPolygons(gc, cx, cy, radius, normalized);
            drawDataPoints(gc, cx, cy, radius, normalized);
        } else {
            drawEmptyMessage(gc, cx, cy);
        }
    }

    /**
     * Рисует светло-серый фон диаграммы.
     */
    private void drawBackground(GraphicsContext gc, double cx, double cy, double radius) {
        gc.setFill(Color.web("#fafafa"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * Рисует концентрические сетки (web-линии уровней).
     */
    private void drawGrid(GraphicsContext gc, double cx, double cy, double radius) {
        gc.setStroke(Color.web("#dfe6e9"));
        gc.setLineWidth(1.0);

        for (int level = 1; level <= GRID_LEVELS; level++) {
            double r = radius * level / GRID_LEVELS;
            double[] xs = new double[AXIS_COUNT];
            double[] ys = new double[AXIS_COUNT];

            for (int i = 0; i < AXIS_COUNT; i++) {
                double angle = axisAngle(i);
                xs[i] = cx + r * Math.cos(angle);
                ys[i] = cy + r * Math.sin(angle);
            }

            gc.strokePolygon(xs, ys, AXIS_COUNT);

            // Подпись уровня (20%, 40%, ..., 100%)
            int percent = level * 100 / GRID_LEVELS;
            gc.setFill(Color.web("#b2bec3"));
            gc.setFont(Font.font("System", 9));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(percent + "%", cx + 4, cy - r + 10);
        }
    }

    /**
     * Рисует оси от центра до вершин.
     */
    private void drawAxes(GraphicsContext gc, double cx, double cy, double radius) {
        gc.setStroke(Color.web("#b2bec3"));
        gc.setLineWidth(1.5);

        for (int i = 0; i < AXIS_COUNT; i++) {
            double angle = axisAngle(i);
            double x = cx + radius * Math.cos(angle);
            double y = cy + radius * Math.sin(angle);
            gc.strokeLine(cx, cy, x, y);
        }
    }

    /**
     * Рисует подписи осей.
     */
    private void drawAxisLabels(GraphicsContext gc, double cx, double cy, double radius) {
        gc.setFont(Font.font("System", FontWeight.BOLD, 11));
        gc.setFill(Color.web("#2d3436"));
        gc.setTextAlign(TextAlignment.CENTER);

        for (int i = 0; i < AXIS_COUNT; i++) {
            double angle = axisAngle(i);
            double r     = radius + LABEL_OFFSET;
            double x     = cx + r * Math.cos(angle);
            double y     = cy + r * Math.sin(angle);

            // Многострочная подпись через явный перенос
            String[] lines = AXIS_LABELS[i].split("\n");
            double lineHeight = 13;
            double totalHeight = lines.length * lineHeight;
            double startY = y - totalHeight / 2.0 + lineHeight / 2.0;

            for (int l = 0; l < lines.length; l++) {
                gc.fillText(lines[l], x, startY + l * lineHeight);
            }
        }
    }

    /**
     * Рисует закрашенные многоугольники данных для каждого алгоритма.
     */
    private void drawDataPolygons(GraphicsContext gc,
                                  double cx, double cy, double radius,
                                  double[][] normalized) {
        for (int r = 0; r < results.size(); r++) {
            Color color = getAlgorithmColor(r);

            double[] xs = new double[AXIS_COUNT];
            double[] ys = new double[AXIS_COUNT];

            for (int i = 0; i < AXIS_COUNT; i++) {
                double angle = axisAngle(i);
                double value = normalized[r][i];
                xs[i] = cx + radius * value * Math.cos(angle);
                ys[i] = cy + radius * value * Math.sin(angle);
            }

            // Заливка полупрозрачным цветом
            gc.setFill(Color.color(
                    color.getRed(), color.getGreen(), color.getBlue(), 0.15
            ));
            gc.fillPolygon(xs, ys, AXIS_COUNT);

            // Обводка
            gc.setStroke(color);
            gc.setLineWidth(2.0);
            gc.strokePolygon(xs, ys, AXIS_COUNT);
        }
    }

    /**
     * Рисует точки данных на осях.
     */
    private void drawDataPoints(GraphicsContext gc,
                                double cx, double cy, double radius,
                                double[][] normalized) {
        for (int r = 0; r < results.size(); r++) {
            Color color = getAlgorithmColor(r);

            for (int i = 0; i < AXIS_COUNT; i++) {
                double angle = axisAngle(i);
                double value = normalized[r][i];
                double x = cx + radius * value * Math.cos(angle);
                double y = cy + radius * value * Math.sin(angle);

                gc.setFill(Color.WHITE);
                gc.fillOval(x - 5, y - 5, 10, 10);
                gc.setStroke(color);
                gc.setLineWidth(2.0);
                gc.strokeOval(x - 5, y - 5, 10, 10);
            }
        }
    }

    private void drawEmptyMessage(GraphicsContext gc, double cx, double cy) {
        gc.setFill(Color.web("#b2bec3"));
        gc.setFont(Font.font("System", 14));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Нет данных для отображения\nЗапустите симуляцию", cx, cy);
    }

    // ── Нормализация значений ─────────────────────────────────────────────

    /**
     * Нормализует все значения к диапазону [0..1] относительно максимума.
     * Возвращает двумерный массив [алгоритм][ось].
     */
    private double[][] normalizeValues() {
        int n = results.size();
        double[][] raw = extractRawValues();

        // Находим максимум по каждой оси
        double[] maxValues = new double[AXIS_COUNT];
        for (int axis = 0; axis < AXIS_COUNT; axis++) {
            for (int r = 0; r < n; r++) {
                maxValues[axis] = Math.max(maxValues[axis], raw[r][axis]);
            }
        }

        // Нормализуем
        double[][] normalized = new double[n][AXIS_COUNT];
        for (int r = 0; r < n; r++) {
            for (int axis = 0; axis < AXIS_COUNT; axis++) {
                if (maxValues[axis] > 0) {
                    normalized[r][axis] = raw[r][axis] / maxValues[axis];
                } else {
                    normalized[r][axis] = 0.0;
                }
            }
        }

        return normalized;
    }

    /**
     * Извлекает сырые значения по осям для каждого алгоритма.
     * Порядок осей соответствует AXIS_LABELS.
     */
    private double[][] extractRawValues() {
        int n = results.size();
        double[][] raw = new double[n][AXIS_COUNT];

        for (int r = 0; r < n; r++) {
            SimulationResult result = results.get(r);
            raw[r][0] = result.getCompressionRatio();                          // Степень сжатия
            raw[r][1] = result.getCompressionSpeedMbps();                      // Скорость сжатия
            raw[r][2] = result.getDecompressionSpeedMbps();                    // Скорость распаковки
            raw[r][3] = result.getEffectiveTransferRateMbps();                 // Скорость передачи
            raw[r][4] = result.getDeliveryRatePercent();                       // Доставка пакетов
            raw[r][5] = result.getOverallEfficiencyScore();                    // Интегральная оценка
        }

        return raw;
    }

    // ── Утилиты ───────────────────────────────────────────────────────────

    /**
     * Угол оси i (в радианах). Начинаем сверху (−π/2), идём по часовой стрелке.
     */
    private double axisAngle(int axisIndex) {
        return -Math.PI / 2.0 + (2.0 * Math.PI * axisIndex / AXIS_COUNT);
    }

    private Color getAlgorithmColor(int index) {
        return ALGORITHM_COLORS[index % ALGORITHM_COLORS.length];
    }

    /**
     * Возвращает цвет алгоритма по его enum-значению.
     * Используется в легенде ResultsController.
     */
    public static Color getColorForAlgorithm(CompressionAlgorithm algorithm) {
        CompressionAlgorithm[] values = CompressionAlgorithm.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i] == algorithm) {
                return ALGORITHM_COLORS[i % ALGORITHM_COLORS.length];
            }
        }
        return Color.GRAY;
    }
}
