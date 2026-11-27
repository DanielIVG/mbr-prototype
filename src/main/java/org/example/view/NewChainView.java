package org.example.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.api.ComponentDto;
import org.example.api.CreateChainRequest;
import org.example.client.ApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pantalla 5 · Nueva cadena de riesgo: origen → peligro → secuencia de eventos →
 * situación peligrosa → daño. A la derecha se dibuja el camino de la falla mientras se escribe.
 */
public class NewChainView extends HBox {

    private record Option(Long id, String text) {
        @Override
        public String toString() {
            return text;
        }
    }

    private final class StepRow {
        final TextField text = new TextField();
        final ComboBox<Option> component = new ComboBox<>();
        final HBox box;

        StepRow() {
            text.setPromptText("Qué ocurre en este paso");
            component.getItems().addAll(options);
            component.setValue(NONE);
            HBox.setHgrow(text, Priority.ALWAYS);
            text.textProperty().addListener((o, a, b) -> refreshPath());
            component.setOnAction(e -> refreshPath());
            Button remove = Ui.button("Quitar", "btn-link", () -> removeStep(this));
            box = new HBox(8, text, component, remove);
            box.setAlignment(Pos.CENTER_LEFT);
        }
    }

    private static final Option NONE = new Option(null, "(sin componente)");

    private final ApiClient api;
    private final List<ComponentDto> components;
    private final Runnable onDone;
    private final Runnable onCancel;
    private final List<Option> options = new ArrayList<>();

    private final ComboBox<Option> origin = new ComboBox<>();
    private final TextArea hazard = new TextArea();
    private final TextArea situation = new TextArea();
    private final TextArea harm = new TextArea();
    private final TextField harmType = new TextField();
    private final List<StepRow> steps = new ArrayList<>();
    private final VBox stepsBox = new VBox(8);
    private final VBox pathHolder = new VBox();
    private final Label error = Ui.label("", "field-error");
    private final Button save;

    public NewChainView(ApiClient api, List<ComponentDto> components, Runnable onDone, Runnable onCancel) {
        this.api = api;
        this.components = components;
        this.onDone = onDone;
        this.onCancel = onCancel;
        setSpacing(20);
        setPadding(new Insets(20, 28, 20, 28));

        options.add(NONE);
        components.forEach(c -> options.add(new Option(c.id(), c.name())));
        origin.getItems().addAll(options);
        origin.setValue(NONE);
        origin.setMaxWidth(Double.MAX_VALUE);
        origin.setOnAction(e -> refreshPath());
        for (TextArea a : List.of(hazard, situation, harm)) {
            a.setPrefRowCount(2);
            a.setWrapText(true);
            a.textProperty().addListener((o, x, y) -> refreshPath());
        }
        hazard.setPromptText("Fuente potencial de daño. Ej.: energía eléctrica, fuerza mecánica, error de dosis");
        situation.setPromptText("Circunstancia en que alguien queda expuesto al peligro");
        harm.setPromptText("Lesión o daño a la salud. Ej.: sobredosis, quemadura, retraso del tratamiento");
        harmType.setPromptText("Tipo de daño (opcional)");

        addStep();

        save = Ui.button("Guardar cadena", "btn-primary", this::save);
        Button cancel = Ui.button("Cancelar", "btn-secondary", onCancel);
        HBox actions = new HBox(8, save, cancel);
        actions.setAlignment(Pos.CENTER_LEFT);
        error.setWrapText(true);

        VBox form = new VBox(14,
                card(1, "Origen: ¿de qué componente parte?", origin),
                card(2, "Peligro", hazard),
                card(3, "Secuencia de eventos", sequenceBlock()),
                card(4, "Situación peligrosa", situation),
                card(5, "Daño", new VBox(8, harm, harmType)),
                error, actions);
        form.setPadding(new Insets(0, 8, 8, 0));
        ScrollPane left = new ScrollPane(form);
        left.setFitToWidth(true);
        left.getStyleClass().add("plain-scroll");
        HBox.setHgrow(left, Priority.ALWAYS);

        VBox aside = new VBox(10, Ui.label("Camino de la falla", "form-title-sm"), pathHolder);
        aside.getStyleClass().add("card");
        aside.setPadding(new Insets(16, 18, 16, 18));
        aside.setPrefWidth(340);
        aside.setMinWidth(340);
        aside.setMaxWidth(340);
        ScrollPane right = new ScrollPane(aside);
        right.setFitToWidth(true);
        right.getStyleClass().add("plain-scroll");
        right.setPrefWidth(360);
        right.setMinWidth(360);

        getChildren().addAll(left, right);
        refreshPath();
    }

    private Node sequenceBlock() {
        Button add = Ui.button("+ Paso", "btn-secondary", () -> {
            addStep();
            refreshPath();
        });
        Label hint = Ui.label("Lo que ocurre, en orden, desde el origen hasta que alguien queda expuesto.", "hint");
        hint.setWrapText(true);
        return new VBox(8, hint, stepsBox, add);
    }

    private void removeStep(StepRow row) {
        if (steps.size() > 1) {
            steps.remove(row);
            stepsBox.getChildren().remove(row.box);
            refreshPath();
        }
    }

    private void addStep() {
        StepRow row = new StepRow();
        steps.add(row);
        stepsBox.getChildren().add(row.box);
    }

    private static Node card(int number, String title, Node content) {
        Label n = Ui.label(String.valueOf(number), "step-number");
        n.setMinWidth(26);
        n.setAlignment(Pos.CENTER);
        HBox head = new HBox(10, n, Ui.label(title, "field-label"));
        head.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(10, head, content);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14, 16, 14, 16));
        return box;
    }

    // ------------------------------------------------------------------ camino

    private String nameOf(Option o) {
        return o == null || o.id() == null ? "" : o.text();
    }

    private void refreshPath() {
        List<ChainPath.Step> path = new ArrayList<>();
        String from = nameOf(origin.getValue());
        path.add(new ChainPath.Step("ORIGEN", from.isEmpty() ? "(elige un componente)" : from, false));
        path.add(new ChainPath.Step("PELIGRO", hazard.getText(), false));
        int i = 1;
        for (StepRow s : steps) {
            String where = nameOf(s.component.getValue());
            String text = s.text.getText();
            path.add(new ChainPath.Step("PASO " + i++ + (where.isEmpty() ? "" : " · " + where), text, false));
        }
        path.add(new ChainPath.Step("SITUACIÓN PELIGROSA", situation.getText(), false));
        path.add(new ChainPath.Step("DAÑO", harm.getText(), true));
        pathHolder.getChildren().setAll(ChainPath.build(path));
    }

    // ------------------------------------------------------------------ guardar

    private void save() {
        error.setText("");
        if (origin.getValue() == null || origin.getValue().id() == null) {
            error.setText("Elige el componente de origen.");
            return;
        }
        if (hazard.getText().isBlank() || situation.getText().isBlank() || harm.getText().isBlank()) {
            error.setText("Completa el peligro, la situación peligrosa y el daño.");
            return;
        }
        List<CreateChainRequest.Step> list = new ArrayList<>();
        for (StepRow s : steps) {
            if (s.text.getText().isBlank()) {
                error.setText("Todos los pasos de la secuencia necesitan una descripción (o quita el paso vacío).");
                return;
            }
            Option c = s.component.getValue();
            list.add(new CreateChainRequest.Step(s.text.getText().trim(), c == null ? null : c.id()));
        }
        CreateChainRequest body = new CreateChainRequest(origin.getValue().id(), hazard.getText().trim(),
                null, null, situation.getText().trim(), harm.getText().trim(),
                harmType.getText().isBlank() ? null : harmType.getText().trim(), list);
        save.setDisable(true);
        Async.run(() -> api.createChain(body), ok -> onDone.run(),
                msg -> {
                    error.setText(msg);
                    save.setDisable(false);
                });
    }
}
