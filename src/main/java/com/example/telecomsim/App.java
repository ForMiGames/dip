package com.example.telecomsim;

import javafx.application.Application;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Точка входа JavaFX-приложения.
 */
public class App  extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                App.class.getResource("fxml/MainView.fxml")
        );

        Scene scene = new Scene(loader.load());

        stage.setTitle("Анализ алгоритмов сжатия и передачи данных");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setWidth(1400);
        stage.setHeight(930);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
