package com.example.telecomsim.model.simulation;



public enum DataType {
    TEXT_UTF8("Текст UTF-8"),
    TEXT_ASCII("Текст ASCII"),
    BINARY_RANDOM("Случайные бинарные данные"),
    BINARY_STRUCTURED("Структурированные данные"),
    REAL_FILE("Файл пользователя");

    private final String displayName;

    DataType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
