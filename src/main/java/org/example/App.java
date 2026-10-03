package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.client.ApiClient;
import org.example.config.AppContext;
import org.example.config.Database;
import org.example.view.DevicesPane;

public class App extends Application {

    private AppContext context;

    @Override
    public void init() {
        // Corre fuera del hilo de la interfaz: migra la base y arranca la API.
        context = AppContext.start(Database.defaultPath());
    }

    @Override
    public void start(Stage stage) {
        var api = new ApiClient(context.apiBaseUrl());
        stage.setTitle("MBR - Dispositivos médicos");
        stage.setScene(new Scene(new DevicesPane(api), 760, 520));
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