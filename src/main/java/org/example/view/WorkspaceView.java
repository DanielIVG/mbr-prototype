package org.example.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.example.api.DeviceDto;
import org.example.client.ApiClient;

import java.util.Objects;

/** Pantalla 3 · Área de trabajo de un dispositivo recién registrado (sin componentes todavía). */
public class WorkspaceView extends BorderPane {

    private final Navigator nav;
    private final DeviceDto device;
    private enum Section { STRUCTURE, RISKS, CRITERIA, EVALUATION, ANALYSIS, BEHAVIOR, MODEL, TRACEABILITY, RPN }

    private final ApiClient api;
    private Section section = Section.STRUCTURE;

    public WorkspaceView(ApiClient api, Navigator nav, DeviceDto device) {
        this.api = api;
        this.nav = nav;
        this.device = device;

        setTop(buildTopBar());
        show(Section.STRUCTURE);
    }

    private Node buildTopBar() {
        Button home = Ui.button("Inicio", "btn-link", nav::showHome);
        HBox left = new HBox(8, home, Ui.label("/", "crumb-sep"),
                Ui.label(device.name(), "crumb-current-strong"));
        left.setAlignment(Pos.CENTER_LEFT);
        Button edit = Ui.button("Editar datos del dispositivo", "btn-secondary",
                () -> nav.showEditDevice(device));
        return Ui.topBar(left, edit);
    }

    private Node buildNav() {
        VBox panel = new VBox(3);
        panel.getStyleClass().add("nav-panel");
        panel.setPrefWidth(248);
        panel.setMinWidth(248);

        panel.getChildren().addAll(
                navItem("Estructura", Section.STRUCTURE),
                navItem("Comportamiento", Section.BEHAVIOR),
                navItem("Riesgos", Section.RISKS),
                navItem("Criterios", Section.CRITERIA),
                navItem("Evaluación", Section.EVALUATION),
                navItem("Modelo de riesgos", Section.MODEL),
                navItem("Análisis (FTA)", Section.ANALYSIS),
                navItem("Dashboard RPN", Section.RPN),
                navItem("Trazabilidad", Section.TRACEABILITY),
                navItem("Reportes", null));
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        panel.getChildren().addAll(spacer, buildProgress());
        return panel;
    }

    private Node navItem(String text, Section target) {
        HBox item = new HBox(Ui.label(text));
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("nav-item");
        if (target == null) {
            item.getStyleClass().add("nav-item-off");
            Tooltip.install(item, new Tooltip("Próximamente"));
        } else {
            item.getStyleClass().add(target == section ? "nav-item-active" : "nav-item-on");
            item.setOnMouseClicked(e -> show(target));
        }
        return item;
    }

    private Node buildProgress() {
        StackPane track = new StackPane();
        track.getStyleClass().add("progress-track");
        track.setAlignment(Pos.CENTER_LEFT);
        Region fill = new Region();
        fill.getStyleClass().add("progress-fill");
        fill.setMaxHeight(5);
        fill.prefWidthProperty().bind(track.widthProperty().multiply(0.06));
        fill.maxWidthProperty().bind(track.widthProperty().multiply(0.06));
        track.getChildren().add(fill);

        Label text = Ui.label("Dispositivo registrado.\nFaltan componentes, cadenas y evaluación.", "hint");
        text.setWrapText(true);

        VBox box = new VBox(8, Ui.label("PROGRESO", "table-head"), track, text);
        box.setPadding(new Insets(14, 12, 2, 12));
        return box;
    }

    private Long behaviorComponent;
    private Long behaviorNode;

    /** Abre la edición del comportamiento en el componente donde está el nodo, resaltándolo. */
    private void openBehavior(Long componentId, Long nodeId) {
        behaviorComponent = componentId;
        behaviorNode = nodeId;
        show(Section.BEHAVIOR);
    }

    private void show(Section target) {
        section = target;
        setLeft(buildNav());
        setCenter(buildMain());
    }

    private Node buildMain() {
        VBox bannerHolder = new VBox();
        Async.run(api::getCriteria, c -> {
            if (c == null && section != Section.CRITERIA) {
                HBox banner = new HBox(10, Ui.label("Los criterios de aceptabilidad aún no están configurados. "
                        + "No podrás evaluar riesgos hasta definirlos (sección Criterios)."));
                banner.getStyleClass().add("workspace-banner");
                banner.setAlignment(Pos.CENTER_LEFT);
                bannerHolder.getChildren().setAll(banner);
            }
        }, msg -> { });

        VBox main = new VBox(bannerHolder);
        Node body;
        switch (section) {
            case STRUCTURE -> {
                main.getChildren().add(buildSummary());
                var structure = new StructureView(api, device, () -> { });
                var riskTree = new RiskTreeView(api, device, this::openBehavior);
                VBox.setVgrow(structure, Priority.ALWAYS);
                body = new VBox(structure, riskTree);
            }
            case BEHAVIOR -> {
                body = new BehaviorView(api, device, behaviorComponent, behaviorNode);
                behaviorComponent = null;
                behaviorNode = null;
            }
            case RISKS -> body = new RisksView(api, device, () -> { });
            case CRITERIA -> body = new CriteriaView(api, () -> show(Section.CRITERIA));
            case ANALYSIS -> body = new FtaView(api, device);
            case MODEL -> body = new ModelGraphView(api, device);
            case RPN -> body = new RpnDashboardView(api, device);
            case TRACEABILITY -> body = new TraceabilityView(api, device);
            default -> body = new AssessmentView(api, device, () -> show(Section.CRITERIA), () -> { });
        }
        main.getChildren().add(body);
        VBox.setVgrow(body, Priority.ALWAYS);
        return main;
    }

    private Node buildSummary() {
        VBox use = summaryBlock("Finalidad prevista", device.intendedUse());
        String misuseText = device.reasonablyForeseeableMisuse();
        VBox misuse = summaryBlock("Uso incorrecto razonablemente previsible",
                misuseText == null || misuseText.isBlank() ? "No se registró." : misuseText);
        // Dos columnas de ancho porcentual: el texto recibe un ancho real y no se calcula a 10 px.
        GridPane card = new GridPane();
        card.setHgap(28);
        ColumnConstraints half = new ColumnConstraints();
        half.setPercentWidth(50);
        ColumnConstraints other = new ColumnConstraints();
        other.setPercentWidth(50);
        card.getColumnConstraints().addAll(half, other);
        card.add(use, 0, 0);
        card.add(misuse, 1, 0);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16, 20, 16, 20));

        VBox wrap = new VBox(card);
        wrap.setPadding(new Insets(24, 28, 0, 28));
        return wrap;
    }

    private static VBox summaryBlock(String title, String body) {
        Label text = Ui.label(Objects.toString(body, ""), "muted");
        text.setWrapText(true);
        text.setMaxHeight(72);                       // ~4 líneas; el texto completo va en el tooltip
        text.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        Tooltip.install(text, new Tooltip(Objects.toString(body, "")));
        return new VBox(4, Ui.label(title, "table-head"), text);
    }
}
