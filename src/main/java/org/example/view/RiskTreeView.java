package org.example.view;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import org.example.api.BehaviorDtos.NodeDto;
import org.example.api.ComponentDto;
import org.example.api.DeviceDto;
import org.example.client.ApiClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Árbol de riesgos del dispositivo: cada camino de la raíz a una hoja peligrosa es una cadena de riesgo
 * (peligro → secuencia de eventos → situación peligrosa → daño). Al pasar el cursor sobre un nodo se indica
 * qué parte de la cadena representa; al hacer clic se abre su edición en el componente donde está.
 */
public class RiskTreeView extends VBox {

    private static final double W = 190;
    private static final double H = 46;
    private static final double GAP_X = 64;
    private static final double ROW = 60;
    private static final double MARGIN = 16;

    private final ApiClient api;
    private final DeviceDto device;
    private final BiConsumer<Long, Long> onOpen;          // (componente, nodo)
    private final Pane canvas = new Pane();
    private final Label status = Ui.label("Cargando árbol de riesgos…", "hint");

    public RiskTreeView(ApiClient api, DeviceDto device, BiConsumer<Long, Long> onOpen) {
        this.api = api;
        this.device = device;
        this.onOpen = onOpen;
        setSpacing(8);
        setPadding(new Insets(12, 28, 16, 28));
        ScrollPane sp = new ScrollPane(canvas);
        sp.getStyleClass().add("card");
        sp.setPrefHeight(290);
        sp.setMinHeight(290);
        getChildren().addAll(Ui.label("Árbol de riesgos del dispositivo", "section-title"), status, sp);
        load();
    }

    private void load() {
        long id = device.id();
        Async.run(() -> new Object[] {api.listComponents(id), api.listDeviceBehavior(id)}, data -> {
            @SuppressWarnings("unchecked") List<ComponentDto> comps = (List<ComponentDto>) data[0];
            @SuppressWarnings("unchecked") List<NodeDto> nodes = (List<NodeDto>) data[1];
            draw(comps, nodes);
        }, msg -> status.setText("No se pudo cargar el árbol de riesgos: " + msg));
    }

    // ------------------------------------------------------------------ dibujo

    private void draw(List<ComponentDto> comps, List<NodeDto> nodes) {
        canvas.getChildren().clear();
        if (nodes.isEmpty()) {
            status.setText("Aún no hay árboles de riesgo. Se crean en la sección «Comportamiento», dentro de un componente. "
                    + "Cada camino de la raíz a un resultado peligroso es una cadena de riesgo.");
            return;
        }
        long chains = nodes.stream().filter(n -> "OUTCOME_HAZARD".equals(n.kind())).count();
        status.setText(chains + (chains == 1 ? " cadena de riesgo" : " cadenas de riesgo")
                + " · Pase el cursor sobre un nodo para ver qué representa; haga clic para ir a donde está.");

        Map<Long, String> names = comps.stream().collect(Collectors.toMap(ComponentDto::id, ComponentDto::name, (a, b) -> a));
        Map<Long, List<NodeDto>> kids = new HashMap<>();
        for (NodeDto n : nodes) {
            if (n.parentId() != null) {
                kids.computeIfAbsent(n.parentId(), k -> new ArrayList<>()).add(n);
            }
        }
        kids.values().forEach(l -> l.sort(Comparator.comparingInt(RiskTreeView::branchOrder).thenComparing(NodeDto::id)));
        List<NodeDto> roots = nodes.stream().filter(n -> n.parentId() == null).sorted(Comparator.comparing(NodeDto::id)).toList();

        Map<Long, double[]> pos = new HashMap<>();
        Map<Long, Integer> depth = new HashMap<>();
        double[] row = {0};
        for (NodeDto r : roots) {
            place(r, 0, kids, pos, depth, row);
            row[0] += 0.5;
        }

        double maxX = 0;
        double maxY = 0;
        for (double[] p : pos.values()) {
            maxX = Math.max(maxX, p[0] + W);
            maxY = Math.max(maxY, p[1] + H);
        }
        canvas.setPrefSize(maxX + MARGIN, maxY + MARGIN);

        for (NodeDto n : nodes) {                       // primero las líneas, para que los nodos queden encima
            if (n.parentId() != null && pos.containsKey(n.parentId())) {
                edge(pos.get(n.parentId()), pos.get(n.id()), n.branch());
            }
        }
        for (NodeDto n : nodes) {
            canvas.getChildren().add(box(n, pos.get(n.id()), depth.get(n.id()), names));
        }
    }

    private static int branchOrder(NodeDto n) {
        return "YES".equals(n.branch()) ? 0 : "NO".equals(n.branch()) ? 1 : 2;
    }

    private void place(NodeDto n, int d, Map<Long, List<NodeDto>> kids, Map<Long, double[]> pos,
                       Map<Long, Integer> depth, double[] row) {
        depth.put(n.id(), d);
        double x = MARGIN + d * (W + GAP_X);
        List<NodeDto> ch = kids.getOrDefault(n.id(), List.of());
        if (ch.isEmpty()) {
            pos.put(n.id(), new double[] {x, MARGIN + row[0] * ROW});
            row[0] += 1;
            return;
        }
        for (NodeDto c : ch) {
            place(c, d + 1, kids, pos, depth, row);
        }
        double first = pos.get(ch.get(0).id())[1];
        double last = pos.get(ch.get(ch.size() - 1).id())[1];
        pos.put(n.id(), new double[] {x, (first + last) / 2});
    }

    private void edge(double[] from, double[] to, String branch) {
        double x1 = from[0] + W;
        double y1 = from[1] + H / 2;
        double x2 = to[0];
        double y2 = to[1] + H / 2;
        double xm = (x1 + x2) / 2;
        canvas.getChildren().addAll(line(x1, y1, xm, y1), line(xm, y1, xm, y2), line(xm, y2, x2, y2));
        if (branch != null) {
            Label l = Ui.label("YES".equals(branch) ? "SÍ" : "NO", "hint");
            l.setStyle("-fx-font-weight: bold; -fx-text-fill: " + ("YES".equals(branch) ? "#2F6B3F" : "#9B2C2C") + ";");
            l.setLayoutX(xm + 4);
            l.setLayoutY(y2 - 18);
            canvas.getChildren().add(l);
        }
    }

    private static Line line(double x1, double y1, double x2, double y2) {
        Line l = new Line(x1, y1, x2, y2);
        l.setStroke(Color.web("#A9A49B"));
        l.setStrokeWidth(1.5);
        return l;
    }

    // ------------------------------------------------------------------ nodos

    private Label box(NodeDto n, double[] p, int depth, Map<Long, String> names) {
        Label l = new Label(shorten(n.description(), 62));
        l.setWrapText(true);
        l.setPrefSize(W, H);
        l.setMinSize(W, H);
        l.setMaxSize(W, H);
        l.setLayoutX(p[0]);
        l.setLayoutY(p[1]);
        l.setStyle(style(n));
        l.setOnMouseClicked(e -> onOpen.accept(n.elementId(), n.id()));
        Tooltip.install(l, new Tooltip(tip(n, depth, names)));
        return l;
    }

    private static String style(NodeDto n) {
        String bg;
        String border;
        String text = "#1C1917";
        switch (n.kind()) {
            case "CONDITION" -> { bg = "#FBF0DC"; border = "#E6C98A"; }
            case "OUTCOME_SAFE" -> { bg = "#E3F1E7"; border = "#8BBF9B"; }
            case "OUTCOME_HAZARD" -> { bg = "#FBE4E4"; border = "#D99A9A"; }
            default -> {
                if (n.parentId() == null) {
                    bg = "#15616D";
                    border = "#15616D";
                    text = "#FFFFFF";
                } else {
                    bg = "#FFFFFF";
                    border = "#15616D";
                }
            }
        }
        return "-fx-background-color: " + bg + "; -fx-border-color: " + border + "; -fx-border-radius: 8;"
                + " -fx-background-radius: 8; -fx-padding: 6 10 6 10; -fx-font-size: 12px; -fx-cursor: hand;"
                + " -fx-text-fill: " + text + ";";
    }

    /** Qué parte de la cadena ISO 14971 representa el nodo. */
    private static String tip(NodeDto n, int depth, Map<Long, String> names) {
        String where = names.getOrDefault(n.elementId(), "componente " + n.elementId());
        StringBuilder t = new StringBuilder();
        switch (n.kind()) {
            case "OUTCOME_HAZARD" -> t.append("SITUACIÓN PELIGROSA Y DAÑO (fin de la cadena)\n")
                    .append("Situación peligrosa: ").append(n.situation()).append('\n')
                    .append("Daño: ").append(n.harm());
            case "OUTCOME_SAFE" -> t.append("RESULTADO SEGURO: este camino termina sin daño.");
            case "CONDITION" -> t.append(depth == 0 ? "PELIGRO · PASO 1 DE LA SECUENCIA (condición)"
                    : "PASO " + (depth + 1) + " DE LA SECUENCIA DE EVENTOS (condición SÍ/NO)");
            default -> t.append(depth == 0 ? "PELIGRO · PASO 1 DE LA SECUENCIA DE EVENTOS"
                    : "PASO " + (depth + 1) + " DE LA SECUENCIA DE EVENTOS");
        }
        t.append("\n\n").append(n.description()).append("\nComponente: ").append(where)
                .append("\n\nClic para ir a su edición.");
        return t.toString();
    }

    private static String shorten(String s, int max) {
        return s == null ? "" : s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
