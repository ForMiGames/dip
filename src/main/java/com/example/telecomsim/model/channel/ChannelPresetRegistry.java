package com.example.telecomsim.model.channel;


import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Реестр предустановленных профилей каналов связи.
 *
 * Содержит типичные конфигурации каналов для быстрого выбора в UI.
 * Пресеты сгруппированы по технологии и сценарию использования.
 *
 * Использование:
 * <pre>{@code
 * List<ChannelPreset> allPresets = ChannelPresetRegistry.getAllPresets();
 * ChannelPreset wifi = ChannelPresetRegistry.getPreset("wifi_office");
 * }</pre>
 */
public final class ChannelPresetRegistry {
    // Хранилище: ключ → пресет (LinkedHashMap сохраняет порядок вставки)
    private static final Map<String, ChannelPreset> PRESETS = new LinkedHashMap<>();

    static {
        registerAll();
    }

    private ChannelPresetRegistry() {}

    // ── Публичный API ─────────────────────────────────────────────────────

    /**
     * Возвращает все встроенные пресеты в порядке регистрации.
     */
    public static List<ChannelPreset> getAllPresets() {
        return Collections.unmodifiableList(new ArrayList<>(PRESETS.values()));
    }

    /**
     * Возвращает пресет по ключу.
     *
     * @throws IllegalArgumentException если пресет с таким ключом не найден
     */
    public static ChannelPreset getPreset(String key) {
        ChannelPreset preset = PRESETS.get(key);
        if (preset == null) {
            throw new IllegalArgumentException("Пресет не найден: " + key);
        }
        return preset;
    }

    // ── Регистрация всех пресетов ─────────────────────────────────────────

    private static void registerAll() {

        // ════════════════════════════════════════════════════════════════
        //  ОПТОВОЛОКНО
        // ════════════════════════════════════════════════════════════════

        register("fiber_datacenter", ChannelPreset.builder()
                .name("Оптоволокно — ЦОД (дата-центр)")
                .description("""
                Соединение между серверами внутри дата-центра.
                Минимальные потери, сверхнизкая задержка, высочайшая пропускная способность.
                Типичный сценарий: репликация БД, backup, межсерверный обмен.
                """)
                .icon("🔵")
                .parameters(ChannelParameters.builder()
                        .channelType(ChannelType.FIBER_OPTIC)
                        .bandwidthBps(10_000_000_000L)   // 10 Гбит/с
                        .latencyMs(1)
                        .jitterMs(1)
                        .bitErrorRate(1e-12)
                        .packetLossRate(0.0001)
                        .mtuBytes(9000)                  // Jumbo frames
                        .distanceKm(0.5)
                        .signalToNoiseRatioDb(40.0)
                        .build())
                .build());

        register("fiber_city", ChannelPreset.builder()
                .name("Оптоволокно — городская сеть")
                .description("""
                Магистральный канал между узлами городской сети.
                Высокая скорость, низкие задержки, стабильный канал.
                Типичный сценарий: соединение офисов, ISP backbone.
                """)
                .icon("🔵")
                .parameters(ChannelParameters.builder()
                        .channelType(ChannelType.FIBER_OPTIC)
                        .bandwidthBps(1_000_000_000L)    // 1 Гбит/с
                        .latencyMs(5)
                        .jitterMs(2)
                        .bitErrorRate(1e-11)
                        .packetLossRate(0.0001)
                        .mtuBytes(1500)
                        .distanceKm(15.0)
                        .signalToNoiseRatioDb(38.0)
                        .build())
                .build());

        register("fiber_intercity", ChannelPreset.builder()
                .name("Оптоволокно — межгородской канал")
                .description("""
                Дальняя магистраль между городами.
                Заметная задержка из-за расстояния, но высокая пропускная способность.
                Типичный сценарий: WAN-соединение, CDN-репликация.
                """)
                .icon("🔵")
                .parameters(ChannelParameters.builder()
                        .channelType(ChannelType.FIBER_OPTIC)
                        .bandwidthBps(10_000_000_000L)   // 10 Гбит/с
                        .latencyMs(10)
                        .jitterMs(3)
                        .bitErrorRate(1e-10)
                        .packetLossRate(0.0002)
                        .mtuBytes(1500)
                        .distanceKm(500.0)
                        .signalToNoiseRatioDb(35.0)
                        .build())
                .build());

        // ════════════════════════════════════════════════════════════════
        //  ETHERNET
        // ════════════════════════════════════════════════════════════════

        register("ethernet_gigabit", ChannelPreset.builder()
                .name("Ethernet — гигабитная ЛВС")
                .description("""
                Типичная гигабитная локальная сеть офиса или предприятия.
                Стабильный канал, минимальные потери при хорошей инфраструктуре.
                Типичный сценарий: корпоративная сеть, NAS, VoIP.
                """)
                .icon("🟢")
                .parameters(ChannelParameters.builder()
                        .channelType(ChannelType.ETHERNET)
                        .bandwidthBps(1_000_000_000L)    // 1 Гбит/с
                        .latencyMs(1)
                        .jitterMs(1)
                        .bitErrorRate(1e-8)
                        .packetLossRate(0.0001)
                        .mtuBytes(1500)
                        .distanceKm(0.1)
                        .signalToNoiseRatioDb(30.0)
                        .build())
                .build());

        register("ethernet_fast", ChannelPreset.builder()
                .name("Ethernet — Fast Ethernet (100 Мбит/с)")
                .description("""
                Устаревший, но широко распространённый стандарт Fast Ethernet.
                Встречается в старых офисных сетях и промышленных системах.
                """)
                .icon("🟢")
                .parameters(ChannelParameters.builder()
                        .channelType(ChannelType.ETHERNET)
                        .bandwidthBps(100_000_000L)      // 100 Мбит/с
                        .latencyMs(2)
                        .jitterMs(2)
                        .bitErrorRate(1e-7)
                        .packetLossRate(0.001)
                        .mtuBytes(1500)
                        .distanceKm(0.1)
                        .signalToNoiseRatioDb(28.0)
                        .build())
                .build());

        // ════════════════════════════════════════════════════════════════
        //  WI-FI
        // ════════════════════════════════════════════════════════════════

        register("wifi_office", ChannelPreset.builder()
                .name("Wi-Fi — офис (802.11ac, хороший сигнал)")
                .description("""
                Wi-Fi соединение в офисе с качественным оборудованием и хорошим сигналом.
                Умеренный джиттер, редкие потери пакетов.
                Типичный сценарий: корпоративный Wi-Fi, рабочее место.
                """)
                .icon("📶")
                .parameters(ChannelParameters.builder()
                        .channelType(ChannelType.WIFI)
                        .bandwidthBps(300_000_000L)      // 300 Мбит/с
                        .latencyMs(15)
                        .jitterMs(8)
                        .bitErrorRate(1e-5)
                        .packetLossRate(0.005)
                        .mtuBytes(1500)
                        .distanceKm(0.05)
                        .signalToNoiseRatioDb(25.0)
                        .build())
                .build());

        register("wifi_home", ChannelPreset.builder()
                .name("Wi-Fi — домашняя сеть (смешанный трафик)")
                .description("""
                Типичная домашняя Wi-Fi сеть с несколькими устройствами.
                Более высокий джиттер из-за интерференции и конкуренции за канал.
                Типичный сценарий: стриминг, загрузки, игры.
                """)
                .icon("📶")
                .parameters(ChannelParameters.builder()
                        .channelType(ChannelType.WIFI)
                        .bandwidthBps(100_000_000L)      // 100 Мбит/с
                        .latencyMs(20)
                        .jitterMs(15)
                        .bitErrorRate(1e-5)
                        .packetLossRate(0.01)
                        .mtuBytes(1500)
                        .distanceKm(0.02)
                        .signalToNoiseRatioDb(20.0)
                        .build())
                .build());

        register("wifi_weak", ChannelPreset.builder()
                .name("Wi-Fi — слабый сигнал (помехи, стены)")
                .description("""
                Wi-Fi с ослабленным сигналом: толстые стены, большое расстояние, помехи.
                Высокий уровень ошибок, заметные потери пакетов.
                Демонстрирует деградацию качества беспроводного канала.
                """)
                .icon("📶")
                .parameters(ChannelParameters.builder()
                        .channelType(ChannelType.WIFI)
                        .bandwidthBps(24_000_000L)       // 24 Мбит/с
                        .latencyMs(40)
                        .jitterMs(25)
                        .bitErrorRate(1e-4)
                        .packetLossRate(0.05)
                        .mtuBytes(1500)
                        .distanceKm(0.08)
                        .signalToNoiseRatioDb(10.0)
                        .build())
                .build());

        // ════════════════════════════════════════════════════════════════
        //  4G / LTE
        // ════════════════════════════════════════════════════════════════

        register("lte_urban", ChannelPreset.builder()
                .name("4G/LTE — город (хорошее покрытие)")
                .description("""
                4G LTE соединение в городе с хорошим покрытием оператора.
                Заметная задержка (по сравнению с проводным каналом), но высокая скорость.
                Типичный сценарий: мобильный интернет, IoT-устройства.
                """)
                .icon("📱")
                .parameters(ChannelParameters.builder()
                        .channelType(ChannelType.LTE_4G)
                        .bandwidthBps(50_000_000L)       // 50 Мбит/с
                        .latencyMs(30)
                        .jitterMs(15)
                        .bitErrorRate(1e-6)
                        .packetLossRate(0.005)
                        .mtuBytes(1500)
                        .distanceKm(1.0)
                        .signalToNoiseRatioDb(20.0)
                        .build())
                .build());

        register("lte_suburban", ChannelPreset.builder()
                .name("4G/LTE — пригород (среднее покрытие)")
                .description("""
                4G LTE в пригородной зоне: покрытие есть, но не идеальное.
                Скорость ниже, задержки выше, возможны кратковременные потери.
                """)
                .icon("📱")
                .parameters(ChannelParameters.builder()
                        .channelType(ChannelType.LTE_4G)
                        .bandwidthBps(20_000_000L)       // 20 Мбит/с
                        .latencyMs(50)
                        .jitterMs(25)
                        .bitErrorRate(1e-5)
                        .packetLossRate(0.02)
                        .mtuBytes(1500)
                        .distanceKm(3.0)
                        .signalToNoiseRatioDb(15.0)
                        .build())
                .build());

        // ════════════════════════════════════════════════════════════════
        //  5G
        // ════════════════════════════════════════════════════════════════

        register("5g_urban", ChannelPreset.builder()
                .name("5G — город (eMBB, высокая скорость)")
                .description("""
                5G в режиме eMBB (Enhanced Mobile Broadband) в городской зоне.
                Сверхвысокая скорость, минимальная задержка.
                Типичный сценарий: AR/VR, 8K-стриминг, высоконагруженные IoT.
                """)
                .icon("🚀")
                .parameters(ChannelParameters.builder()
                        .channelType(ChannelType.MOBILE_5G)
                        .bandwidthBps(1_000_000_000L)    // 1 Гбит/с
                        .latencyMs(5)
                        .jitterMs(3)
                        .bitErrorRate(1e-7)
                        .packetLossRate(0.001)
                        .mtuBytes(1500)
                        .distanceKm(0.3)
                        .signalToNoiseRatioDb(30.0)
                        .build())
                .build());

        // ════════════════════════════════════════════════════════════════
        //  СПУТНИКОВЫЙ КАНАЛ
        // ════════════════════════════════════════════════════════════════

        register("satellite_geo", ChannelPreset.builder()
                .name("Спутник — геостационарный (GEO)")
                .description("""
                Геостационарный спутниковый канал (высота ~36 000 км).
                Огромная задержка из-за расстояния — критична для интерактивных приложений.
                Подходит для широковещательных задач: телевидение, интернет в отдалённых районах.
                Типичный сценарий: VSAT, спутниковый интернет (HughesNet, Viasat).
                """)
                .icon("🛰️")
                .parameters(ChannelParameters.builder()
                        .channelType(ChannelType.SATELLITE)
                        .bandwidthBps(20_000_000L)       // 20 Мбит/с
                        .latencyMs(20)
                        .jitterMs(10)
                        .bitErrorRate(1e-7)
                        .packetLossRate(0.005)
                        .mtuBytes(1500)
                        .distanceKm(36_000.0)            // ~36 000 км до GEO
                        .signalToNoiseRatioDb(18.0)
                        .build())
                .build());

        register("satellite_leo", ChannelPreset.builder()
                .name("Спутник — низкая орбита (LEO, Starlink)")
                .description("""
                Низкоорбитальный спутниковый интернет (высота ~550 км).
                Задержка значительно ниже GEO благодаря близкой орбите.
                Типичный сценарий: Starlink, OneWeb — широкополосный доступ из любой точки мира.
                """)
                .icon("🛰️")
                .parameters(ChannelParameters.builder()
                        .channelType(ChannelType.SATELLITE)
                        .bandwidthBps(200_000_000L)      // 200 Мбит/с
                        .latencyMs(10)
                        .jitterMs(8)
                        .bitErrorRate(1e-7)
                        .packetLossRate(0.003)
                        .mtuBytes(1500)
                        .distanceKm(550.0)
                        .signalToNoiseRatioDb(22.0)
                        .build())
                .build());

        // ════════════════════════════════════════════════════════════════
        //  DSL
        // ════════════════════════════════════════════════════════════════

        register("dsl_adsl", ChannelPreset.builder()
                .name("DSL — ADSL (асимметричный, 8 Мбит/с)")
                .description("""
                Устаревший, но широко используемый ADSL через телефонную линию.
                Асимметричный: скорость загрузки выше, чем отдачи.
                Типичный сценарий: домашний интернет в районах без оптики.
                """)
                .icon("☎️")
                .parameters(ChannelParameters.builder()
                        .channelType(ChannelType.DSL)
                        .bandwidthBps(8_000_000L)        // 8 Мбит/с
                        .latencyMs(40)
                        .jitterMs(15)
                        .bitErrorRate(1e-6)
                        .packetLossRate(0.005)
                        .mtuBytes(1492)                  // PPPoE overhead
                        .distanceKm(3.0)
                        .signalToNoiseRatioDb(15.0)
                        .build())
                .build());

        register("dsl_vdsl", ChannelPreset.builder()
                .name("DSL — VDSL2 (50 Мбит/с)")
                .description("""
                Современный VDSL2 по телефонной паре (короткая линия от узла).
                Значительно быстрее ADSL, подходит для большинства бытовых задач.
                Типичный сценарий: городской DSL с оптикой до уличного шкафа (FTTC).
                """)
                .icon("☎️")
                .parameters(ChannelParameters.builder()
                        .channelType(ChannelType.DSL)
                        .bandwidthBps(50_000_000L)       // 50 Мбит/с
                        .latencyMs(25)
                        .jitterMs(10)
                        .bitErrorRate(1e-7)
                        .packetLossRate(0.002)
                        .mtuBytes(1492)
                        .distanceKm(0.5)
                        .signalToNoiseRatioDb(22.0)
                        .build())
                .build());

        // ════════════════════════════════════════════════════════════════
        //  СПЕЦИАЛЬНЫЕ СЦЕНАРИИ
        // ════════════════════════════════════════════════════════════════

        register("ideal", ChannelPreset.builder()
                .name("⚡ Идеальный канал (без ошибок)")
                .description("""
                Теоретический идеальный канал: нет ошибок, нет потерь, минимальная задержка.
                Используется как базовая точка отсчёта для оценки алгоритмов сжатия
                в изоляции от влияния канала.
                """)
                .icon("⚡")
                .parameters(ChannelParameters.builder()
                        .channelType(ChannelType.ETHERNET)
                        .bandwidthBps(10_000_000_000L)   // 10 Гбит/с
                        .latencyMs(0)
                        .jitterMs(0)
                        .bitErrorRate(0.0)
                        .packetLossRate(0.0)
                        .mtuBytes(9000)
                        .distanceKm(0.0)
                        .signalToNoiseRatioDb(60.0)
                        .build())
                .build());

        register("degraded", ChannelPreset.builder()
                .name("💀 Деградированный канал (высокие потери)")
                .description("""
                Канал с высоким уровнем ошибок и потерь.
                Демонстрирует поведение алгоритмов в экстремальных условиях:
                аварийные ситуации, сильные помехи, перегруженный канал.
                """)
                .icon("💀")
                .parameters(ChannelParameters.builder()
                        .channelType(ChannelType.WIFI)
                        .bandwidthBps(10_000_000L)       // 10 Мбит/с
                        .latencyMs(100)
                        .jitterMs(50)
                        .bitErrorRate(1e-3)
                        .packetLossRate(0.1)
                        .mtuBytes(1500)
                        .distanceKm(0.2)
                        .signalToNoiseRatioDb(5.0)
                        .build())
                .build());
    }

    private static void register(String key, ChannelPreset preset) {
        PRESETS.put(key, preset);
    }
}
