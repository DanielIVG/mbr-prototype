package org.example.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.api.ComponentDto;
import org.example.api.DeviceDto;
import org.example.api.RiskChainDto;
import org.example.client.ApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Lista de cadenas de riesgo del dispositivo y acceso al formulario de nueva cadena. */
public class RisksView extends VBox {

    private final ApiClient api;
    private final DeviceDto device;
    private final Runnable onChanged;
    private final VBox listBox = new VBox(8);
    private final VBox aside = new VBox(12);

    private List<ComponentDto> components = List.of();
    private List<RiskChainDto> chains = List.of();
    private Long selectedHarmId;
    private final HBox content = new HBox(20);

    public RisksView(ApiClient api, DeviceDto device, Runnable onChanged) {
        this.api = api;
        this.device = device;
        this.onChanged = onChanged;
        setPadding(new Insets(20, 28, 20, 28));
        setSpacing(12);
        VBox.setVgrow(content, Priority.ALWAYS);
        getChildren().add(content);
        load();
    }

    private void load() {
        content.getChildren().setAll(Ui.label("Cargando riesgos…", "hint"));
        long id = device.id();
        Async.run(() -> {
            List<ComponentDto> comps = api.listComponents(id);
            Set<Long> ids = comps.stream().map(ComponentDto::id).collect(Collectors.toSet());
            List<RiskChainDto> mine = api.listChains().stream()
                    .filter(c -> c.hazard().modelElementId() != null && ids.contains(c.hazard().modelElementId()))
                    .toList();
            return new Object[] {comps, mine};
        }, data -> {
            @SuppressWarnings("unchecked") List<ComponentDto> c = (List<ComponentDto>) data[0];
            @SuppressWarnings("unchecked") List<RiskChainDto> r = (List<RiskChainDto>) data[1];
            components = c;
            chains = r;
            onChanged.run();
            showList();
        }, msg -> {
            Label l = Ui.label("No se pudieron cargar los riesgos: " + msg, "field-error");
            l.setWrapText(true);
            content.getChildren().setAll(new VBox(8, l, Ui.button("Reintentar", "btn-secondary", this::load)));
        });
    }

    private void showList() {
        Button add = Ui.button("+ Nueva cadena de riesgo", "btn-primary", this::showNew);
        if (components.isEmpty()) {
            add.setDisable(true);
        }
        Label count = Ui.label(chains.size() + (chains.size() == 1 ? " cadena" : " cadenas"), "table-head");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox head = new HBox(10, count, spacer, add);
        head.setAlignment(Pos.CENTER_LEFT);

        listBox.getChildren().clear();
        for (RiskChainDto c : chains) {
            listBox.getChildren().add(row(c));
        }
        if (chains.isEmpty()) {
            VBox empty = Ui.emptyState(
                    Ui.ICON_ALERT_TRIANGLE,
                    components.isEmpty() ? "Primero define componentes" : "Aún no hay cadenas de riesgo",
                    components.isEmpty()
                            ? "Ve a la sección Estructura para definir los componentes del dispositivo antes de crear cadenas de riesgo."
                            : "Cada cadena de riesgo sigue el camino: peligro → situación peligrosa → daño. Usa el botón «Nueva cadena» para comenzar.");
            listBox.getChildren().add(empty);
        }
        ScrollPane sp = new ScrollPane(listBox);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("plain-scroll");
        VBox left = new VBox(12, head, sp);
        VBox.setVgrow(sp, Priority.ALWAYS);
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

    private HBox row(RiskChainDto c) {
        Label title = Ui.label(Objects.toString(c.harm().description(), ""), "row-title");
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS);
        Label sub = Ui.label(Objects.toString(c.hazard().description(), ""), "hint");
        sub.setMaxWidth(220);
        HBox box = new HBox(10, title, sub);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("list-row");
        if (Objects.equals(c.harm().id(), selectedHarmId)) {
            box.getStyleClass().add("list-row-selected");
        }
        box.setOnMouseClicked(e -> {
            selectedHarmId = c.harm().id();
            showList();
        });
        return box;
    }

    private String nameOf(Long id) {
        return components.stream().filter(c -> c.id().equals(id)).map(ComponentDto::name)
                .findFirst().orElse(null);
    }

    private void renderAside() {
        aside.getChildren().clear();
        RiskChainDto chain = chains.stream().filter(c -> c.harm().id().equals(selectedHarmId))
                .findFirst().orElse(null);
        if (chain == null) {
            Label hint = Ui.label("Selecciona una cadena para ver su camino de falla.", "hint");
            hint.setWrapText(true);
            aside.getChildren().add(hint);
            return;
        }
        aside.getChildren().addAll(Ui.label("Camino de la falla", "form-title-sm"),
                Ui.label("Cargando…", "hint"));
        long hazardId = chain.hazard().id();
        Async.run(() -> api.listSequence(hazardId), seq -> {
            List<ChainPath.Step> path = new ArrayList<>();
            String origin = nameOf(chain.hazard().modelElementId());
            path.add(new ChainPath.Step("ORIGEN", origin, false));
            path.add(new ChainPath.Step("PELIGRO", chain.hazard().description(), false));
            for (RiskChainDto.SequenceDto s : seq) {
                String where = nameOf(s.modelElementId());
                path.add(new ChainPath.Step("PASO " + s.stepOrder() + (where == null ? "" : " · " + where),
                        s.description(), false));
            }
            path.add(new ChainPath.Step("SITUACIÓN PELIGROSA", chain.situation().description(), false));
            path.add(new ChainPath.Step("DAÑO", chain.harm().description(), true));
            aside.getChildren().setAll(Ui.label("Camino de la falla", "form-title-sm"), ChainPath.build(path));
        }, msg -> aside.getChildren().setAll(Ui.label("No se pudo cargar: " + msg, "field-error")));
    }

    private void showNew() {
        content.getChildren().setAll(new NewChainView(api, components, this::load, this::showList));
    }
}
