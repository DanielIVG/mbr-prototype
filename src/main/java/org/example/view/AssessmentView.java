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
import org.example.api.AssessmentDtos.ControlDto;
import org.example.api.AssessmentDtos.CriteriaDto;
import org.example.api.AssessmentDtos.EstimationDto;
import org.example.api.AssessmentDtos.EstimationRequest;
import org.example.api.AssessmentDtos.EvidenceLevelDto;
import org.example.api.AssessmentDtos.QuantityDto;
import org.example.api.AssessmentDtos.SensitivityDto;
import org.example.api.AssessmentDtos.VerificationDto;
import org.example.api.ComponentDto;
import org.example.api.DeviceDto;
import org.example.api.RiskChainDto;
import org.example.client.ApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pantallas 7 a 9 · Evaluación de cada cadena de riesgo:
 * estimación P = P1 × P2 con procedencia y veredicto (7 y 8), control y verificación (8),
 * y sensibilidad del veredicto (9).
 */
public class AssessmentView extends HBox {

    private enum Tab { ESTIMATION, CONTROL, SENSITIVITY }

    private record Opt(String code, String text) {
        @Override
        public String toString() {
            return text;
        }
    }

    private static final List<Opt> MODES = List.of(
            new Opt("FAILURE_MODE", "Desde la falla: tasa × fracción × barreras, y P2"),
            new Opt("P1_P2", "Directa: P1 × P2"),
            new Opt("FTA", "Con el árbol de fallas: P1 sale del árbol, y P2"),
            new Opt("TOTAL_ONLY", "Solo probabilidad total (exige justificación)"));

    private final ApiClient api;
    private final DeviceDto device;
    private final Runnable goCriteria;
    private final Runnable onChanged;

    private CriteriaDto criteria;
    private List<EvidenceLevelDto> evidence = List.of();
    private List<RiskChainDto> chains = List.of();
    private final Map<Long, EstimationDto> latest = new HashMap<>();
    private Long selected;
    private Tab tab = Tab.ESTIMATION;
    private String reasonPrefill = "";

    private final VBox listBox = new VBox(8);
    private final VBox right = new VBox(12);
    private final HBox tabsBar = new HBox(6);
    private final VBox body = new VBox(14);

    public AssessmentView(ApiClient api, DeviceDto device, Runnable goCriteria, Runnable onChanged) {
        this.api = api;
        this.device = device;
        this.goCriteria = goCriteria;
        this.onChanged = onChanged;
        setSpacing(20);
        setPadding(new Insets(20, 28, 20, 28));
        load();
    }

    // ------------------------------------------------------------------ carga

    private void load() {
        getChildren().setAll(Ui.label("Cargando evaluación…", "hint"));
        long deviceId = device.id();
        Async.run(() -> {
            CriteriaDto c = api.getCriteria();
            if (c == null) {
                return new Object[] {null};
            }
            Set<Long> ids = api.listComponents(deviceId).stream().map(ComponentDto::id).collect(Collectors.toSet());
            List<RiskChainDto> mine = api.listChains().stream()
                    .filter(x -> x.hazard().modelElementId() != null && ids.contains(x.hazard().modelElementId()))
                    .toList();
            Map<Long, EstimationDto> est = new HashMap<>();
            for (RiskChainDto ch : mine) {
                EstimationDto e = api.latestEstimation(ch.harm().id());
                if (e != null) {
                    est.put(ch.harm().id(), e);
                }
            }
            return new Object[] {c, api.listEvidenceLevels(), mine, est};
        }, data -> {
            if (data[0] == null) {
                showNoCriteria();
                return;
            }
            criteria = (CriteriaDto) data[0];
            @SuppressWarnings("unchecked") List<EvidenceLevelDto> ev = (List<EvidenceLevelDto>) data[1];
            @SuppressWarnings("unchecked") List<RiskChainDto> ch = (List<RiskChainDto>) data[2];
            @SuppressWarnings("unchecked") Map<Long, EstimationDto> est = (Map<Long, EstimationDto>) data[3];
            evidence = ev;
            chains = ch;
            latest.clear();
            latest.putAll(est);
            onChanged.run();
            build();
        }, msg -> {
            Label l = Ui.label("No se pudo cargar la evaluación: " + msg, "field-error");
            l.setWrapText(true);
            getChildren().setAll(new VBox(8, l, Ui.button("Reintentar", "btn-secondary", this::load)));
        });
    }

    private void showNoCriteria() {
        Label l = Ui.label("Antes de evaluar riesgos hay que definir los criterios de aceptabilidad "
                + "(escalas, matriz y criterios).", "page-lead");
        l.setWrapText(true);
        getChildren().setAll(new VBox(12, l, Ui.button("Definir criterios", "btn-primary", goCriteria)));
    }

    // ------------------------------------------------------------------ estructura de pantalla

    private void build() {
        listBox.getChildren().clear();
        for (RiskChainDto c : chains) {
            listBox.getChildren().add(row(c));
        }
        if (chains.isEmpty()) {
            VBox empty = Ui.emptyState(Ui.ICON_CLIPBOARD,
                    "No hay cadenas de riesgo para evaluar",
                    "Ve a la sección Riesgos para crear las primeras cadenas de riesgo del dispositivo.");
            listBox.getChildren().add(empty);
        }
        ScrollPane left = new ScrollPane(listBox);
        left.setFitToWidth(true);
        left.getStyleClass().add("plain-scroll");
        left.setPrefWidth(320);
        left.setMinWidth(320);
        left.setMaxWidth(320);

        ScrollPane main = new ScrollPane(right);
        main.setFitToWidth(true);
        main.getStyleClass().add("plain-scroll");
        HBox.setHgrow(main, Priority.ALWAYS);
        getChildren().setAll(left, main);
        renderRight();
    }

    private HBox row(RiskChainDto c) {
        Label title = Ui.label(Objects.toString(c.harm().description(), ""), "row-title");
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS);
        EstimationDto e = latest.get(c.harm().id());
        Label chip = e == null ? Ui.label("Sin estimar", "hint")
                : Ui.label(e.riskLevel(), e.acceptable() ? "tag" : "tag-bad");
        HBox box = new HBox(10, title, chip);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("list-row");
        if (Objects.equals(c.harm().id(), selected)) {
            box.getStyleClass().add("list-row-selected");
        }
        box.setOnMouseClicked(ev -> {
            selected = c.harm().id();
            tab = Tab.ESTIMATION;
            reasonPrefill = "";
            build();
        });
        return box;
    }

    private void renderRight() {
        right.getChildren().clear();
        RiskChainDto chain = chains.stream().filter(c -> c.harm().id().equals(selected)).findFirst().orElse(null);
        if (chain == null) {
            Label hint = Ui.label("Elige una cadena de riesgo de la lista para estimarla.", "hint");
            hint.setWrapText(true);
            right.getChildren().add(hint);
            return;
        }
        Label t = Ui.label(Objects.toString(chain.harm().description(), ""), "form-title-sm");
        t.setWrapText(true);
        Label sub = Ui.label("Peligro: " + chain.hazard().description() + " · Situación: "
                + chain.situation().description(), "hint");
        sub.setWrapText(true);
        sub.setMaxHeight(48);
        tabsBar.getChildren().setAll(tabButton(Tab.ESTIMATION, "Estimación y veredicto"),
                tabButton(Tab.CONTROL, "Control y verificación"), tabButton(Tab.SENSITIVITY, "Sensibilidad"));
        body.getChildren().clear();
        right.getChildren().addAll(t, sub, tabsBar, body);
        switch (tab) {
            case ESTIMATION -> estimationTab(chain);
            case CONTROL -> controlTab(chain);
            case SENSITIVITY -> sensitivityTab(chain);
        }
    }

    private Button tabButton(Tab target, String text) {
        return Ui.button(text, tab == target ? "btn-tab-active" : "btn-tab", () -> {
            tab = target;
            renderRight();
        });
    }

    private static Node card(Node... children) {
        VBox box = new VBox(10, children);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14, 16, 14, 16));
        return box;
    }

    private static Label wrapped(String text, String... styles) {
        Label l = Ui.label(text, styles);
        l.setWrapText(true);
        return l;
    }

    private static String num(double v) {
        return v == 0 ? "0" : String.format(Locale.ROOT, "%.3g", v);
    }

    // ------------------------------------------------------------------ 7 y 8 · estimación y veredicto

    private void estimationTab(RiskChainDto chain) {
        long harmId = chain.harm().id();
        EstimationDto e = latest.get(harmId);
        if (e != null) {
            body.getChildren().add(verdictCard(e));
            Async.run(() -> api.estimationHistory(harmId), hist -> {
                if (hist.size() > 1) {
                    VBox lines = new VBox(4);
                    for (EstimationDto h : hist) {
                        lines.getChildren().add(Ui.label(h.date().substring(0, 10) + " · severidad " + h.severity()
                                + " · P = " + num(h.pTotal()) + " " + h.unit() + " → " + h.riskLevel()
                                + (h.acceptable() ? " (aceptable)" : " (no aceptable)"), "hint"));
                    }
                    body.getChildren().add(1, card(Ui.label("Historial (riesgo inicial y residual)", "field-label"), lines));
                }
            }, msg -> { });
        }
        body.getChildren().add(estimationForm(harmId, e != null));
    }

    private Node verdictCard(EstimationDto e) {
        Label level = Ui.label(e.riskLevel(), e.acceptable() ? "verdict-ok" : "verdict-bad");
        Label state = Ui.label(e.acceptable() ? "Riesgo aceptable con los criterios definidos"
                : "Riesgo NO aceptable: requiere medidas de control y reevaluación", "field-label");
        String formula = e.p1() != null && e.p2() != null
                ? "P = P1 × P2 = " + num(e.p1()) + " × " + num(e.p2()) + " = " + num(e.pTotal()) + " " + e.unit()
                : "P (total declarada) = " + num(e.pTotal()) + " " + e.unit();
        Label detail = wrapped("Severidad " + e.severity() + " (" + e.severityDescriptor() + ") · Probabilidad nivel "
                + e.probabilityLevel() + " (" + e.probabilityDescriptor() + ")", "muted");
        List<Node> items = new ArrayList<>(List.of(new HBox(12, level, state), wrapped(formula, "field-label"), detail));
        if (e.ftaAnalysisId() != null) {
            items.add(wrapped("P1 calculado con el árbol de fallas (análisis " + e.ftaAnalysisId() + ").", "hint"));
        }
        for (QuantityDto q : e.quantities()) {
            items.add(wrapped(roleName(q.role()) + ": " + num(q.value()) + " " + q.unit()
                    + (q.min() != null && q.max() != null ? " [" + num(q.min()) + " – " + num(q.max()) + "]" : "")
                    + " · fuente: " + q.source() + " · evidencia: " + q.evidence() + " · " + q.author(), "hint"));
        }
        if (e.justification() != null) {
            items.add(wrapped("Por qué no se descompone: " + e.justification(), "hint"));
        }
        return card(items.toArray(new Node[0]));
    }

    private static String roleName(String role) {
        return switch (role) {
            case "RATE" -> "Tasa de falla";
            case "FRACTION" -> "Fracción del modo";
            case "BARRIER" -> "Cobertura de barrera";
            case "TOTAL" -> "Probabilidad total";
            default -> role;
        };
    }

    private Node estimationForm(long harmId, boolean reassessment) {
        ComboBox<Opt> mode = new ComboBox<>();
        mode.getItems().addAll(MODES);
        mode.setValue(MODES.get(0));
        mode.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Opt> severity = new ComboBox<>();
        criteria.severities().forEach(s -> severity.getItems().add(
                new Opt(String.valueOf(s.value()), s.value() + " — " + s.descriptor())));
        severity.setValue(severity.getItems().get(severity.getItems().size() - 1));
        severity.setMaxWidth(Double.MAX_VALUE);

        TextField author = new TextField(System.getProperty("user.name", ""));
        TextField reason = new TextField(reasonPrefill);
        reason.setPromptText(reassessment ? "Motivo de la reevaluación (obligatorio)" : "");
        TextArea justification = new TextArea();
        justification.setPrefRowCount(2);
        justification.setWrapText(true);
        justification.setPromptText("Por qué no se puede descomponer en P1 y P2 (obligatorio en este modo)");

        VBox quantities = new VBox(10);
        List<QuantityForm> editors = new ArrayList<>();
        Button addBarrier = Ui.button("+ Barrera", "btn-secondary", () -> {
            QuantityForm b = new QuantityForm("BARRIER", "Cobertura de la barrera (0 a 1: qué fracción de fallas detiene)", "adimensional", false, evidence);
            editors.add(editors.size() - 1, b);               // antes de P2, que es el último
            quantities.getChildren().add(quantities.getChildren().size() - 1, b.node());
        });
        Runnable rebuild = () -> {
            editors.clear();
            quantities.getChildren().clear();
            String m = mode.getValue().code();
            Ui.show(justification, m.equals("TOTAL_ONLY"));
            Ui.show(addBarrier, m.equals("FAILURE_MODE"));
            switch (m) {
                case "FAILURE_MODE" -> {
                    editors.add(new QuantityForm("RATE", "Tasa de falla del componente", criteria.exposureUnit(), true, evidence));
                    editors.add(new QuantityForm("FRACTION", "Fracción de fallas que son de este modo (0 a 1)", "adimensional", false, evidence));
                    editors.add(new QuantityForm("P2", "P2: probabilidad de que la situación peligrosa cause el daño (0 a 1)", "adimensional", false, evidence));
                }
                case "P1_P2" -> {
                    editors.add(new QuantityForm("P1", "P1: probabilidad de que ocurra la situación peligrosa", criteria.exposureUnit(), true, evidence));
                    editors.add(new QuantityForm("P2", "P2: probabilidad de que la situación peligrosa cause el daño (0 a 1)", "adimensional", false, evidence));
                }
                case "FTA" -> editors.add(new QuantityForm("P2", "P2: probabilidad de que la situación peligrosa cause el daño (0 a 1)", "adimensional", false, evidence));
                default -> editors.add(new QuantityForm("TOTAL", "Probabilidad total de que ocurra el daño", criteria.exposureUnit(), true, evidence));
            }
            editors.forEach(q -> quantities.getChildren().add(q.node()));
        };
        mode.setOnAction(e -> rebuild.run());
        rebuild.run();

        Label error = wrapped("", "field-error");
        Button save = Ui.button(reassessment ? "Guardar reevaluación" : "Estimar y evaluar", "btn-primary", () -> { });
        save.setOnAction(e -> {
            error.setText("");
            if (author.getText().isBlank()) {
                error.setText("Indica quién registra los valores (autor).");
                return;
            }
            List<QuantityDto> qs = new ArrayList<>();
            try {
                for (QuantityForm q : editors) {
                    qs.add(q.toDto(author.getText().trim()));
                }
            } catch (IllegalArgumentException ex) {
                error.setText(ex.getMessage());
                return;
            }
            EstimationRequest req = new EstimationRequest(mode.getValue().code(),
                    Integer.parseInt(severity.getValue().code()), qs,
                    justification.getText().isBlank() ? null : justification.getText().trim(),
                    reason.getText().isBlank() ? null : reason.getText().trim());
            save.setDisable(true);
            Async.run(() -> api.estimate(harmId, req), ok -> {
                reasonPrefill = "";
                load();
            }, msg -> {
                error.setText(msg);
                save.setDisable(false);
            });
        });

        List<Node> nodes = new ArrayList<>(List.of(
                Ui.label(reassessment ? "Reevaluar (riesgo residual)" : "Estimar la probabilidad", "form-title-sm"),
                wrapped("Cada número lleva unidad, fuente y nivel de evidencia. El veredicto sale de la matriz de "
                        + "criterios definida; el valor RPN de FMEA no decide la aceptabilidad.", "hint"),
                labeled("Cómo se estima", mode), labeled("Severidad del daño", severity)));
        nodes.add(quantities);
        nodes.add(addBarrier);
        nodes.add(justification);
        if (reassessment) {
            nodes.add(labeled("Motivo de la reevaluación", reason));
        }
        nodes.add(labeled("Autor de los valores", author));
        nodes.add(error);
        nodes.add(save);
        return card(nodes.toArray(new Node[0]));
    }

    private static Node labeled(String label, Node input) {
        return new VBox(4, Ui.label(label, "field-label"), input);
    }

    // ------------------------------------------------------------------ 8 · control y verificación

    private void controlTab(RiskChainDto chain) {
        long harmId = chain.harm().id();
        VBox holder = new VBox(12, Ui.label("Cargando medidas de control…", "hint"));
        body.getChildren().add(holder);
        Async.run(() -> {
            List<ControlDto> cs = api.listControls(harmId);
            Map<Long, List<VerificationDto>> ver = new HashMap<>();
            for (ControlDto c : cs) {
                ver.put(c.id(), api.listVerifications(c.id()));
            }
            return new Object[] {cs, ver};
        }, data -> {
            @SuppressWarnings("unchecked") List<ControlDto> cs = (List<ControlDto>) data[0];
            @SuppressWarnings("unchecked") Map<Long, List<VerificationDto>> ver = (Map<Long, List<VerificationDto>>) data[1];
            holder.getChildren().setAll(controlContent(harmId, cs, ver));
        }, msg -> holder.getChildren().setAll(wrapped("No se pudieron cargar las medidas: " + msg, "field-error")));
    }

    private List<Node> controlContent(long harmId, List<ControlDto> cs, Map<Long, List<VerificationDto>> ver) {
        List<Node> out = new ArrayList<>();
        EstimationDto e = latest.get(harmId);
        if (e == null) {
            out.add(wrapped("Estima primero el riesgo inicial para saber si requiere control.", "hint"));
        } else if (e.acceptable() && cs.isEmpty()) {
            out.add(wrapped("El riesgo ya es aceptable con los criterios definidos; las medidas de control son opcionales.", "hint"));
        }
        VBox list = new VBox(10);
        boolean anyEffective = false;
        for (ControlDto c : cs) {
            VBox item = new VBox(4, wrapped(c.description() + (c.type() == null ? "" : " · " + c.type()), "field-label"));
            for (VerificationDto v : ver.getOrDefault(c.id(), List.of())) {
                String result = !v.implemented() ? "no implementada"
                        : v.effective() == null ? "implementada, eficacia sin evaluar"
                        : v.effective() ? "implementada y eficaz" : "implementada pero NO eficaz";
                anyEffective |= v.implemented() && Boolean.TRUE.equals(v.effective());
                item.getChildren().add(wrapped("Verificación: " + v.method() + " → " + result
                        + (v.evidence() == null || v.evidence().isBlank() ? "" : " · evidencia: " + v.evidence()), "hint"));
            }
            if (ver.getOrDefault(c.id(), List.of()).isEmpty()) {
                item.getChildren().add(Ui.label("Sin verificación todavía", "hint"));
            }
            list.getChildren().add(item);
        }
        if (cs.isEmpty()) {
            list.getChildren().add(Ui.label("Aún no hay medidas de control.", "hint"));
        }
        out.add(card(Ui.label("Medidas de control", "form-title-sm"), list));
        if (anyEffective) {
            Button reassess = Ui.button("Reevaluar el riesgo residual", "btn-primary", () -> {
                reasonPrefill = "Medida de control implementada y verificada";
                tab = Tab.ESTIMATION;
                renderRight();
            });
            out.add(card(wrapped("Hay medidas implementadas y eficaces: reevalúa el riesgo con los nuevos valores "
                    + "para obtener el riesgo residual.", "muted"), reassess));
        }

        TextField desc = new TextField();
        desc.setPromptText("Qué se hace para reducir el riesgo");
        TextField type = new TextField();
        type.setPromptText("Tipo (diseño seguro, protección, información de seguridad)");
        Label err1 = wrapped("", "field-error");
        out.add(card(Ui.label("Agregar medida", "field-label"), desc, type, err1,
                Ui.button("Agregar", "btn-secondary", () -> Async.run(
                        () -> api.addControl(harmId, desc.getText(), type.getText().isBlank() ? null : type.getText().trim()),
                        ok -> renderRight(), err1::setText))));

        if (!cs.isEmpty()) {
            ComboBox<Opt> which = new ComboBox<>();
            cs.forEach(c -> which.getItems().add(new Opt(String.valueOf(c.id()), c.description())));
            which.setValue(which.getItems().get(0));
            which.setMaxWidth(Double.MAX_VALUE);
            TextField method = new TextField();
            method.setPromptText("Método (ensayo, inspección, análisis…)");
            CheckBox implemented = new CheckBox("La medida está implementada");
            ComboBox<Opt> eff = new ComboBox<>();
            eff.getItems().addAll(new Opt("", "Eficacia sin evaluar"), new Opt("1", "Eficaz"), new Opt("0", "No eficaz"));
            eff.setValue(eff.getItems().get(0));
            TextField evid = new TextField();
            evid.setPromptText("Evidencia (informe, registro de ensayo…)");
            Label err2 = wrapped("", "field-error");
            out.add(card(Ui.label("Registrar verificación", "field-label"), which, method, implemented, eff, evid, err2,
                    Ui.button("Registrar", "btn-secondary", () -> {
                        Boolean effective = eff.getValue().code().isEmpty() ? null : eff.getValue().code().equals("1");
                        Async.run(() -> api.addVerification(Long.parseLong(which.getValue().code()), method.getText(),
                                implemented.isSelected(), effective, evid.getText()),
                                ok -> renderRight(), err2::setText);
                    })));
        }
        return out;
    }

    // ------------------------------------------------------------------ 9 · sensibilidad

    private void sensitivityTab(RiskChainDto chain) {
        long harmId = chain.harm().id();
        if (latest.get(harmId) == null) {
            body.getChildren().add(wrapped("Estima el riesgo primero: la sensibilidad se calcula sobre la estimación vigente.", "hint"));
            return;
        }
        VBox holder = new VBox(12, Ui.label("Calculando…", "hint"));
        body.getChildren().add(holder);
        Async.run(() -> api.sensitivity(harmId), r -> holder.getChildren().setAll(sensitivityContent(r)),
                msg -> holder.getChildren().setAll(wrapped(msg, "field-error")));
    }

    private List<Node> sensitivityContent(SensitivityDto r) {
        List<Node> out = new ArrayList<>();
        out.add(wrapped("Nivel de riesgo actual: " + r.baselineLevel() + ". Se prueba cada valor con rango dentro de su "
                + "rango, de a uno, y además las combinaciones de extremos.", "muted"));
        VBox rows = new VBox(6);
        boolean anyRange = false;
        for (var p : r.params()) {
            anyRange |= !p.name().equals("Severidad");
            String text = p.decides()
                    ? "DECIDE: cambia a " + p.changes().stream().map(c -> c.riskLevel()).distinct().collect(Collectors.joining(", "))
                    + " con valores como " + num(p.changes().get(0).value())
                    : "No decide por sí solo (el nivel no cambia en los valores probados)";
            rows.getChildren().add(wrapped(p.name() + ": " + text, p.decides() ? "field-label" : "muted"));
        }
        out.add(card(Ui.label("Uno a la vez", "field-label"), rows));
        if (!anyRange) {
            out.add(wrapped("Ninguna cantidad tiene rango de incertidumbre; solo se probó la severidad. Declara rangos "
                    + "(mín. y máx.) al estimar para que el análisis sea útil.", "notice-warn"));
        }
        if (r.changesOnlyJointly()) {
            VBox corners = new VBox(6);
            r.corners().stream().limit(5).forEach(c -> corners.getChildren().add(wrapped(
                    c.values().entrySet().stream().map(en -> en.getKey() + " = " + num(en.getValue()))
                            .collect(Collectors.joining("; ")) + " → " + c.riskLevel(), "hint")));
            out.add(card(wrapped("Ningún valor cambia el veredicto por sí solo, pero ciertas combinaciones extremas sí. "
                    + "«No decide» solo es cierto para variaciones aisladas.", "field-label"), corners));
        } else if (!r.corners().isEmpty()) {
            out.add(wrapped("Además hay " + r.corners().size() + " combinaciones de extremos que cambian el nivel.", "hint"));
        }
        if (!r.cornersExplored()) {
            out.add(wrapped("Hay demasiados parámetros: no se exploraron combinaciones de extremos.", "notice-warn"));
        }
        out.add(wrapped("Límite del método: no se prueban todas las combinaciones intermedias. «No decide» significa que "
                + "el nivel no cambió en lo probado, no que sea imposible.", "hint"));
        return out;
    }
}
