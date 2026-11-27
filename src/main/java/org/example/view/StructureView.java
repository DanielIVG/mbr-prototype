package org.example.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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
import org.example.api.ComponentLibraryDtos;
import org.example.api.DeviceDto;
import org.example.api.FunctionDto;
import org.example.api.InteractionDto;
import org.example.client.ApiClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Pantalla 4 · Estructura del dispositivo: componentes (árbol), funciones e interacciones.
 * Lista a la izquierda, detalle o formulario a la derecha (sin ventanas emergentes).
 */
public class StructureView extends HBox {

    private enum Tab { COMPONENTS, FUNCTIONS, INTERACTIONS }

    private enum Mode { DETAIL, CREATE, EDIT }

    private record Option(Long id, String text) {
        @Override
        public String toString() {
            return text;
        }
    }

    private static final Option NO_PARENT = new Option(null, "(Ninguno: componente de primer nivel)");

    private final ApiClient api;
    private List<ComponentLibraryDtos.EntryDto> library = List.of();
    private final DeviceDto device;
    private final Runnable onChanged;

    private List<ComponentDto> components = List.of();
    private List<FunctionDto> functions = List.of();
    private List<InteractionDto> interactions = List.of();

    private Tab tab = Tab.COMPONENTS;
    private Mode mode = Mode.DETAIL;
    private Long selectedId;
    private String notice;

    private final VBox listBox = new VBox(8);
    private final VBox aside = new VBox(12);
    private final HBox tabsBar = new HBox(6);
    private final Label loading = Ui.label("Cargando estructura…", "hint");

    public StructureView(ApiClient api, DeviceDto device, Runnable onChanged) {
        this.api = api;
        this.device = device;
        this.onChanged = onChanged;
        setSpacing(20);
        setPadding(new Insets(20, 28, 20, 28));

        VBox left = new VBox(12, tabsBar, scroll(listBox));
        VBox.setVgrow(left.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(left, Priority.ALWAYS);

        aside.getStyleClass().add("card");
        aside.setPadding(new Insets(16, 18, 16, 18));
        aside.setPrefWidth(360);
        aside.setMinWidth(360);
        aside.setMaxWidth(360);

        ScrollPane asideScroll = scroll(aside);
        asideScroll.setPrefWidth(380);
        asideScroll.setMinWidth(380);
        getChildren().addAll(left, asideScroll);
        reload();
    }

    private static ScrollPane scroll(Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("plain-scroll");
        return sp;
    }

    // ------------------------------------------------------------------ datos

    private void reload() {
        listBox.getChildren().setAll(loading);
        long id = device.id();
        Async.run(() -> new Object[] {api.listComponents(id), api.listFunctions(id), api.listInteractions(id)},
                data -> {
                    @SuppressWarnings("unchecked") List<ComponentDto> c = (List<ComponentDto>) data[0];
                    @SuppressWarnings("unchecked") List<FunctionDto> f = (List<FunctionDto>) data[1];
                    @SuppressWarnings("unchecked") List<InteractionDto> i = (List<InteractionDto>) data[2];
                    components = c;
                    functions = f;
                    interactions = i;
                    onChanged.run();
                    render();
                },
                msg -> {
                    listBox.getChildren().setAll(errorBox("No se pudo cargar la estructura: " + msg));
                });
    }

    private Node errorBox(String message) {
        Label l = Ui.label(message, "field-error");
        l.setWrapText(true);
        Button retry = Ui.button("Reintentar", "btn-secondary", this::reload);
        return new VBox(8, l, retry);
    }

    public int componentCount() {
        return components.size();
    }

    // ------------------------------------------------------------------ pintado

    private void render() {
        renderTabs();
        renderList();
        renderAside();
    }

    private void renderTabs() {
        tabsBar.getChildren().clear();
        tabsBar.setAlignment(Pos.CENTER_LEFT);
        tabsBar.getChildren().addAll(
                tabButton(Tab.COMPONENTS, "Componentes", components.size()),
                tabButton(Tab.FUNCTIONS, "Funciones", functions.size()),
                tabButton(Tab.INTERACTIONS, "Interacciones", interactions.size()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        String add = switch (tab) {
            case COMPONENTS -> "+ Agregar componente";
            case FUNCTIONS -> "+ Agregar función";
            case INTERACTIONS -> "+ Agregar interacción";
        };
        tabsBar.getChildren().addAll(spacer, Ui.button(add, "btn-primary", () -> {
            mode = Mode.CREATE;
            selectedId = null;
            notice = null;
            render();
        }));
    }

    private Button tabButton(Tab target, String text, int count) {
        Button b = Ui.button(text + " (" + count + ")",
                tab == target ? "btn-tab-active" : "btn-tab", () -> {
                    tab = target;
                    mode = Mode.DETAIL;
                    selectedId = null;
                    notice = null;
                    render();
                });
        return b;
    }

    private void renderList() {
        listBox.getChildren().clear();
        switch (tab) {
            case COMPONENTS -> renderComponentTree();
            case FUNCTIONS -> {
                for (FunctionDto f : functions) {
                    listBox.getChildren().add(row(f.id(), f.name(),
                            names(f.componentIds()), false));
                }
            }
            case INTERACTIONS -> {
                for (InteractionDto i : interactions) {
                    listBox.getChildren().add(row(i.id(), i.description(),
                            names(i.componentIds()), false));
                }
            }
        }
        if (listBox.getChildren().isEmpty()) {
            VBox empty = switch (tab) {
                case COMPONENTS -> Ui.emptyState(Ui.ICON_BLOCKS,
                        "Aún no hay componentes",
                        "Los componentes son las partes físicas del dispositivo. Agrega el primero con el botón de arriba.");
                case FUNCTIONS -> Ui.emptyState(Ui.ICON_LAYERS,
                        "Aún no hay funciones",
                        "Una función es lo que el dispositivo hace gracias a sus componentes. Define las funciones principales del dispositivo.");
                case INTERACTIONS -> Ui.emptyState(Ui.ICON_LINK,
                        "Aún no hay interacciones",
                        "Registra solo las interacciones relevantes para el riesgo, involucrando mínimo dos componentes.");
            };
            listBox.getChildren().add(empty);
        }
    }

    private void renderComponentTree() {
        Map<Long, List<ComponentDto>> byParent = new LinkedHashMap<>();
        Set<Long> ids = new HashSet<>();
        components.forEach(c -> ids.add(c.id()));
        for (ComponentDto c : components) {
            // Un hijo cuyo padre fue desactivado se muestra como de primer nivel.
            Long parent = c.parentComponentId() != null && ids.contains(c.parentComponentId())
                    ? c.parentComponentId() : null;
            byParent.computeIfAbsent(parent, k -> new ArrayList<>()).add(c);
        }
        addBranch(byParent, null, 0);
    }

    private void addBranch(Map<Long, List<ComponentDto>> byParent, Long parent, int depth) {
        List<ComponentDto> children = byParent.getOrDefault(parent, List.of()).stream()
                .sorted(Comparator.comparing(ComponentDto::name, String.CASE_INSENSITIVE_ORDER)).toList();
        for (ComponentDto c : children) {
            HBox r = row(c.id(), c.name(), c.essential() ? "Esencial" : "", c.essential());
            r.setPadding(new Insets(0, 0, 0, depth * 22));
            listBox.getChildren().add(r);
            addBranch(byParent, c.id(), depth + 1);
        }
    }

    /** Fila de una lista: título acotado en una línea (el texto completo va en el detalle). */
    private HBox row(Long id, String title, String subtitle, boolean tag) {
        Label t = Ui.label(Objects.toString(title, ""), "row-title");
        t.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(t, Priority.ALWAYS);
        HBox box = new HBox(10, t);
        if (!subtitle.isBlank()) {
            Label s = Ui.label(subtitle, tag ? "tag" : "hint");
            s.setMaxWidth(260);
            box.getChildren().add(s);
        }
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("list-row");
        if (Objects.equals(id, selectedId)) {
            box.getStyleClass().add("list-row-selected");
        }
        box.setOnMouseClicked(e -> {
            selectedId = id;
            mode = Mode.DETAIL;
            notice = null;
            render();
        });
        return box;
    }

    private String names(Set<Long> ids) {
        List<String> out = new ArrayList<>();
        for (ComponentDto c : components) {
            if (ids.contains(c.id())) {
                out.add(c.name());
            }
        }
        return String.join(" · ", out);
    }

    // ------------------------------------------------------------------ panel derecho

    private void renderAside() {
        aside.getChildren().clear();
        if (mode == Mode.CREATE || mode == Mode.EDIT) {
            switch (tab) {
                case COMPONENTS -> componentForm();
                case FUNCTIONS -> functionForm();
                case INTERACTIONS -> interactionForm();
            }
            return;
        }
        if (selectedId == null) {
            Label hint = Ui.label("Selecciona un elemento de la lista para ver su detalle.", "hint");
            hint.setWrapText(true);
            aside.getChildren().add(hint);
            return;
        }
        switch (tab) {
            case COMPONENTS -> components.stream().filter(c -> c.id().equals(selectedId)).findFirst()
                    .ifPresent(this::componentDetail);
            case FUNCTIONS -> functions.stream().filter(f -> f.id().equals(selectedId)).findFirst()
                    .ifPresent(this::functionDetail);
            case INTERACTIONS -> interactions.stream().filter(i -> i.id().equals(selectedId)).findFirst()
                    .ifPresent(this::interactionDetail);
        }
    }

    private static Node field(String name, String value) {
        Label v = Ui.label(value == null || value.isBlank() ? "—" : value, "muted");
        v.setWrapText(true);
        return new VBox(3, Ui.label(name, "table-head"), v);
    }

    private void componentDetail(ComponentDto c) {
        String parent = components.stream().filter(p -> p.id().equals(c.parentComponentId()))
                .map(ComponentDto::name).findFirst().orElse(null);
        aside.getChildren().addAll(Ui.label("Componente seleccionado", "table-head"),
                Ui.label(c.name(), "form-title-sm"),
                field("Dentro de", parent), field("Descripción", c.description()),
                field("¿Es esencial?", c.essential() ? "Sí. " + Objects.toString(c.essentialJustification(), "") : "No"),
                actions(() -> { mode = Mode.EDIT; notice = null; render(); },
                        () -> deactivate("el componente «" + c.name() + "»",
                                () -> api.deactivateComponent(c.id()))));
    }

    private void functionDetail(FunctionDto f) {
        aside.getChildren().addAll(Ui.label("Función seleccionada", "table-head"),
                Ui.label(f.name(), "form-title-sm"),
                field("Descripción", f.description()), field("Componentes que la cumplen", names(f.componentIds())),
                actions(() -> { mode = Mode.EDIT; notice = null; render(); },
                        () -> deactivate("la función «" + f.name() + "»", () -> api.deactivateFunction(f.id()))));
    }

    private void interactionDetail(InteractionDto i) {
        aside.getChildren().addAll(Ui.label("Interacción seleccionada", "table-head"),
                field("Descripción", i.description()),
                field("Por qué es relevante", i.relevanceJustification()),
                field("Componentes que intervienen", names(i.componentIds())),
                actions(() -> { mode = Mode.EDIT; notice = null; render(); },
                        () -> deactivate("la interacción", () -> api.deactivateInteraction(i.id()))));
    }

    private Node actions(Runnable edit, Runnable deactivate) {
        HBox box = new HBox(8, Ui.button("Editar", "btn-secondary", edit),
                Ui.button("Desactivar", "btn-link", deactivate));
        box.setAlignment(Pos.CENTER_LEFT);
        VBox wrap = new VBox(8, box);
        if (notice != null) {
            Label n = Ui.label(notice, "field-error");
            n.setWrapText(true);
            wrap.getChildren().add(n);
        }
        return wrap;
    }

    private void deactivate(String what, Runnable call) {
        if (!Ui.confirm("¿Desactivar " + what + "? Seguirá registrado para conservar el historial de riesgo, "
                + "pero dejará de aparecer en la estructura activa.", "Desactivar")) {
            return;
        }
        Async.run(() -> { call.run(); return Boolean.TRUE; },
                ok -> { selectedId = null; reload(); },
                msg -> { notice = msg; renderAside(); });
    }

    // ------------------------------------------------------------------ formularios

    private static TextField text(String value) {
        TextField t = new TextField(value == null ? "" : value);
        t.setMaxWidth(Double.MAX_VALUE);
        return t;
    }

    private static TextArea area(String value) {
        TextArea a = new TextArea(value == null ? "" : value);
        a.setPrefRowCount(3);
        a.setWrapText(true);
        return a;
    }

    private static Node labeled(String label, Node input) {
        return new VBox(4, Ui.label(label, "field-label"), input);
    }

    /** Casillas de componentes activos; devuelve el contenedor y deja el mapa para leer la selección. */
    private VBox componentChecks(Set<Long> selected, Map<Long, CheckBox> out) {
        VBox box = new VBox(4);
        for (ComponentDto c : components) {
            CheckBox cb = new CheckBox(c.name());
            cb.setSelected(selected.contains(c.id()));
            out.put(c.id(), cb);
            box.getChildren().add(cb);
        }
        if (components.isEmpty()) {
            box.getChildren().add(Ui.label("Primero agrega componentes.", "hint"));
        }
        return box;
    }

    private static Set<Long> checked(Map<Long, CheckBox> checks) {
        Set<Long> ids = new TreeSet<>();
        checks.forEach((id, cb) -> {
            if (cb.isSelected()) {
                ids.add(id);
            }
        });
        return ids;
    }

    private Node formFooter(Label error, Button save) {
        Button cancel = Ui.button("Cancelar", "btn-secondary", () -> { mode = Mode.DETAIL; notice = null; render(); });
        HBox row = new HBox(8, save, cancel);
        row.setAlignment(Pos.CENTER_LEFT);
        error.setWrapText(true);
        return new VBox(8, error, row);
    }

    private void submit(Button save, Label error, java.util.function.Supplier<Object> call) {
        save.setDisable(true);
        error.setText("");
        Async.run(call::get, ok -> { mode = Mode.DETAIL; reload(); },
                msg -> { error.setText(msg); save.setDisable(false); });
    }

    private void componentForm() {
        ComponentDto editing = mode == Mode.EDIT
                ? components.stream().filter(c -> c.id().equals(selectedId)).findFirst().orElse(null) : null;
        TextField name = text(editing == null ? "" : editing.name());
        String[] chosen = {editing == null ? null : editing.name()};     // término de la biblioteca elegido
        VBox suggestions = new VBox(4);
        Label libraryHint = Ui.label("Escriba para buscar y elija un componente de la biblioteca. "
                + "No se admiten nombres que no estén en ella.", "hint");
        libraryHint.setWrapText(true);
        Runnable[] refresh = {() -> { }};
        name.textProperty().addListener((o, was, now) -> {
            if (chosen[0] != null && !chosen[0].equals(now)) {
                chosen[0] = null;
            }
            refresh[0].run();
        });
        if (library.isEmpty()) {
            Async.run(api::listComponentLibrary, l -> { library = l; refresh[0].run(); }, msg -> { });
        }
        TextArea desc = area(editing == null ? "" : editing.description());
        CheckBox essential = new CheckBox("Es esencial para la función o la seguridad del dispositivo");
        essential.setSelected(editing != null && editing.essential());
        TextArea just = area(editing == null ? "" : editing.essentialJustification());
        Node justBox = labeled("Justificación (obligatoria si es esencial)", just);
        Ui.show(justBox, essential.isSelected());
        essential.setOnAction(e -> Ui.show(justBox, essential.isSelected()));

        Set<Long> excluded = editing == null ? Set.of() : descendantsAndSelf(editing.id());
        ComboBox<Option> parent = new ComboBox<>();
        parent.getItems().add(NO_PARENT);
        for (ComponentDto c : components) {
            if (!excluded.contains(c.id())) {
                parent.getItems().add(new Option(c.id(), c.name()));
            }
        }
        parent.setValue(parent.getItems().stream()
                .filter(o -> editing != null && Objects.equals(o.id(), editing.parentComponentId()))
                .findFirst().orElse(NO_PARENT));
        parent.setMaxWidth(Double.MAX_VALUE);
        refresh[0] = () -> refreshSuggestions(name, chosen, suggestions, parent);
        parent.setOnAction(e -> refresh[0].run());
        refresh[0].run();

        Label error = Ui.label("", "field-error");
        Button save = Ui.button(editing == null ? "Guardar componente" : "Guardar cambios", "btn-primary", () -> { });
        save.setOnAction(e -> {
            if (name.getText().isBlank()) {
                error.setText("El nombre es obligatorio.");
                return;
            }
            if (chosen[0] == null) {
                error.setText("Elija un componente de la lista de la biblioteca.");
                return;
            }
            if (essential.isSelected() && just.getText().isBlank()) {
                error.setText("Un componente esencial requiere justificación.");
                return;
            }
            Long parentId = parent.getValue() == null ? null : parent.getValue().id();
            ComponentDto body = new ComponentDto(editing == null ? null : editing.id(), device.id(), parentId,
                    chosen[0], desc.getText(), essential.isSelected(),
                    essential.isSelected() ? just.getText().trim() : null, true);
            submit(save, error, () -> editing == null ? api.createComponent(body)
                    : api.updateComponent(editing.id(), body));
        });
        aside.getChildren().addAll(Ui.label(editing == null ? "Nuevo componente" : "Editar componente", "form-title-sm"),
                labeled("Nombre (de la biblioteca)", name), suggestions, libraryHint, labeled("Dentro de", parent), labeled("Descripción", desc),
                essential, justBox, formFooter(error, save));
    }

    /** Término de la biblioteca del componente elegido como contenedor (por su nombre oficial), si lo hay. */
    private String libraryCodeOfParent(ComboBox<Option> parent) {
        Option o = parent.getValue();
        if (o == null || o.id() == null) {
            return null;
        }
        String parentName = components.stream().filter(c -> c.id().equals(o.id()))
                .map(ComponentDto::name).findFirst().orElse(null);
        return library.stream().filter(e -> e.name().equals(parentName))
                .map(ComponentLibraryDtos.EntryDto::code).findFirst().orElse(null);
    }

    private void refreshSuggestions(TextField name, String[] chosen, VBox suggestions, ComboBox<Option> parent) {
        String now = name.getText();
        String parentCode = libraryCodeOfParent(parent);
        suggestions.getChildren().clear();
        if (now != null && now.equals(chosen[0])) {
            return;
        }
        if (now == null || now.isBlank()) {
            if (parentCode != null) {
                var kids = library.stream().filter(e -> parentCode.equals(e.parentCode())).toList();
                if (!kids.isEmpty()) {
                    suggestions.getChildren().add(Ui.label("Tipos de este componente en la biblioteca:", "hint"));
                    kids.forEach(k -> suggestions.getChildren().add(suggestion(k, name, chosen, suggestions)));
                }
            }
            return;
        }
        Async.run(() -> api.searchComponentLibrary(now, 8), found -> {
            if (!now.equals(name.getText())) {
                return;                                                  // llegó tarde: el texto ya cambió
            }
            suggestions.getChildren().clear();
            var ordered = new ArrayList<>(found);
            ordered.sort(java.util.Comparator.comparingInt(
                    m -> parentCode != null && parentCode.equals(m.entry().parentCode()) ? 0 : 1));   // estable
            for (var m : ordered) {
                suggestions.getChildren().add(suggestion(m.entry(), name, chosen, suggestions));
            }
            if (found.isEmpty()) {
                suggestions.getChildren().add(Ui.label("Ningún componente de la biblioteca se parece.", "hint"));
            }
        }, msg -> { });
    }

    private Button suggestion(ComponentLibraryDtos.EntryDto entry, TextField name, String[] chosen, VBox suggestions) {
        Button b = Ui.button(entry.name(), "btn-secondary", () -> {
            chosen[0] = entry.name();
            name.setText(entry.name());
            suggestions.getChildren().clear();
        });
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private Set<Long> descendantsAndSelf(Long id) {
        Set<Long> out = new HashSet<>();
        out.add(id);
        boolean grew = true;
        while (grew) {
            grew = false;
            for (ComponentDto c : components) {
                if (c.parentComponentId() != null && out.contains(c.parentComponentId()) && out.add(c.id())) {
                    grew = true;
                }
            }
        }
        return out;
    }

    private void functionForm() {
        FunctionDto editing = mode == Mode.EDIT
                ? functions.stream().filter(f -> f.id().equals(selectedId)).findFirst().orElse(null) : null;
        TextField name = text(editing == null ? "" : editing.name());
        TextArea desc = area(editing == null ? "" : editing.description());
        Map<Long, CheckBox> checks = new LinkedHashMap<>();
        VBox checkBox = componentChecks(editing == null ? Set.of() : editing.componentIds(), checks);
        Label error = Ui.label("", "field-error");
        Button save = Ui.button(editing == null ? "Guardar función" : "Guardar cambios", "btn-primary", () -> { });
        save.setOnAction(e -> {
            if (name.getText().isBlank()) {
                error.setText("El nombre es obligatorio.");
                return;
            }
            FunctionDto body = new FunctionDto(editing == null ? null : editing.id(), device.id(),
                    name.getText().trim(), desc.getText(), checked(checks), true);
            submit(save, error, () -> editing == null ? api.createFunction(body)
                    : api.updateFunction(editing.id(), body));
        });
        aside.getChildren().addAll(Ui.label(editing == null ? "Nueva función" : "Editar función", "form-title-sm"),
                labeled("Nombre", name), labeled("Descripción", desc),
                labeled("Componentes que la cumplen", checkBox), formFooter(error, save));
    }

    private void interactionForm() {
        InteractionDto editing = mode == Mode.EDIT
                ? interactions.stream().filter(i -> i.id().equals(selectedId)).findFirst().orElse(null) : null;
        TextArea desc = area(editing == null ? "" : editing.description());
        TextArea relevance = area(editing == null ? "" : editing.relevanceJustification());
        Map<Long, CheckBox> checks = new LinkedHashMap<>();
        VBox checkBox = componentChecks(editing == null ? Set.of() : editing.componentIds(), checks);
        Label error = Ui.label("", "field-error");
        Button save = Ui.button(editing == null ? "Guardar interacción" : "Guardar cambios", "btn-primary", () -> { });
        save.setOnAction(e -> {
            if (desc.getText().isBlank() || relevance.getText().isBlank()) {
                error.setText("La descripción y la justificación de relevancia son obligatorias.");
                return;
            }
            if (checked(checks).size() < 2) {
                error.setText("Una interacción involucra al menos dos componentes.");
                return;
            }
            InteractionDto body = new InteractionDto(editing == null ? null : editing.id(), device.id(),
                    desc.getText().trim(), relevance.getText().trim(), checked(checks), true);
            submit(save, error, () -> editing == null ? api.createInteraction(body)
                    : api.updateInteraction(editing.id(), body));
        });
        aside.getChildren().addAll(Ui.label(editing == null ? "Nueva interacción" : "Editar interacción", "form-title-sm"),
                labeled("Descripción", desc), labeled("Por qué es relevante para el riesgo", relevance),
                labeled("Componentes que intervienen (mínimo 2)", checkBox), formFooter(error, save));
    }
}
