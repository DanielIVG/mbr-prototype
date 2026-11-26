package org.example;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.client.ApiClient;
import org.example.config.AppContext;
import org.example.config.Database;
import org.example.view.Navigator;

import java.util.Objects;

public class App extends Application {

    private AppContext context;

    @Override
    public void init() {
        // Corre fuera del hilo de la interfaz: migra la base y arranca la API.
        context = AppContext.start(Database.defaultPath());
    }

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        var api = new ApiClient(context.apiBaseUrl());
        var scene = new Scene(new Navigator(api), 1280, 800);
        scene.getStylesheets().add(
                Objects.requireNonNull(App.class.getResource("/css/mbr.css")).toExternalForm());
        stage.setTitle("MBR — Gestión de riesgos de dispositivos médicos");
        stage.setMinWidth(1000);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
