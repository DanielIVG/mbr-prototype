package org.example.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import org.example.api.AssessmentDtos.EstimationDto;
import org.example.api.ComponentDto;
import org.example.api.DeviceDto;
import org.example.api.RiskChainDto;
import org.example.client.ApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Visualización del modelo de riesgos: componentes → peligro → situación peligrosa → daño.
 * El daño se colorea con el veredicto vigente (aceptable / tolerable / inaceptable / sin estimar).
 * Las líneas punteadas unen los componentes por donde pasa la secuencia con la situación peligrosa.
 */
public class ModelGraphView extends VBox {

    private static final double NODE_W = 200;
    private static final double NODE_H = 44;
    private static final double ROW = 64;
    private static final double COL = 270;
    private static final double MARGIN = 20;

    private record Chain(RiskChainDto dto, List<Long> stepComponents, EstimationDto estimation) {}

    private final ApiClient api;
    private final DeviceDto device;
    private final VBox content = new VBox(12);

    public ModelGraphView(ApiClient api, DeviceDto device) {
        this.api = api;
        this.device = device;
        setPadding(new Insets(20, 28, 20, 28));
        setSpacing(12);
        getChildren().add(content);
        load();
    }

    private void load() {
        content.getChildren().setAll(Ui.label("Construyendo el modelo…", "hint"));
        long id = device.id();
        Async.run(() -> {
            List<ComponentDto> comps = api.listComponents(id);
            Set<Long> ids = comps.stream().map(ComponentDto::id).collect(Collectors.toSet());
            List<Chain> chains = new ArrayList<>();
            for (RiskChainDto c : api.listChains()) {
                Long origin = c.hazard().modelElementId();
                if (origin == null || !ids.contains(origin)) {
                    continue;
                }
                List<Long> steps = api.listSequence(c.hazard().id()).stream()
                        .map(RiskChainDto.SequenceDto::modelElementId).filter(Objects::nonNull).distinct().toList();
                chains.add(new Chain(c, steps, api.latestEstimation(c.harm().id())));
            }
            return new Object[] {comps, chains};
        }, data -> {
            @SuppressWarnings("unchecked") List<ComponentDto> comps = (List<ComponentDto>) data[0];
            @SuppressWarnings("unchecked") List<Chain> chains = (List<Chain>) data[1];
            show(comps, chains);
        }, msg -> {
            Label l = Ui.label("No se pudo construir el modelo: " + msg, "field-error");
            l.setWrapText(true);
            content.getChildren().setAll(l, Ui.button("Reintentar", "btn-secondary", this::load));
        });
    }

    private void show(List<ComponentDto> comps, List<Chain> chains) {
        if (chains.isEmpty()) {
            Label l = Ui.label("Todavía no hay cadenas de riesgo que mostrar. Crea una en la sección Riesgos.", "hint");
            l.setWrapText(true);
            content.getChildren().setAll(l);
            return;
        }
        Map<Long, ComponentDto> byId = comps.stream().collect(Collectors.toMap(ComponentDto::id, c -> c));

        // Componentes involucrados, ubicados a la altura media de las cadenas donde participan.
        Map<Long, List<Integer>> rowsOf = new LinkedHashMap<>();
        for (int i = 0; i < chains.size(); i++) {
            Chain c = chains.get(i);
            rowsOf.computeIfAbsent(c.dto().hazard().modelElementId(), k -> new ArrayList<>()).add(i);
            for (Long s : c.stepComponents()) {
                rowsOf.computeIfAbsent(s, k -> new ArrayList<>()).add(i);
            }
        }
        Map<Long, Double> compY = new HashMap<>();
        List<Map.Entry<Long, List<Integer>>> ordered = new ArrayList<>(rowsOf.entrySet());
        ordered.sort((a, b) -> Double.compare(avg(a.getValue()), avg(b.getValue())));
        double nextFree = MARGIN + 28;
        for (var e : ordered) {
            double y = Math.max(nextFree, MARGIN + 28 + avg(e.getValue()) * ROW);
            compY.put(e.getKey(), y);
            nextFree = y + ROW * 0.8;
        }
        double height = Math.max(nextFree, MARGIN + 28 + chains.size() * ROW) + MARGIN;

        Pane pane = new Pane();
        pane.setPrefSize(MARGIN * 2 + COL * 3 + NODE_W, height);
        String[] heads = {"COMPONENTE", "PELIGRO", "SITUACIÓN PELIGROSA", "DAÑO Y VEREDICTO"};
        for (int c = 0; c < heads.length; c++) {
            Label h = Ui.label(heads[c], "table-head");
            h.setLayoutX(MARGIN + c * COL);
            h.setLayoutY(MARGIN);
            pane.getChildren().add(h);
        }
        List<Node> edges = new ArrayList<>();
        List<Node> nodes = new ArrayList<>();
        double xComp = MARGIN, xHaz = MARGIN + COL, xSit = MARGIN + 2 * COL, xHarm = MARGIN + 3 * COL;

        for (var e : compY.entrySet()) {
            ComponentDto c = byId.get(e.getKey());
            nodes.add(box(xComp, e.getValue(), c == null ? "(desactivado)" : c.name(), null,
                    "#F1F7F8", "#CDE3E6", c != null && c.essential() ? "Componente esencial" : "Componente"));
        }
        for (int i = 0; i < chains.size(); i++) {
            Chain c = chains.get(i);
            double y = MARGIN + 28 + i * ROW;
            String status = status(c.estimation());
            nodes.add(box(xHaz, y, c.dto().hazard().description(), null, "#FFFFFF", "#E3E0DA", "Peligro"));
            nodes.add(box(xSit, y, c.dto().situation().description(), null, "#FFFFFF", "#E3E0DA", "Situación peligrosa"));
            nodes.add(box(xHarm, y, c.dto().harm().description(),
                    c.estimation() == null ? "sin estimar" : c.estimation().riskLevel(),
                    fill(status), stroke(status), tooltipOf(c)));
            Long origin = c.dto().hazard().modelElementId();
            edges.add(line(xComp + NODE_W, compY.get(origin) + NODE_H / 2, xHaz, y + NODE_H / 2, false));
            edges.add(line(xHaz + NODE_W, y + NODE_H / 2, xSit, y + NODE_H / 2, false));
            edges.add(line(xSit + NODE_W, y + NODE_H / 2, xHarm, y + NODE_H / 2, false));
            for (Long s : c.stepComponents()) {
                if (!s.equals(origin)) {
                    edges.add(line(xComp + NODE_W, compY.get(s) + NODE_H / 2, xSit, y + NODE_H / 2, true));
                }
            }
        }
        pane.getChildren().addAll(edges);                // las líneas quedan debajo de las cajas
        pane.getChildren().addAll(nodes);

        ScrollPane scroll = new ScrollPane(pane);
        scroll.getStyleClass().add("plain-scroll");
        VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);
        content.getChildren().setAll(legend(), scroll);
        VBox.setVgrow(content, javafx.scene.layout.Priority.ALWAYS);
    }

    private static double avg(List<Integer> rows) {
        return rows.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    private static String status(EstimationDto e) {
        if (e == null) {
            return "none";
        }
        return e.acceptable() ? "ok" : "Inaceptable".equals(e.riskLevel()) ? "bad" : "warn";
    }

    private static String fill(String status) {
        return switch (status) {
            case "ok" -> "#DDF0E4";
            case "warn" -> "#FBEBC8";
            case "bad" -> "#F8DCDC";
            default -> "#EEECE8";
        };
    }

    private static String stroke(String status) {
        return switch (status) {
            case "ok" -> "#8FCBA6";
            case "warn" -> "#E3B95C";
            case "bad" -> "#D98B8B";
            default -> "#D5D1CA";
        };
    }

    private static String tooltipOf(Chain c) {
        EstimationDto e = c.estimation();
        if (e == null) {
            return Objects.toString(c.dto().harm().description(), "");
        }
        return c.dto().harm().description() + "\nSeveridad " + e.severity() + " (" + e.severityDescriptor()
                + ") · probabilidad nivel " + e.probabilityLevel() + " (" + e.probabilityDescriptor() + ")\n"
                + e.riskLevel() + (e.acceptable() ? " · aceptable" : " · no aceptable");
    }

    private Node legend() {
        HBox box = new HBox(14, chip("Aceptable", "ok"), chip("Tolerable (requiere control)", "warn"),
                chip("Inaceptable", "bad"), chip("Sin estimar", "none"),
                Ui.label("┄ punteada: el componente participa en la secuencia", "hint"));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static Node chip(String text, String status) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + fill(status) + "; -fx-border-color: " + stroke(status)
                + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 2 10 2 10; -fx-font-size: 11px;");
        return l;
    }

    /** Caja de una sola línea con elipsis; el texto completo va en el tooltip. */
    private static Node box(double x, double y, String text, String badge, String fill, String stroke, String tip) {
        Label title = new Label(Objects.toString(text, ""));
        title.setMaxWidth(NODE_W - 20);
        title.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        title.setStyle("-fx-font-size: 12px;");
        VBox b = new VBox(1, title);
        if (badge != null) {
            Label bl = new Label(badge);
            bl.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #4A4640;");
            b.getChildren().add(bl);
        }
        b.setPadding(new Insets(5, 10, 5, 10));
        b.setPrefSize(NODE_W, NODE_H);
        b.setMinSize(NODE_W, NODE_H);
        b.setMaxSize(NODE_W, NODE_H);
        b.setLayoutX(x);
        b.setLayoutY(y);
        b.setStyle("-fx-background-color: " + fill + "; -fx-border-color: " + stroke
                + "; -fx-border-radius: 8; -fx-background-radius: 8;");
        Tooltip.install(b, new Tooltip(tip == null || tip.equals(text) ? Objects.toString(text, "") : tip + ": " + text));
        return b;
    }

    private static Line line(double x1, double y1, double x2, double y2, boolean dashed) {
        Line l = new Line(x1, y1, x2, y2);
        l.setStroke(Color.web(dashed ? "#9AA5A8" : "#8A857E"));
        l.setStrokeWidth(dashed ? 1.2 : 1.6);
        if (dashed) {
            l.getStrokeDashArray().addAll(5.0, 4.0);
        }
        return l;
    }
}
