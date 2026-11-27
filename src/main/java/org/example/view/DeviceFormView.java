package org.example.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.api.DeviceDto;
import org.example.client.ApiClient;

import java.util.Objects;

/** Pantalla 2 · Registrar dispositivo (y editar sus datos si se recibe uno existente). */
public class DeviceFormView extends BorderPane {

    private final ApiClient api;
    private final Navigator nav;
    private final DeviceDto existing;

    private final TextField nameField = new TextField();
    private final TextArea useArea = new TextArea();
    private final TextArea misuseArea = new TextArea();
    private final Label nameError = Ui.label("", "field-error");
    private final Label useError = Ui.label("", "field-error");
    private final VBox saveError = new VBox(4);
    private final Label saveErrorText = new Label();
    private final Button saveButton;

    private final String initialName;
    private final String initialUse;
    private final String initialMisuse;

    /** @param existing null para crear un dispositivo nuevo */
    public DeviceFormView(ApiClient api, Navigator nav, DeviceDto existing) {
        this.api = api;
        this.nav = nav;
        this.existing = existing;

        initialName = existing == null ? "" : Objects.toString(existing.name(), "");
        initialUse = existing == null ? "" : Objects.toString(existing.intendedUse(), "");
        initialMisuse = existing == null ? "" : Objects.toString(existing.reasonablyForeseeableMisuse(), "");
        nameField.setText(initialName);
        useArea.setText(initialUse);
        misuseArea.setText(initialMisuse);

        saveButton = Ui.button(existing == null ? "Guardar y continuar" : "Guardar cambios",
                "btn-primary", this::save);

        setTop(buildTopBar());
        ScrollPane scroll = new ScrollPane(buildPage());
        scroll.setFitToWidth(true);
        setCenter(scroll);

        Platform.runLater(nameField::requestFocus);
    }

    private Node buildTopBar() {
        Button home = Ui.button("Inicio", "btn-link", this::cancel);
        HBox left = new HBox(6, home, Ui.label("/", "crumb-sep"),
                Ui.label(existing == null ? "Nuevo dispositivo" : "Editar dispositivo", "crumb-current"));
        left.setAlignment(Pos.CENTER_LEFT);
        return Ui.topBar(left, new Region());
    }

    private Node buildPage() {
        nameField.setPromptText("Por ejemplo: Bomba de infusión BI-200");

        useArea.setWrapText(true);
        useArea.setPrefRowCount(3);
        useArea.setPromptText("Qué hace el dispositivo, para quién y en qué entorno.");

        misuseArea.setWrapText(true);
        misuseArea.setPrefRowCount(4);
        misuseArea.setPromptText("Opcional. Por ejemplo: programar la dosis bajo presión de tiempo con unidades equivocadas.");

        Ui.show(nameError, false);
        Ui.show(useError, false);
        nameField.textProperty().addListener((o, a, b) -> clearError(nameError, nameField));
        useArea.textProperty().addListener((o, a, b) -> clearError(useError, useArea));

        saveError.getStyleClass().add("notice-error");
        saveErrorText.setWrapText(true);
        saveError.getChildren().addAll(Ui.label("No se pudo guardar", "notice-title"), saveErrorText);
        Ui.show(saveError, false);

        VBox form = new VBox(22,
                field("Nombre del dispositivo", true, nameField, null, nameError),
                field("Finalidad prevista", true, useArea, null, useError),
                field("Uso incorrecto razonablemente previsible", false, misuseArea,
                        "Describe comportamientos previsibles del usuario, no sabotaje. "
                                + "Define qué peligros habrá que analizar más adelante.", null),
                saveError);
        form.getStyleClass().add("card");
        form.setPadding(new Insets(28));

        Button cancel = Ui.button("Cancelar", "btn-secondary", this::cancel);
        HBox actions = new HBox(10, cancel, saveButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox page = new VBox(0,
                Ui.label(existing == null ? "Registrar dispositivo médico" : "Datos del dispositivo", "form-title"),
                spaced(Ui.label("Estos datos definen el alcance del análisis de riesgos. Se pueden editar después.",
                        "page-lead"), 6, 28),
                form, spaced(actions, 20, 0));
        page.setMaxWidth(720);
        page.setPrefWidth(720);

        HBox centered = new HBox(page);
        centered.setAlignment(Pos.TOP_CENTER);
        centered.setPadding(new Insets(44, 0, 44, 0));
        HBox.setHgrow(page, Priority.NEVER);
        return centered;
    }

    private static Node spaced(Node node, double top, double bottom) {
        VBox.setMargin(node, new Insets(top, 0, bottom, 0));
        return node;
    }

    private static VBox field(String labelText, boolean required, Node input, String hint, Label error) {
        HBox label = new HBox(4, Ui.label(labelText, "field-label"));
        if (required) {
            label.getChildren().add(Ui.label("*", "required-mark"));
        }
        VBox box = new VBox(6, label, input);
        if (error != null) {
            box.getChildren().add(error);
        }
        if (hint != null) {
            Label h = Ui.label(hint, "hint");
            h.setWrapText(true);
            box.getChildren().add(h);
        }
        return box;
    }

    private static void clearError(Label error, Node input) {
        Ui.show(error, false);
        input.getStyleClass().remove("field-invalid");
    }

    private static void markInvalid(Label error, Node input, String message) {
        error.setText(message);
        Ui.show(error, true);
        if (!input.getStyleClass().contains("field-invalid")) {
            input.getStyleClass().add("field-invalid");
        }
    }

    private boolean isDirty() {
        return !text(nameField.getText()).equals(initialName.trim())
                || !text(useArea.getText()).equals(initialUse.trim())
                || !text(misuseArea.getText()).equals(initialMisuse.trim());
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private void cancel() {
        if (isDirty() && !Ui.confirm("Hay cambios sin guardar. ¿Descartarlos?", "Descartar")) {
            return;
        }
        if (existing == null) {
            nav.showHome();
        } else {
            nav.showWorkspace(existing);
        }
    }

    private void save() {
        String name = text(nameField.getText());
        String use = text(useArea.getText());
        String misuse = text(misuseArea.getText());

        boolean valid = true;
        if (name.isEmpty()) {
            markInvalid(nameError, nameField, "Escribe el nombre del dispositivo.");
            valid = false;
        }
        if (use.isEmpty()) {
            markInvalid(useError, useArea, "Describe la finalidad prevista del dispositivo.");
            valid = false;
        }
        if (!valid) {
            return;
        }

        Ui.show(saveError, false);
        saveButton.setDisable(true);
        saveButton.setText("Guardando…");
        String misuseOrNull = misuse.isEmpty() ? null : misuse;

        Async.run(
                () -> existing == null
                        ? api.createDevice(name, use, misuseOrNull)
                        : api.updateDevice(existing.id(), name, use, misuseOrNull),
                saved -> nav.showWorkspace(saved),
                message -> {
                    saveErrorText.setText(message);
                    Ui.show(saveError, true);
                    saveButton.setDisable(false);
                    saveButton.setText(existing == null ? "Guardar y continuar" : "Guardar cambios");
                });
    }
}
