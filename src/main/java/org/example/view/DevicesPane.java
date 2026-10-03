package org.example.view;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.api.DeviceDto;
import org.example.client.ApiClient;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DevicesPane extends BorderPane {

    private final ApiClient api;
    private final TableView<DeviceDto> table = new TableView<>();
    private final TextField nameField = new TextField();
    private final TextField useField = new TextField();
    private final TextField misuseField = new TextField();
    private final Button createButton = new Button("Crear");
    private final Button deactivateButton = new Button("Desactivar seleccionado");
    private final Label status = new Label();

    public DevicesPane(ApiClient api) {
        this.api = api;
        buildTable();
        setCenter(table);
        setBottom(buildForm());
        refresh();
    }

    private void buildTable() {
        var name = new TableColumn<DeviceDto, String>("Nombre");
        name.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().name()));
        name.setPrefWidth(200);

        var use = new TableColumn<DeviceDto, String>("Uso previsto");
        use.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().intendedUse()));
        use.setPrefWidth(260);

        var misuse = new TableColumn<DeviceDto, String>("Mal uso previsible");
        misuse.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                Objects.toString(c.getValue().reasonablyForeseeableMisuse(), "")));
        misuse.setPrefWidth(240);

        table.getColumns().add(name);
        table.getColumns().add(use);
        table.getColumns().add(misuse);
    }

    private VBox buildForm() {
        nameField.setPromptText("Nombre del dispositivo");
        useField.setPromptText("Uso previsto");
        misuseField.setPromptText("Mal uso razonablemente previsible (opcional)");

        createButton.setOnAction(e -> create());
        deactivateButton.setOnAction(e -> deactivate());
        deactivateButton.disableProperty()
                .bind(table.getSelectionModel().selectedItemProperty().isNull());

        var box = new VBox(8, nameField, useField, misuseField,
                new HBox(8, createButton, deactivateButton), status);
        box.setPadding(new Insets(12));
        return box;
    }

    private void refresh() {
        runAsync(api::listDevices, list -> table.getItems().setAll(list));
    }

    private void create() {
        String name = nameField.getText();
        String use = useField.getText();
        String misuse = misuseField.getText();
        runAsync(
                () -> api.createDevice(name, use, misuse == null || misuse.isBlank() ? null : misuse),
                created -> {
                    nameField.clear();
                    useField.clear();
                    misuseField.clear();
                    info("Creado: " + created.name());
                    refresh();
                });
    }

    private void deactivate() {
        DeviceDto selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        runAsync(
                () -> {
                    api.deactivateDevice(selected.id());
                    return null;
                },
                v -> {
                    info("Desactivado: " + selected.name());
                    refresh();
                });
    }

    /** Ejecuta la llamada a la API fuera del hilo de la interfaz. */
    private <T> void runAsync(Supplier<T> work, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return work.get();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> error(task.getException().getMessage()));
        Thread thread = new Thread(task, "api-call");
        thread.setDaemon(true);
        thread.start();
    }

    private void info(String message) {
        status.setStyle("");
        status.setText(message);
    }

    private void error(String message) {
        status.setStyle("-fx-text-fill: #b00020;");
        status.setText("Error: " + message);
    }
}