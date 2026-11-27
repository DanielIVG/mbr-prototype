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
import javafx.scene.layout.VBox;
import org.example.api.BehaviorDtos.CreateNodeRequest;
import org.example.api.BehaviorDtos.NodeDto;
import org.example.api.ComponentDto;
import org.example.api.DeviceDto;
import org.example.client.ApiClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Comportamiento de un componente: árbol de acciones y condiciones SÍ/NO. Cada camino de la raíz (el peligro)
 * a un resultado peligroso genera una cadena de riesgo (peligro → secuencia → situación peligrosa → daño).
 * Un camino que ya generó una cadena no se edita ni se elimina: se agrega una rama nueva.
 */
public class BehaviorView extends HBox {

    private record Opt(String code, String text) {
        @Override
        public String toString() {
            return text;
        }
    }

    private record CompOpt(Long id, String text) {
        @Override
        public String toString() {
            return text;
        }
    }

    private static final List<Opt> ROOT_KINDS = List.of(
            new Opt("ACTION", "Acción: algo ocurre o falla"),
            new Opt("CONDITION", "Condición: pregunta con salida SÍ / NO"));
    private static final List<Opt> ALL_KINDS = List.of(ROOT_KINDS.get(0), ROOT_KINDS.get(1),
            new Opt("OUTCOME_SAFE", "Resultado seguro: termina sin daño"),
            new Opt("OUTCOME_HAZARD", "Resultado peligroso: situación peligrosa y daño"));

    private final ApiClient api;
    private final DeviceDto device;

    private List<ComponentDto> components = List.of();
    private Long selected;
    private Long highlight;
    private List<NodeDto> nodes = List.of();

    private final VBox listBox = new VBox(6);
    private final VBox editor = new VBox(12);
    private final VBox formHolder = new VBox();
    private final Label error = Ui.label("", "field-error");

    public BehaviorView(ApiClient api, DeviceDto device, Long componentId, Long nodeId) {
        this.api = api;
        this.device = device;
        this.selected = componentId;
        this.highlight = nodeId;
        setSpacing(20);
        setPadding(new Insets(20, 28, 20, 28));

        ScrollPane left = new ScrollPane(listBox);
        left.setFitToWidth(true);
        left.getStyleClass().add("plain-scroll");
        left.setPrefWidth(300);
        left.setMinWidth(300);
        left.setMaxWidth(300);
        ScrollPane main = new ScrollPane(editor);
        main.setFitToWidth(true);
        main.getStyleClass().add("plain-scroll");
        HBox.setHgrow(main, Priority.ALWAYS);
        getChildren().addAll(left, main);
        error.setWrapText(true);
        loadComponents();
    }

    // ------------------------------------------------------------------ carga

    private void loadComponents() {
        listBox.getChildren().setAll(Ui.label("Cargando…", "hint"));
        Async.run(() -> api.listComponents(device.id()), comps -> {
            components = comps;
            renderList();
            if (selected != null) {
                loadNodes();
            } else {
                renderEditor();
            }
        }, msg -> listBox.getChildren().setAll(Ui.label("No se pudo cargar: " + msg, "field-error")));
    }

    private void loadNodes() {
        long id = selected;
        Async.run(() -> api.listBehavior(id), list -> {
            nodes = list;
            renderEditor();
        }, msg -> {
            error.setText(msg);
            renderEditor();
        });
    }

    private void select(Long componentId) {
        selected = componentId;
        highlight = null;
        formHolder.getChildren().clear();
        error.setText("");
        renderList();
        loadNodes();
    }

    // ------------------------------------------------------------------ lista de componentes

    private void renderList() {
        listBox.getChildren().clear();
        listBox.getChildren().add(Ui.label("Componentes", "table-head"));
        if (components.isEmpty()) {
            listBox.getChildren().add(Ui.label("Primero agregue componentes en la sección Estructura.", "hint"));
            return;
        }
        Map<Long, List<ComponentDto>> kids = new HashMap<>();
        List<ComponentDto> roots = new ArrayList<>();
        var ids = components.stream().map(ComponentDto::id).toList();
        for (ComponentDto c : components) {
            if (c.parentComponentId() == null || !ids.contains(c.parentComponentId())) {
                roots.add(c);
            } else {
                kids.computeIfAbsent(c.parentComponentId(), k -> new ArrayList<>()).add(c);
            }
        }
        roots.sort(Comparator.comparing(ComponentDto::name));
        for (ComponentDto r : roots) {
            addComponentRow(r, 0, kids);
        }
    }

    private void addComponentRow(ComponentDto c, int depth, Map<Long, List<ComponentDto>> kids) {
        Label name = Ui.label(c.name(), "row-title");
        HBox row = new HBox(name);
        row.getStyleClass().add("list-row");
        if (Objects.equals(c.id(), selected)) {
            row.getStyleClass().add("list-row-selected");
        }
        row.setPadding(new Insets(8, 10, 8, 10 + depth * 18));
        row.setOnMouseClicked(e -> select(c.id()));
        listBox.getChildren().add(row);
        kids.getOrDefault(c.id(), List.of()).stream().sorted(Comparator.comparing(ComponentDto::name))
                .forEach(k -> addComponentRow(k, depth + 1, kids));
    }

    // ------------------------------------------------------------------ editor

    private String componentName(Long id) {
        return components.stream().filter(c -> c.id().equals(id)).map(ComponentDto::name).findFirst().orElse("?");
    }

    private void renderEditor() {
        editor.getChildren().clear();
        if (selected == null) {
            Label hint = Ui.label("Elija un componente para ver y editar su comportamiento.", "hint");
            hint.setWrapText(true);
            editor.getChildren().add(hint);
            return;
        }
        Label title = Ui.label("Comportamiento · " + componentName(selected), "form-title-sm");
        Label help = Ui.label("La raíz es el peligro. Una condición se ramifica en SÍ y NO; una acción sigue a un solo "
                + "paso. Un resultado peligroso (situación peligrosa y daño) cierra una cadena de riesgo. Se muestran "
                + "los árboles cuya raíz o algún nodo está en este componente.", "hint");
        help.setWrapText(true);
        Button newRoot = Ui.button("+ Nuevo peligro (raíz)", "btn-secondary", () -> openForm(null, null, null));
        editor.getChildren().addAll(title, help, error, newRoot, formHolder);

        Map<Long, List<NodeDto>> kids = new HashMap<>();
        for (NodeDto n : nodes) {
            if (n.parentId() != null) {
                kids.computeIfAbsent(n.parentId(), k -> new ArrayList<>()).add(n);
            }
        }
        List<NodeDto> roots = nodes.stream().filter(n -> n.parentId() == null)
                .sorted(Comparator.comparing(NodeDto::id)).toList();
        if (roots.isEmpty()) {
            editor.getChildren().add(Ui.label("Este componente aún no tiene comportamiento definido.", "hint"));
        }
        for (NodeDto r : roots) {
            VBox tree = new VBox(6);
            tree.getStyleClass().add("card");
            tree.setPadding(new Insets(12, 14, 12, 14));
            renderNode(r, 0, kids, tree);
            editor.getChildren().add(tree);
        }
    }

    private void renderNode(NodeDto n, int depth, Map<Long, List<NodeDto>> kids, VBox out) {
        out.getChildren().add(nodeRow(n, depth));
        List<NodeDto> ch = kids.getOrDefault(n.id(), List.of());
        if ("CONDITION".equals(n.kind())) {
            for (String branch : List.of("YES", "NO")) {
                NodeDto c = ch.stream().filter(x -> branch.equals(x.branch())).findFirst().orElse(null);
                if (c != null) {
                    renderNode(c, depth + 1, kids, out);
                } else {
                    out.getChildren().add(slot(depth + 1, branch, n.id(), "YES".equals(branch) ? "SÍ" : "NO"));
                }
            }
        } else if ("ACTION".equals(n.kind())) {
            if (ch.isEmpty()) {
                out.getChildren().add(slot(depth + 1, null, n.id(), "Siguiente"));
            } else {
                renderNode(ch.get(0), depth + 1, kids, out);
            }
        }
    }

    private Node slot(int depth, String branch, Long parentId, String label) {
        Button add = Ui.button("+ Agregar", "btn-link", () -> openForm(parentId, branch, null));
        HBox row = new HBox(8, Ui.label(label + " →", "hint"), add);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 0, depth * 26));
        return row;
    }

    private Node nodeRow(NodeDto n, int depth) {
        HBox head = new HBox(8);
        head.setAlignment(Pos.CENTER_LEFT);
        if (n.branch() != null) {
            Label b = Ui.label("YES".equals(n.branch()) ? "SÍ" : "NO", "tag");
            head.getChildren().add(b);
        }
        head.getChildren().add(Ui.label(kindName(n), "tag"));
        Label text = Ui.label(n.description(), "row-title");
        text.setWrapText(true);
        text.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(text, Priority.ALWAYS);
        head.getChildren().add(text);
        if (!Objects.equals(n.elementId(), selected)) {
            head.getChildren().add(Ui.label("en: " + componentName(n.elementId()), "hint"));
        }
        if (n.locked()) {
            head.getChildren().add(Ui.label("En una cadena de riesgo", "hint"));
        }
        Button edit = Ui.button("Editar", "btn-link", () -> openForm(n.parentId(), n.branch(), n));
        Button remove = Ui.button("Quitar", "btn-link", () -> remove(n));
        edit.setDisable(n.locked());
        remove.setDisable(n.locked());
        head.getChildren().addAll(edit, remove);

        VBox box = new VBox(2, head);
        if ("OUTCOME_HAZARD".equals(n.kind())) {
            Label detail = Ui.label("Situación peligrosa: " + n.situation() + "\nDaño: " + n.harm(), "hint");
            detail.setWrapText(true);
            box.getChildren().add(detail);
        }
        box.setPadding(new Insets(4, 6, 4, 6 + depth * 26));
        box.getStyleClass().add("list-row-flat");
        if (Objects.equals(n.id(), highlight)) {
            box.getStyleClass().add("list-row-selected");
        }
        return box;
    }

    private static String kindName(NodeDto n) {
        return switch (n.kind()) {
            case "CONDITION" -> n.parentId() == null ? "Peligro · condición" : "Condición";
            case "OUTCOME_SAFE" -> "Seguro";
            case "OUTCOME_HAZARD" -> "Peligroso";
            default -> n.parentId() == null ? "Peligro · acción" : "Acción";
        };
    }

    private void remove(NodeDto n) {
        error.setText("");
        Async.run(() -> { api.deactivateBehaviorNode(n.id()); return Boolean.TRUE; },
                ok -> loadNodes(), msg -> error.setText(msg));
    }

    // ------------------------------------------------------------------ formulario

    private void openForm(Long parentId, String branch, NodeDto editing) {
        error.setText("");
        NodeDto parent = parentId == null ? null : nodes.stream().filter(x -> x.id().equals(parentId)).findFirst().orElse(null);

        ComboBox<Opt> kind = new ComboBox<>();
        kind.getItems().addAll(parentId == null ? ROOT_KINDS : ALL_KINDS);
        kind.setValue(kind.getItems().get(0));
        kind.setMaxWidth(Double.MAX_VALUE);

        TextArea description = new TextArea(editing == null ? "" : editing.description());
        description.setPrefRowCount(2);
        description.setWrapText(true);

        ComboBox<CompOpt> where = new ComboBox<>();
        components.forEach(c -> where.getItems().add(new CompOpt(c.id(), c.name())));
        Long defaultComp = editing != null ? editing.elementId() : parent != null ? parent.elementId() : selected;
        where.getItems().stream().filter(o -> o.id().equals(defaultComp)).findFirst().ifPresent(where::setValue);
        where.setMaxWidth(Double.MAX_VALUE);

        TextArea situation = new TextArea();
        situation.setPrefRowCount(2);
        situation.setWrapText(true);
        TextArea harm = new TextArea();
        harm.setPrefRowCount(2);
        harm.setWrapText(true);
        TextField harmType = new TextField();
        harmType.setPromptText("Tipo de daño (opcional)");
        VBox hazardFields = new VBox(8, field("Situación peligrosa", situation), field("Daño", harm), harmType);
        Ui.show(hazardFields, false);
        kind.setOnAction(e -> Ui.show(hazardFields, kind.getValue() != null && kind.getValue().code().equals("OUTCOME_HAZARD")));

        Label err = Ui.label("", "field-error");
        err.setWrapText(true);
        Button save = Ui.button(editing == null ? "Guardar nodo" : "Guardar cambios", "btn-primary", () -> { });
        Button cancel = Ui.button("Cancelar", "btn-link", () -> formHolder.getChildren().clear());
        save.setOnAction(e -> {
            if (description.getText().isBlank()) {
                err.setText("Escriba la descripción.");
                return;
            }
            String code = kind.getValue().code();
            if (editing == null && code.equals("OUTCOME_HAZARD") && (situation.getText().isBlank() || harm.getText().isBlank())) {
                err.setText("Un resultado peligroso necesita la situación peligrosa y el daño.");
                return;
            }
            Long comp = where.getValue() == null ? null : where.getValue().id();
            save.setDisable(true);
            Async.run(() -> {
                if (editing != null) {
                    return api.updateBehaviorNode(editing.id(), description.getText().trim());
                }
                return api.createBehaviorNode(new CreateNodeRequest(comp, parentId, code, description.getText().trim(),
                        branch, situation.getText().trim(), harm.getText().trim(),
                        harmType.getText().isBlank() ? null : harmType.getText().trim()));
            }, ok -> {
                formHolder.getChildren().clear();
                loadNodes();
            }, msg -> {
                err.setText(msg);
                save.setDisable(false);
            });
        });

        String where2 = parent == null ? "Nuevo peligro (raíz)"
                : "Nuevo nodo " + (branch == null ? "tras «" + parent.description() + "»"
                : "en la rama " + ("YES".equals(branch) ? "SÍ" : "NO") + " de «" + parent.description() + "»");
        VBox form = new VBox(10, Ui.label(editing == null ? where2 : "Editar descripción", "form-title-sm"));
        if (editing == null) {
            form.getChildren().add(field("Qué es", kind));
        }
        form.getChildren().add(field("Descripción", description));
        if (editing == null) {
            form.getChildren().addAll(field("Componente donde ocurre", where), hazardFields);
        }
        form.getChildren().addAll(err, new HBox(10, save, cancel));
        form.getStyleClass().add("card");
        form.setPadding(new Insets(14, 16, 14, 16));
        formHolder.getChildren().setAll(form);
    }

    private static Node field(String label, Node control) {
        return new VBox(4, Ui.label(label, "field-label"), control);
    }
}
