package org.example.view;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.example.api.ComponentDto;
import org.example.api.DeviceDto;
import org.example.api.RiskChainDto;
import org.example.api.TraceabilityDtos.ControlDto;
import org.example.api.TraceabilityDtos.MatrixDto;
import org.example.api.TraceabilityDtos.RequirementDto;
import org.example.api.TraceabilityDtos.RequirementRequest;
import org.example.api.TraceabilityDtos.TraceRowDto;
import org.example.client.ApiClient;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Trazabilidad del dispositivo: requisitos ↔ medidas de control ↔ pruebas (verificaciones) ↔ daños,
 * con la matriz completa y las consultas hacia atrás (qué componentes recorrió un daño) y hacia
 * delante (a qué daños lleva un componente).
 */
public class TraceabilityView extends VBox {

    private static final List<String> TYPES =
            List.of("FUNCIONAL", "SEGURIDAD", "USABILIDAD", "DESEMPENO", "REGULATORIO");

    private final ApiClient api;
    private final DeviceDto device;
    private final HBox content = new HBox(20);
    private final VBox aside = new VBox(12);

    private List<RequirementDto> requirements = List.of();
    private List<ControlDto> controls = List.of();
    private List<ComponentDto> components = List.of();
    private List<RiskChainDto> chains = List.of();
    private MatrixDto matrix;
    private Long selectedId;
    private boolean editing;          // el formulario está abierto
    private RequirementDto editTarget; // null = requisito nuevo

    public TraceabilityView(ApiClient api, DeviceDto device) {
        this.api = api;
        this.device = device;
        setPadding(new Insets(20, 28, 20, 28));
        setSpacing(12);
        VBox.setVgrow(content, Priority.ALWAYS);
        getChildren().add(content);
        load();
    }

    private void load() {
        content.getChildren().setAll(Ui.label("Cargando trazabilidad…", "hint"));
        long id = device.id();
        Async.run(() -> {
            List<ComponentDto> comps = api.listComponents(id);
            Set<Long> ids = comps.stream().map(ComponentDto::id).collect(Collectors.toSet());
            List<RiskChainDto> mine = api.listChains().stream()
                    .filter(c -> c.hazard().modelElementId() != null && ids.contains(c.hazard().modelElementId()))
                    .toList();
            return new Object[] {api.listRequirements(id), api.traceability(id), api.listDeviceControls(id),
                    comps, mine};
        }, data -> {
            @SuppressWarnings("unchecked") List<RequirementDto> r = (List<RequirementDto>) data[0];
            @SuppressWarnings("unchecked") List<ControlDto> k = (List<ControlDto>) data[2];
            @SuppressWarnings("unchecked") List<ComponentDto> c = (List<ComponentDto>) data[3];
            @SuppressWarnings("unchecked") List<RiskChainDto> ch = (List<RiskChainDto>) data[4];
            requirements = r;
            matrix = (MatrixDto) data[1];
            controls = k;
            components = c;
            chains = ch;
            showAll();
        }, msg -> {
            Label l = Ui.label("No se pudo cargar la trazabilidad: " + msg, "field-error");
            l.setWrapText(true);
            content.getChildren().setAll(new VBox(8, l, Ui.button("Reintentar", "btn-secondary", this::load)));
        });
    }

    // ------------------------------------------------------------------ pantalla

    private void showAll() {
        var s = matrix.summary();
        Label summary = Ui.label(s.requirements() + " requisitos · " + s.requirementsWithControl()
                + " con medida de control · " + s.controlsVerified() + " de " + s.controls()
                + " medidas con prueba", "table-head");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox head = new HBox(10, summary, spacer,
                Ui.button("+ Nuevo requisito", "btn-primary", () -> openForm(null)));
        head.setAlignment(Pos.CENTER_LEFT);

        VBox list = new VBox(8);
        for (RequirementDto r : requirements) {
            list.getChildren().add(row(r));
        }
        if (requirements.isEmpty()) {
            VBox empty = Ui.emptyState(Ui.ICON_LINK,
                    "Aún no hay requisitos registrados",
                    "Registra un requisito y enlázalo a la medida de control que lo cumple para trazar el camino: requisito → control → prueba → daño.");
            list.getChildren().add(empty);
        }
        ScrollPane sp = new ScrollPane(list);
        sp.setFitToWidth(true);
        sp.setPrefHeight(230);
        sp.getStyleClass().add("plain-scroll");

        Label gaps = Ui.label(gapsText(), "hint");
        gaps.setWrapText(true);

        TableView<TraceRowDto> table = matrixTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox left = new VBox(12, head, sp, Ui.label("Matriz de trazabilidad", "table-head"), table, gaps);
        HBox.setHgrow(left, Priority.ALWAYS);

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
        content.getChildren().setAll(left, right);
        renderAside();
    }

    private String gapsText() {
        int noControl = matrix.requirementsWithoutControl().size();
        int noRequirement = matrix.controlsWithoutRequirement().size();
        if (noControl == 0 && noRequirement == 0) {
            return "Sin huecos: todo requisito tiene medida de control y toda medida tiene requisito.";
        }
        return "Huecos: " + noControl + " requisito(s) sin medida de control y " + noRequirement
                + " medida(s) de control sin requisito.";
    }

    private HBox row(RequirementDto r) {
        Label title = Ui.label(r.code() + " · " + r.description(), "row-title");
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS);
        long linked = matrix.rows().stream().filter(t -> t.requirementId().equals(r.id()))
                .map(TraceRowDto::controlId).distinct().count();
        Label coverage = Ui.label(linked == 0 ? "Sin medida de control"
                : linked + (linked == 1 ? " medida" : " medidas"), linked == 0 ? "tag-warn" : "tag-ok");
        Label type = Ui.label(r.type(), "tag");
        HBox box = new HBox(10, title, type, coverage);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("list-row");
        if (Objects.equals(r.id(), selectedId)) {
            box.getStyleClass().add("list-row-selected");
        }
        box.setOnMouseClicked(e -> {
            selectedId = r.id();
            editing = false;
            showAll();
        });
        return box;
    }

    private TableView<TraceRowDto> matrixTable() {
        TableView<TraceRowDto> table = new TableView<>();
        table.setPlaceholder(new Label("Aún no hay requisitos enlazados a medidas de control."));
        table.getColumns().add(col("Requisito", 90, TraceRowDto::requirementCode));
        table.getColumns().add(col("Medida de control", 200, TraceRowDto::controlDescription));
        table.getColumns().add(col("Pruebas", 120, r -> r.verifications() == 0 ? "Sin prueba"
                : r.verifications() + " (" + r.effectiveVerifications() + " efectiva"
                + (r.effectiveVerifications() == 1 ? "" : "s") + ")"));
        table.getColumns().add(col("Daño", 200, TraceRowDto::harmDescription));
        table.getColumns().add(col("Peligro", 180, TraceRowDto::hazardDescription));
        table.getColumns().add(col("Origen", 120, r -> Objects.toString(r.elementName(), "—")));
        table.getItems().setAll(matrix.rows());
        return table;
    }

    private static TableColumn<TraceRowDto, String> col(String title, double width,
                                                        Function<TraceRowDto, String> value) {
        TableColumn<TraceRowDto, String> c = new TableColumn<>(title);
        c.setCellValueFactory(x -> new ReadOnlyStringWrapper(Objects.toString(value.apply(x.getValue()), "")));
        c.setPrefWidth(width);
        return c;
    }

    // ------------------------------------------------------------------ panel derecho

    private void renderAside() {
        aside.getChildren().clear();
        if (editing) {
            aside.getChildren().add(form());
        } else {
            RequirementDto sel = requirements.stream().filter(r -> r.id().equals(selectedId)).findFirst().orElse(null);
            if (sel == null) {
                Label hint = Ui.label("Selecciona un requisito para ver y editar sus medidas de control.", "hint");
                hint.setWrapText(true);
                aside.getChildren().add(hint);
            } else {
                aside.getChildren().add(detail(sel));
            }
        }
        aside.getChildren().add(queries());
    }

    private Node detail(RequirementDto r) {
        Label desc = Ui.label(r.description(), "muted");
        desc.setWrapText(true);
        VBox box = new VBox(8, Ui.label(r.code(), "form-title-sm"), Ui.label(r.type(), "tag"), desc);
        if (r.source() != null) {
            Label src = Ui.label("Fuente: " + r.source(), "hint");
            src.setWrapText(true);
            box.getChildren().add(src);
        }
        HBox actions = new HBox(8, Ui.button("Editar", "btn-secondary", () -> openForm(r)),
                Ui.button("Desactivar", "btn-secondary", () -> deactivate(r)));
        box.getChildren().add(actions);

        box.getChildren().add(Ui.label("Medidas de control enlazadas", "table-head"));
        List<TraceRowDto> mine = matrix.rows().stream().filter(t -> t.requirementId().equals(r.id())).toList();
        if (mine.isEmpty()) {
            box.getChildren().add(Ui.label("Ninguna todavía.", "hint"));
        }
        for (TraceRowDto t : mine) {
            Label text = Ui.label(t.controlDescription() + "\n→ " + t.harmDescription() + "\n"
                    + (t.verifications() == 0 ? "Sin prueba" : t.verifications() + " prueba(s), "
                    + t.effectiveVerifications() + " efectiva(s)"), "muted");
            text.setWrapText(true);
            HBox.setHgrow(text, Priority.ALWAYS);
            Button unlink = Ui.button("Quitar", "btn-link", () -> unlink(r, t.controlId()));
            HBox line = new HBox(8, text, unlink);
            line.setAlignment(Pos.TOP_LEFT);
            box.getChildren().add(line);
        }

        Set<Long> linked = new HashSet<>();
        mine.forEach(t -> linked.add(t.controlId()));
        List<ControlDto> available = controls.stream().filter(k -> !linked.contains(k.controlId())).toList();
        ComboBox<ControlDto> picker = new ComboBox<>();
        picker.getItems().setAll(available);
        picker.setPromptText(controls.isEmpty() ? "No hay medidas de control" : "Elegir medida de control");
        picker.setMaxWidth(Double.MAX_VALUE);
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(ControlDto k) {
                return k == null ? "" : "#" + k.controlId() + " · " + k.description();
            }

            @Override
            public ControlDto fromString(String s) {
                return null;
            }
        });
        Label error = Ui.label("", "field-error");
        error.setWrapText(true);
        Button link = Ui.button("Vincular", "btn-primary", () -> {
            ControlDto k = picker.getValue();
            if (k == null) {
                error.setText("Elige una medida de control.");
                return;
            }
            Async.run(() -> {
                api.linkRequirement(r.id(), k.controlId());
                return Boolean.TRUE;
            }, ok -> load(), error::setText);
        });
        link.setDisable(available.isEmpty());
        box.getChildren().addAll(picker, link, error);
        return box;
    }

    private void unlink(RequirementDto r, Long controlId) {
        Async.run(() -> {
            api.unlinkRequirement(r.id(), controlId);
            return Boolean.TRUE;
        }, ok -> load(), msg -> aside.getChildren().add(Ui.label(msg, "field-error")));
    }

    private void deactivate(RequirementDto r) {
        if (!Ui.confirm("¿Desactivar el requisito " + r.code() + "? Saldrá de la lista y de la matriz.",
                "Desactivar")) {
            return;
        }
        Async.run(() -> {
            api.deactivateRequirement(r.id());
            return Boolean.TRUE;
        }, ok -> {
            selectedId = null;
            load();
        }, msg -> aside.getChildren().add(Ui.label(msg, "field-error")));
    }

    // ------------------------------------------------------------------ formulario

    private void openForm(RequirementDto target) {
        editTarget = target;
        editing = true;
        renderAside();
    }

    private Node form() {
        TextField code = new TextField(editTarget == null ? "" : editTarget.code());
        code.setPromptText("Ej.: REQ-001");
        ComboBox<String> type = new ComboBox<>();
        type.getItems().setAll(TYPES);
        type.setValue(editTarget == null ? TYPES.get(1) : editTarget.type());
        type.setMaxWidth(Double.MAX_VALUE);
        TextArea description = new TextArea(editTarget == null ? "" : editTarget.description());
        description.setPrefRowCount(4);
        description.setWrapText(true);
        description.setPromptText("Qué debe cumplir el dispositivo");
        TextField source = new TextField(editTarget == null || editTarget.source() == null ? ""
                : editTarget.source());
        source.setPromptText("Fuente (norma, documento…) — opcional");
        Label error = Ui.label("", "field-error");
        error.setWrapText(true);

        Button save = Ui.button("Guardar", "btn-primary", () -> {
            RequirementRequest body = new RequirementRequest(device.id(), code.getText(), description.getText(),
                    type.getValue(), source.getText());
            Async.run(() -> editTarget == null ? api.createRequirement(body)
                            : api.updateRequirement(editTarget.id(), body),
                    saved -> {
                        selectedId = saved.id();
                        editing = false;
                        load();
                    }, error::setText);
        });
        Button cancel = Ui.button("Cancelar", "btn-secondary", () -> {
            editing = false;
            renderAside();
        });
        return new VBox(8, Ui.label(editTarget == null ? "Nuevo requisito" : "Editar requisito", "form-title-sm"),
                Ui.label("Código", "field-label"), code, Ui.label("Tipo", "field-label"), type,
                Ui.label("Descripción", "field-label"), description, Ui.label("Fuente", "field-label"), source,
                error, new HBox(8, save, cancel));
    }

    // ------------------------------------------------------------------ consultas hacia atrás / delante

    private Node queries() {
        ComboBox<ComponentDto> comp = new ComboBox<>();
        comp.getItems().setAll(components);
        comp.setPromptText("Componente");
        comp.setMaxWidth(Double.MAX_VALUE);
        comp.setConverter(new StringConverter<>() {
            @Override
            public String toString(ComponentDto c) {
                return c == null ? "" : c.name();
            }

            @Override
            public ComponentDto fromString(String s) {
                return null;
            }
        });
        VBox forwardResult = new VBox(4);
        comp.setOnAction(e -> {
            ComponentDto c = comp.getValue();
            if (c == null) {
                return;
            }
            forwardResult.getChildren().setAll(Ui.label("Buscando…", "hint"));
            Async.run(() -> api.harmsOfComponent(c.id()), list -> {
                forwardResult.getChildren().clear();
                if (list.isEmpty()) {
                    forwardResult.getChildren().add(Ui.label("Este componente no lleva a ningún daño.", "hint"));
                }
                list.forEach(h -> forwardResult.getChildren().add(wrapped("→ " + h.harmDescription()
                        + "  (peligro: " + h.hazardDescription() + ")")));
            }, msg -> forwardResult.getChildren().setAll(Ui.label(msg, "field-error")));
        });

        ComboBox<RiskChainDto> harm = new ComboBox<>();
        harm.getItems().setAll(chains);
        harm.setPromptText("Daño");
        harm.setMaxWidth(Double.MAX_VALUE);
        harm.setConverter(new StringConverter<>() {
            @Override
            public String toString(RiskChainDto c) {
                return c == null ? "" : c.harm().description();
            }

            @Override
            public RiskChainDto fromString(String s) {
                return null;
            }
        });
        VBox backwardResult = new VBox(4);
        harm.setOnAction(e -> {
            RiskChainDto c = harm.getValue();
            if (c == null) {
                return;
            }
            backwardResult.getChildren().setAll(Ui.label("Buscando…", "hint"));
            Async.run(() -> api.componentsOfHarm(c.harm().id()), list -> {
                backwardResult.getChildren().clear();
                if (list.isEmpty()) {
                    backwardResult.getChildren().add(Ui.label("El daño no tiene componentes asociados.", "hint"));
                }
                int i = 1;
                for (var el : list) {
                    backwardResult.getChildren().add(wrapped((i == 1 ? "Origen: " : "Paso: ") + el.name()));
                    i++;
                }
            }, msg -> backwardResult.getChildren().setAll(Ui.label(msg, "field-error")));
        });

        return new VBox(8, Ui.label("Consultas de trazabilidad", "form-title-sm"),
                Ui.label("Hacia delante: ¿a qué daños lleva un componente?", "table-head"), comp, forwardResult,
                Ui.label("Hacia atrás: ¿qué componentes recorrió un daño?", "table-head"), harm, backwardResult);
    }

    private static Label wrapped(String text) {
        Label l = Ui.label(text, "muted");
        l.setWrapText(true);
        return l;
    }
}
