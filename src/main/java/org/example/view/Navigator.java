package org.example.view;

import javafx.scene.layout.BorderPane;
import org.example.api.DeviceDto;
import org.example.client.ApiClient;

/** Raíz de la aplicación: decide qué pantalla se muestra. */
public class Navigator extends BorderPane {

    private final ApiClient api;

    public Navigator(ApiClient api) {
        this.api = api;
        showHome();
    }

    public void showHome() {
        setCenter(new HomeView(api, this));
    }

    public void showCreateDevice() {
        setCenter(new DeviceFormView(api, this, null));
    }

    public void showEditDevice(DeviceDto device) {
        setCenter(new DeviceFormView(api, this, device));
    }

    public void showWorkspace(DeviceDto device) {
        setCenter(new WorkspaceView(api, this, device));
    }
}
