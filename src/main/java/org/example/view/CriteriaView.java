package org.example.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.api.AssessmentDtos.CellDto;
import org.example.api.AssessmentDtos.CriteriaDto;
import org.example.api.AssessmentDtos.CriterionDto;
import org.example.api.AssessmentDtos.ProbabilityDto;
import org.example.api.AssessmentDtos.SeverityDto;
import org.example.client.ApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pantalla 6 · Criterios de aceptabilidad (plan de gestión de riesgos): unidad de exposición,
 * escala de severidad, bandas de probabilidad, matriz y criterios con justificación.
 * Una vez que existe una estimación, los criterios quedan bloqueados.
 */
public class CriteriaView extends ScrollPane {

    private static final List<String> LEVELS = List.of("Aceptable", "Tolerable", "Inaceptable");

    private final ApiClient api;
    private final Runnable onSaved;
    private final VBox page = new VBox(14);
    private final Label message = Ui.label("", "field-error");

    private final TextField unit = new TextField();
    private final List<TextField> severityNames = new ArrayList<>();
    private final List<TextField[]> probabilityRows = new ArrayList<>();      // nombre, desde, hasta
    private final List<ComboBox<String>[]> matrix = new ArrayList<>();         // fila = severidad
    private final List<CheckBox> acceptable = new ArrayList<>();
    private final List<TextField> justifications = new ArrayList<>();
    private boolean locked;
    private Button save;

    public CriteriaView(ApiClient api, Runnable onSaved) {
        this.api = api;
        this.onSaved = onSaved;
        page.setPadding(new Insets(20, 28, 28, 28));
        setContent(page);
        setFitToWidth(true);
        getStyleClass().add("plain-scroll");
        load();
    }

    private void load() {
        page.getChildren().setAll(Ui.label("Cargando criterios…", "hint"));
        Async.run(api::getCriteria, this::build, msg -> {
            Label l = Ui.label("No se pudieron cargar los criterios: " + msg, "field-error");
            l.setWrapText(true);
            page.getChildren().setAll(l, Ui.button("Reintentar", "btn-secondary", this::load));
        });
    }

    private void build(CriteriaDto existing) {
        severityNames.clear();
        probabilityRows.clear();
        matrix.clear();
        acceptable.clear();
        justifications.clear();
        locked = existing != null && existing.locked();
        CriteriaDto d = existing != null ? existing : template();
        boolean isTemplate = existing == null;

        page.getChildren().clear();
        Label title = Ui.label("Criterios de aceptabilidad del riesgo", "form-title");
        Label lead = Ui.label("Defínelos antes de evaluar riesgos. Son decisión del fabricante y deben "
                + "justificarse: ISO 14971 no fija una matriz obligatoria.", "page-lead");
        lead.setWrapText(true);
        page.getChildren().addAll(title, lead);
        if (isTemplate) {
            Label n = Ui.label("Plantilla de EJEMPLO (no es normativa): los valores son una propuesta para empezar. "
                    + "Ajústalos a la política de riesgo de tu organización y justifica cada criterio.", "notice-warn");
            n.setWrapText(true);
            page.getChildren().add(n);
        }
        if (locked) {
            Label n = Ui.label("Ya existen estimaciones de riesgo hechas con estos criterios: están bloqueados "
                    + "para no invalidarlas.", "notice-warn");
            n.setWrapText(true);
            page.getChildren().add(n);
        }

        unit.setText(d.exposureUnit());
        page.getChildren().add(section("1 · Unidad de exposición de la probabilidad",
                "Las probabilidades se expresan por esta unidad (por hora, por ciclo, por uso…).", unit));

        VBox sev = new VBox(6);
        for (SeverityDto s : d.severities()) {
            TextField t = new TextField(s.descriptor());
            severityNames.add(t);
            HBox row = new HBox(10, Ui.label("Nivel " + s.value(), "hint"), t);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(t, Priority.ALWAYS);
            sev.getChildren().add(row);
        }
        page.getChildren().add(section("2 · Escala de severidad", "Del menos grave (1) al más grave.", sev));

        VBox prob = new VBox(6);
        for (ProbabilityDto p : d.probabilities()) {
            TextField name = new TextField(p.descriptor());
            TextField from = new TextField(fmt(p.min()));
            TextField to = new TextField(fmt(p.max()));
            from.setPrefWidth(110);
            to.setPrefWidth(110);
            probabilityRows.add(new TextField[] {name, from, to});
            HBox row = new HBox(10, Ui.label("Nivel " + p.value(), "hint"), name,
                    Ui.label("desde", "hint"), from, Ui.label("hasta (excl.)", "hint"), to);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(name, Priority.ALWAYS);
            prob.getChildren().add(row);
        }
        page.getChildren().add(section("3 · Bandas de probabilidad",
                "Sin huecos ni traslapes: la primera empieza en 0 y la última termina en 1. Cada banda incluye su "
                        + "límite inferior (la última incluye 1).", prob));

        page.getChildren().add(section("4 · Matriz severidad × probabilidad",
                "Elige el nivel de riesgo de cada celda.", matrixGrid(d)));

        VBox crit = new VBox(8);
        for (String level : LEVELS) {
            CriterionDto c = d.criteria().stream().filter(x -> x.riskLevel().equals(level)).findFirst().orElse(null);
            CheckBox ok = new CheckBox("Aceptable sin más acciones");
            ok.setSelected(c != null && c.acceptable());
            TextField why = new TextField(c == null ? "" : Objects.toString(c.justification(), ""));
            why.setPromptText("Justificación");
            acceptable.add(ok);
            justifications.add(why);
            HBox row = new HBox(10, Ui.label(level, "field-label"), ok, why);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(why, Priority.ALWAYS);
            crit.getChildren().add(row);
        }
        page.getChildren().add(section("5 · Criterios por nivel de riesgo",
                "Qué niveles se aceptan y por qué. Lo no aceptable exige medidas de control.", crit));

        message.setWrapText(true);
        save = Ui.button("Guardar criterios", "btn-primary", this::save);
        save.setDisable(locked);
        if (locked) {
            setDisabledAll(true);
        }
        page.getChildren().addAll(message, save);
    }

    private void setDisabledAll(boolean disabled) {
        unit.setDisable(disabled);
        severityNames.forEach(t -> t.setDisable(disabled));
        probabilityRows.forEach(r -> { for (TextField t : r) { t.setDisable(disabled); } });
        matrix.forEach(r -> { for (ComboBox<String> c : r) { c.setDisable(disabled); } });
        acceptable.forEach(c -> c.setDisable(disabled));
        justifications.forEach(t -> t.setDisable(disabled));
    }

    @SuppressWarnings("unchecked")
    private Node matrixGrid(CriteriaDto d) {
        GridPane grid = new GridPane();
        grid.setHgap(6);
        for (int p = 0; p < d.probabilities().size(); p++) {
            grid.add(Ui.label("P" + d.probabilities().get(p).value(), "table-head"), p + 1, 0);
        }
        int rows = d.severities().size();
        for (int r = 0; r < rows; r++) {
            SeverityDto s = d.severities().get(rows - 1 - r);                 // la más grave arriba
            grid.add(Ui.label("S" + s.value(), "table-head"), 0, r + 1);
            ComboBox<String>[] rowBoxes = new ComboBox[d.probabilities().size()];
            for (int p = 0; p < rowBoxes.length; p++) {
                ComboBox<String> cb = new ComboBox<>();
                cb.getItems().addAll(LEVELS);
                int sv = s.value();
                int pv = d.probabilities().get(p).value();
                cb.setValue(d.cells().stream().filter(c -> c.severity() == sv && c.probability() == pv)
                        .map(CellDto::riskLevel).findFirst().orElse(LEVELS.get(0)));
                rowBoxes[p] = cb;
                grid.add(cb, p + 1, r + 1);
            }
            matrix.add(rowBoxes);                                              // índice 0 = severidad más grave
        }
        return grid;
    }

    private static Node section(String title, String hint, Node content) {
        Label h = Ui.label(hint, "hint");
        h.setWrapText(true);
        VBox box = new VBox(8, Ui.label(title, "field-label"), h, content);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14, 16, 14, 16));
        return box;
    }

    private static String fmt(double v) {
        return v == 0 ? "0" : v == 1 ? "1" : Double.toString(v);
    }

    private static double parse(String text, String what) {
        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("'" + text + "' no es un número válido en " + what);
        }
    }

    // ------------------------------------------------------------------ guardar

    private void save() {
        message.setText("");
        CriteriaDto body;
        try {
            body = collect();
        } catch (IllegalArgumentException e) {
            message.setText(e.getMessage());
            return;
        }
        save.setDisable(true);
        Async.run(() -> api.saveCriteria(body), ok -> {
            onSaved.run();
            build(ok);
            page.getChildren().add(Ui.label("Criterios guardados.", "hint"));
        }, msg -> {
            message.setText(msg);
            save.setDisable(false);
        });
    }

    private CriteriaDto collect() {
        if (unit.getText().isBlank()) {
            throw new IllegalArgumentException("Indica la unidad de exposición.");
        }
        List<SeverityDto> sev = new ArrayList<>();
        for (int i = 0; i < severityNames.size(); i++) {
            if (severityNames.get(i).getText().isBlank()) {
                throw new IllegalArgumentException("Todos los niveles de severidad necesitan un nombre.");
            }
            sev.add(new SeverityDto(i + 1, severityNames.get(i).getText().trim()));
        }
        List<ProbabilityDto> prob = new ArrayList<>();
        for (int i = 0; i < probabilityRows.size(); i++) {
            TextField[] r = probabilityRows.get(i);
            if (r[0].getText().isBlank()) {
                throw new IllegalArgumentException("Todos los niveles de probabilidad necesitan un nombre.");
            }
            prob.add(new ProbabilityDto(i + 1, r[0].getText().trim(),
                    parse(r[1].getText(), "«desde» del nivel " + (i + 1)),
                    parse(r[2].getText(), "«hasta» del nivel " + (i + 1))));
        }
        List<CellDto> cells = new ArrayList<>();
        int rows = matrix.size();
        for (int r = 0; r < rows; r++) {
            int severity = rows - r;
            for (int p = 0; p < matrix.get(r).length; p++) {
                cells.add(new CellDto(severity, p + 1, matrix.get(r)[p].getValue()));
            }
        }
        List<CriterionDto> crit = new ArrayList<>();
        for (int i = 0; i < LEVELS.size(); i++) {
            crit.add(new CriterionDto(LEVELS.get(i), acceptable.get(i).isSelected(),
                    justifications.get(i).getText().trim()));
        }
        return new CriteriaDto(unit.getText().trim(), sev, prob, cells, crit, false);
    }

    /** Plantilla de EJEMPLO 5×5 (no normativa). */
    private static CriteriaDto template() {
        List<SeverityDto> sev = List.of(new SeverityDto(1, "Despreciable"), new SeverityDto(2, "Menor"),
                new SeverityDto(3, "Seria"), new SeverityDto(4, "Crítica"), new SeverityDto(5, "Catastrófica"));
        List<ProbabilityDto> prob = List.of(new ProbabilityDto(1, "Improbable", 0.0, 1e-7),
                new ProbabilityDto(2, "Remota", 1e-7, 1e-5), new ProbabilityDto(3, "Ocasional", 1e-5, 1e-3),
                new ProbabilityDto(4, "Probable", 1e-3, 1e-1), new ProbabilityDto(5, "Frecuente", 1e-1, 1.0));
        List<CellDto> cells = new ArrayList<>();
        for (int s = 1; s <= 5; s++) {
            int tolerableAt = 6 - s;
            for (int p = 1; p <= 5; p++) {
                cells.add(new CellDto(s, p, p < tolerableAt ? "Aceptable" : p == tolerableAt ? "Tolerable" : "Inaceptable"));
            }
        }
        List<CriterionDto> crit = List.of(new CriterionDto("Aceptable", true, ""),
                new CriterionDto("Tolerable", false, ""), new CriterionDto("Inaceptable", false, ""));
        return new CriteriaDto("por hora", sev, prob, cells, crit, false);
    }
}
