package org.example.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.api.AssessmentDtos.EvidenceLevelDto;
import org.example.api.ComponentDto;
import org.example.api.DeviceDto;
import org.example.api.FtaDtos.BasicDto;
import org.example.api.FtaDtos.FtaDto;
import org.example.api.FtaDtos.NodeDto;
import org.example.api.FtaDtos.ResultDto;
import org.example.api.RiskChainDto;
import org.example.client.ApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Análisis de árbol de fallas (FTA) de un daño: el evento superior es la situación peligrosa y su
 * probabilidad alimenta P1 en la estimación (modo «con el árbol de fallas»).
 */
public class FtaView extends HBox {

    private static final String AND = "AND";
    private static final String OR = "OR";

    private final ApiClient api;
    private final DeviceDto device;

    private List<ComponentDto> components = List.of();
    private List<RiskChainDto> chains = List.of();
    private List<EvidenceLevelDto> evidence = List.of();
    private String exposureUnit = "por hora";
    private Long selected;

    private final VBox listBox = new VBox(8);
    private final VBox right = new VBox(12);

    // estado del editor
    private final TextField topEvent = new TextField();
    private final TextField author = new TextField(System.getProperty("user.name", ""));
    private final List<BasicEditor> basics = new ArrayList<>();
    private final VBox basicsBox = new VBox(10);
    private GateEditor root;
    private final VBox treeBox = new VBox(8);
    private final VBox resultBox = new VBox(8);
    private final Label error = Ui.label("", "field-error");

    public FtaView(ApiClient api, DeviceDto device) {
        this.api = api;
        this.device = device;
        setSpacing(20);
        setPadding(new Insets(20, 28, 20, 28));
        load();
    }

    // ------------------------------------------------------------------ carga

    private void load() {
        getChildren().setAll(Ui.label("Cargando…", "hint"));
        long id = device.id();
        Async.run(() -> {
            List<ComponentDto> comps = api.listComponents(id);
            Set<Long> ids = comps.stream().map(ComponentDto::id).collect(Collectors.toSet());
            List<RiskChainDto> mine = api.listChains().stream()
                    .filter(x -> x.hazard().modelElementId() != null && ids.contains(x.hazard().modelElementId()))
                    .toList();
            var criteria = api.getCriteria();
            return new Object[] {comps, mine, api.listEvidenceLevels(), criteria == null ? null : criteria.exposureUnit()};
        }, data -> {
            @SuppressWarnings("unchecked") List<ComponentDto> c = (List<ComponentDto>) data[0];
            @SuppressWarnings("unchecked") List<RiskChainDto> ch = (List<RiskChainDto>) data[1];
            @SuppressWarnings("unchecked") List<EvidenceLevelDto> ev = (List<EvidenceLevelDto>) data[2];
            components = c;
            chains = ch;
            evidence = ev;
            if (data[3] != null) {
                exposureUnit = (String) data[3];
            }
            build();
        }, msg -> {
            Label l = Ui.label("No se pudo cargar: " + msg, "field-error");
            l.setWrapText(true);
            getChildren().setAll(new VBox(8, l, Ui.button("Reintentar", "btn-secondary", this::load)));
        });
    }

    private void build() {
        listBox.getChildren().clear();
        for (RiskChainDto c : chains) {
            Label title = Ui.label(Objects.toString(c.harm().description(), ""), "row-title");
            HBox row = new HBox(title);
            row.getStyleClass().add("list-row");
            if (Objects.equals(c.harm().id(), selected)) {
                row.getStyleClass().add("list-row-selected");
            }
            row.setOnMouseClicked(e -> {
                selected = c.harm().id();
                build();
                openEditor(c);
            });
            listBox.getChildren().add(row);
        }
        if (chains.isEmpty()) {
            listBox.getChildren().add(Ui.label("No hay cadenas de riesgo. Créalas en la sección Riesgos.", "hint"));
        }
        ScrollPane left = new ScrollPane(listBox);
        left.setFitToWidth(true);
        left.getStyleClass().add("plain-scroll");
        left.setPrefWidth(300);
        left.setMinWidth(300);
        left.setMaxWidth(300);
        ScrollPane main = new ScrollPane(right);
        main.setFitToWidth(true);
        main.getStyleClass().add("plain-scroll");
        HBox.setHgrow(main, Priority.ALWAYS);
        getChildren().setAll(left, main);
        if (selected == null) {
            Label hint = Ui.label("Elige un daño para construir su árbol de fallas.", "hint");
            hint.setWrapText(true);
            right.getChildren().setAll(hint);
        }
    }

    // ------------------------------------------------------------------ editor

    private void openEditor(RiskChainDto chain) {
        right.getChildren().setAll(Ui.label("Cargando árbol…", "hint"));
        long harmId = chain.harm().id();
        Async.run(() -> api.getFta(harmId), saved -> {
            basics.clear();
            basicsBox.getChildren().clear();
            treeBox.getChildren().clear();
            resultBox.getChildren().clear();
            error.setText("");
            root = new GateEditor(null);
            topEvent.setText(saved == null ? Objects.toString(chain.situation().description(), "") : saved.topEvent());
            if (saved != null) {
                for (BasicDto b : saved.basics()) {
                    addBasic().fill(b);
                }
                root.fill(saved.root());
                showResult(saved.result());
            } else {
                addBasic();
                addBasic();
            }
            treeBox.getChildren().add(root.node());
            renderEditor(chain);
        }, msg -> right.getChildren().setAll(Ui.label(msg, "field-error")));
    }

    private void renderEditor(RiskChainDto chain) {
        Label t = Ui.label("Árbol de fallas · " + chain.harm().description(), "form-title-sm");
        t.setWrapText(true);
        Label help = Ui.label("El evento superior es la situación peligrosa. Las compuertas O significan «basta una "
                + "causa»; las Y, «deben ocurrir todas». Una compuerta Y supone que sus entradas son independientes: "
                + "debes justificarlo. Un evento que aparece en varias ramas se cuenta una sola vez. Al menos un "
                + "evento debe tener unidad de exposición (por hora, por ciclo…) para obtener P1; en una compuerta Y "
                + "solo uno puede tenerla, los demás se expresan como probabilidad adimensional.", "hint");
        help.setWrapText(true);
        Button addBasic = Ui.button("+ Evento básico", "btn-secondary", () -> addBasic());
        Button save = Ui.button("Guardar y calcular", "btn-primary", () -> { });
        save.setOnAction(e -> save(chain.harm().id(), save));
        right.getChildren().setAll(t, help,
                card(Ui.label("Evento superior", "field-label"), topEvent),
                card(Ui.label("Eventos básicos (con su probabilidad y procedencia)", "field-label"), basicsBox, addBasic),
                card(Ui.label("Estructura del árbol", "field-label"), treeBox),
                card(Ui.label("Autor de los valores", "field-label"), author),
                error, save, resultBox);
    }

    private static Node card(Node... children) {
        VBox box = new VBox(10, children);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14, 16, 14, 16));
        return box;
    }

    private BasicEditor addBasic() {
        BasicEditor b = new BasicEditor();
        basics.add(b);
        basicsBox.getChildren().add(b.node());
        return b;
    }

    private List<String> basicNames() {
        return basics.stream().map(b -> b.name.getText().trim()).filter(n -> !n.isEmpty()).toList();
    }

    private void save(long harmId, Button save) {
        error.setText("");
        FtaDto body;
        try {
            if (author.getText().isBlank()) {
                throw new IllegalArgumentException("Indica quién registra los valores (autor).");
            }
            List<BasicDto> bs = new ArrayList<>();
            for (BasicEditor b : basics) {
                bs.add(b.toDto(author.getText().trim()));
            }
            body = new FtaDto(null, topEvent.getText().trim(), bs, root.toDto(), null);
        } catch (IllegalArgumentException ex) {
            error.setText(ex.getMessage());
            return;
        }
        save.setDisable(true);
        Async.run(() -> api.saveFta(harmId, body), saved -> {
            save.setDisable(false);
            showResult(saved.result());
        }, msg -> {
            error.setText(msg);
            save.setDisable(false);
        });
    }

    private static String num(double v) {
        return v == 0 ? "0" : String.format(Locale.ROOT, "%.3g", v);
    }

    private void showResult(ResultDto r) {
        VBox cuts = new VBox(4);
        r.cutSets().stream().limit(10).forEach(c -> cuts.getChildren().add(wrapped(
                String.join(" y ", c.events()) + "  →  " + num(c.probability()), "hint")));
        if (r.cutSets().size() > 10) {
            cuts.getChildren().add(Ui.label("… y " + (r.cutSets().size() - 10) + " más", "hint"));
        }
        Label single = wrapped(r.singlePoints().isEmpty()
                ? "No hay puntos únicos de falla: ninguna causa provoca sola el evento superior."
                : "Puntos únicos de falla (una sola causa basta): " + String.join(", ", r.singlePoints()), "field-label");
        resultBox.getChildren().setAll(card(
                Ui.label("Resultado", "form-title-sm"),
                wrapped("P1 = " + num(r.probability()) + " " + r.unit() + "   (rango " + num(r.min()) + " – "
                        + num(r.max()) + ")", "field-label"),
                wrapped(r.exact() ? "Cálculo exacto sobre los conjuntos de corte mínimos."
                        : "Demasiados conjuntos de corte: se muestra una cota superior, no el valor exacto.", "hint"),
                single, Ui.label("Conjuntos de corte mínimos (combinaciones que bastan)", "field-label"), cuts,
                wrapped("Para usar este valor: ve a Evaluación, elige este daño y estima «Con el árbol de fallas».", "hint")));
    }

    private static Label wrapped(String text, String... styles) {
        Label l = Ui.label(text, styles);
        l.setWrapText(true);
        return l;
    }

    // ------------------------------------------------------------------ evento básico

    private final class BasicEditor {
        final TextField name = new TextField();
        final QuantityForm quantity = new QuantityForm("P", "Probabilidad del evento", exposureUnit, true, evidence);
        final ComboBox<String> component = new ComboBox<>();
        final VBox box;

        BasicEditor() {
            name.setPromptText("Nombre del evento (único)");
            component.getItems().add("(sin componente)");
            components.forEach(c -> component.getItems().add(c.name()));
            component.setValue("(sin componente)");
            component.setMaxWidth(Double.MAX_VALUE);
            Button remove = Ui.button("Quitar", "btn-link", () -> {
                basics.remove(this);
                basicsBox.getChildren().remove(this.node());
            });
            HBox head = new HBox(8, name, component, remove);
            head.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(name, Priority.ALWAYS);
            box = new VBox(6, head, quantity.node());
            box.getStyleClass().add("list-row-flat");
        }

        Node node() {
            return box;
        }

        void fill(BasicDto b) {
            name.setText(b.name());
            quantity.fill(b.quantity());
            components.stream().filter(c -> c.id().equals(b.modelElementId())).findFirst()
                    .ifPresent(c -> component.setValue(c.name()));
        }

        BasicDto toDto(String authorName) {
            if (name.getText().isBlank()) {
                throw new IllegalArgumentException("Todo evento básico necesita un nombre.");
            }
            Long compId = components.stream().filter(c -> c.name().equals(component.getValue()))
                    .map(ComponentDto::id).findFirst().orElse(null);
            return new BasicDto(name.getText().trim(), quantity.toDto(authorName), compId);
        }
    }

    // ------------------------------------------------------------------ compuerta (recursiva)

    private final class GateEditor {
        final ComboBox<String> type = new ComboBox<>();
        final TextField label = new TextField();
        final TextField independence = new TextField();
        final VBox childrenBox = new VBox(6);
        final List<Object[]> kids = new ArrayList<>();           // {Node fila, GateEditor o ComboBox<String>}
        final VBox box;

        GateEditor(GateEditor parent) {
            type.getItems().addAll("O (basta una causa)", "Y (todas deben ocurrir)");
            type.setValue(type.getItems().get(0));
            label.setPromptText("Descripción de la compuerta");
            independence.setPromptText("Por qué las entradas son independientes (obligatorio en Y)");
            Ui.show(independence, false);
            type.setOnAction(e -> Ui.show(independence, isAnd()));
            Button addEvent = Ui.button("+ Evento", "btn-secondary", () -> addRef(null));
            Button addGate = Ui.button("+ Subcompuerta", "btn-secondary", () -> addGate().type.setValue(type.getItems().get(0)));
            HBox head = new HBox(8, type, label);
            head.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(label, Priority.ALWAYS);
            HBox actions = new HBox(8, addEvent, addGate);
            box = new VBox(6, head, independence, childrenBox, actions);
            box.setPadding(new Insets(8, 0, 8, 12));
            box.getStyleClass().add("gate-box");
            if (parent == null) {
                label.setText("Evento superior");
            }
        }

        boolean isAnd() {
            return type.getValue() != null && type.getValue().startsWith("Y");
        }

        Node node() {
            return box;
        }

        void addRef(String selectedName) {
            ComboBox<String> pick = new ComboBox<>();
            pick.setPromptText("Elige un evento básico");
            pick.setOnShowing(e -> {
                String current = pick.getValue();
                pick.getItems().setAll(basicNames());
                pick.setValue(current);
            });
            pick.getItems().setAll(basicNames());
            if (selectedName != null) {
                pick.setValue(selectedName);
            }
            pick.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(pick, Priority.ALWAYS);
            HBox row = new HBox(8, Ui.label("Evento:", "hint"), pick);
            row.setAlignment(Pos.CENTER_LEFT);
            Object[] entry = {row, pick};
            row.getChildren().add(Ui.button("Quitar", "btn-link", () -> {
                kids.remove(entry);
                childrenBox.getChildren().remove(row);
            }));
            kids.add(entry);
            childrenBox.getChildren().add(row);
        }

        GateEditor addGate() {
            GateEditor g = new GateEditor(this);
            g.label.setText("");
            HBox wrap = new HBox(8, g.node());
            HBox.setHgrow(g.node(), Priority.ALWAYS);
            Object[] entry = {wrap, g};
            wrap.getChildren().add(Ui.button("Quitar", "btn-link", () -> {
                kids.remove(entry);
                childrenBox.getChildren().remove(wrap);
            }));
            kids.add(entry);
            childrenBox.getChildren().add(wrap);
            return g;
        }

        void fill(NodeDto n) {
            type.setValue(OR.equals(n.gate()) ? type.getItems().get(0) : type.getItems().get(1));
            label.setText(Objects.toString(n.label(), ""));
            independence.setText(Objects.toString(n.independence(), ""));
            Ui.show(independence, isAnd());
            for (NodeDto c : n.children()) {
                if ("BASIC".equals(c.kind())) {
                    addRef(c.ref());
                } else {
                    addGate().fill(c);
                }
            }
        }

        @SuppressWarnings("unchecked")
        NodeDto toDto() {
            List<NodeDto> children = new ArrayList<>();
            for (Object[] k : kids) {
                if (k[1] instanceof GateEditor g) {
                    children.add(g.toDto());
                } else {
                    String ref = ((ComboBox<String>) k[1]).getValue();
                    if (ref == null || ref.isBlank()) {
                        throw new IllegalArgumentException("Hay una entrada de compuerta sin evento elegido.");
                    }
                    children.add(new NodeDto("BASIC", null, null, null, null, ref));
                }
            }
            return new NodeDto("GATE", label.getText().trim(), isAnd() ? AND : OR,
                    isAnd() ? independence.getText().trim() : null, children, null);
        }
    }
}
