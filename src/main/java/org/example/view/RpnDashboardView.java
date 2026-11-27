package org.example.view;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.api.DashboardDtos.CategoryCountDto;
import org.example.api.DashboardDtos.DashboardDto;
import org.example.api.DashboardDtos.RpnRowDto;
import org.example.api.DeviceDto;
import org.example.client.ApiClient;

import java.util.Objects;
import java.util.function.Function;

/**
 * Dashboard de RPN del dispositivo: cuántos modos de fallo hay en cada categoría de prioridad, los
 * indicadores principales y la tabla ordenada por RPN. El RPN solo prioriza; la aceptabilidad del
 * riesgo la decide la evaluación ISO 14971, que se muestra en su propia columna.
 */
public class RpnDashboardView extends VBox {

    private final ApiClient api;
    private final DeviceDto device;
    private final VBox body = new VBox(14);

    public RpnDashboardView(ApiClient api, DeviceDto device) {
        this.api = api;
        this.device = device;
        setPadding(new Insets(20, 28, 20, 28));
        setSpacing(10);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox head = new HBox(10, Ui.label("Dashboard de RPN", "form-title-sm"), spacer,
                Ui.button("Actualizar", "btn-secondary", this::load));
        head.setAlignment(Pos.CENTER_LEFT);
        Label hint = Ui.label("El RPN (severidad × ocurrencia × detección) ordena los modos de fallo por "
                + "prioridad de atención. No decide si un riesgo es aceptable: eso lo decide la evaluación ISO 14971.",
                "hint");
        hint.setWrapText(true);

        getChildren().addAll(head, hint, body);
        VBox.setVgrow(body, Priority.ALWAYS);
        load();
    }

    private void load() {
        body.getChildren().setAll(Ui.label("Cargando…", "hint"));
        long id = device.id();
        Async.run(() -> api.rpnDashboard(id), this::render, msg -> {
            Label error = Ui.label("No se pudo cargar el dashboard: " + msg, "field-error");
            error.setWrapText(true);
            body.getChildren().setAll(error, Ui.button("Reintentar", "btn-secondary", this::load));
        });
    }

    private void render(DashboardDto d) {
        if (d.rows().isEmpty()) {
            VBox empty = Ui.emptyState(Ui.ICON_GRID,
                    "Aún no hay análisis FMEA",
                    "Los análisis FMEA se crean desde un daño en la sección Riesgos, especificando su modo de fallo y los factores S, O y D.");
            body.getChildren().setAll(empty);
            return;
        }
        TableView<RpnRowDto> table = buildTable(d);
        VBox.setVgrow(table, Priority.ALWAYS);
        body.getChildren().setAll(kpis(d), categoryBars(d), table);
    }

    private Node kpis(DashboardDto d) {
        HBox row = new HBox(12,
                kpi("Modos de fallo", String.valueOf(d.rows().size())),
                kpi("Mayor RPN", String.valueOf(d.maxRpn())),
                kpi("RPN promedio", String.valueOf(d.averageRpn())));
        if (d.uncategorized() > 0) {
            row.getChildren().add(kpi("Sin categoría", String.valueOf(d.uncategorized())));
        }
        return row;
    }

    private static VBox kpi(String title, String value) {
        VBox box = new VBox(2, Ui.label(title, "table-head"), Ui.label(value, "kpi-value"));
        box.getStyleClass().add("card");
        box.setPadding(new Insets(10, 16, 10, 16));
        box.setMinWidth(150);
        return box;
    }

    /** Una barra por categoría de prioridad; el largo es proporcional a la categoría con más casos. */
    private Node categoryBars(DashboardDto d) {
        int max = d.categories().stream().mapToInt(CategoryCountDto::count).max().orElse(0);
        VBox box = new VBox(6, Ui.label("Modos de fallo por categoría de prioridad", "table-head"));
        if (d.categories().isEmpty()) {
            box.getChildren().add(Ui.label("No hay categorías de prioridad configuradas.", "hint"));
        }
        for (CategoryCountDto c : d.categories()) {
            Label name = Ui.label(c.name() + "  (" + c.minRpn() + "–" + c.maxRpn() + ")");
            name.setMinWidth(220);
            double share = max == 0 ? 0 : (double) c.count() / max;
            StackPane track = new StackPane();
            track.getStyleClass().add("bar-track");
            track.setAlignment(Pos.CENTER_LEFT);
            track.setMinHeight(10);
            track.setMaxHeight(10);
            Region fill = new Region();
            fill.getStyleClass().add("bar-fill");
            fill.setMaxHeight(10);
            fill.prefWidthProperty().bind(track.widthProperty().multiply(share));
            fill.maxWidthProperty().bind(track.widthProperty().multiply(share));
            track.getChildren().add(fill);
            HBox.setHgrow(track, Priority.ALWAYS);
            Label count = Ui.label(String.valueOf(c.count()), "row-title");
            count.setMinWidth(30);
            HBox line = new HBox(10, name, track, count);
            line.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(line);
        }
        box.getStyleClass().add("card");
        box.setPadding(new Insets(12, 16, 12, 16));
        return box;
    }

    private TableView<RpnRowDto> buildTable(DashboardDto d) {
        TableView<RpnRowDto> table = new TableView<>();
        table.getColumns().add(column("RPN", 60, r -> String.valueOf(r.rpn())));
        table.getColumns().add(column("Categoría", 130, r -> Objects.toString(r.category(), "—")));
        table.getColumns().add(column("Modo de fallo", 220, RpnRowDto::failureMode));
        table.getColumns().add(column("Componente", 140, r -> Objects.toString(r.component(), "—")));
        table.getColumns().add(column("Daño", 220, RpnRowDto::harm));
        table.getColumns().add(column("S", 40, r -> String.valueOf(r.severity())));
        table.getColumns().add(column("O", 40, r -> String.valueOf(r.occurrence())));
        table.getColumns().add(column("D", 40, r -> String.valueOf(r.detection())));
        table.getColumns().add(column("Evaluación ISO 14971", 190, RpnDashboardView::iso));
        table.getItems().setAll(d.rows());
        return table;
    }

    private static String iso(RpnRowDto r) {
        if (r.isoRiskLevel() == null) {
            return "Sin estimar";
        }
        return r.isoRiskLevel() + (Boolean.TRUE.equals(r.isoAcceptable()) ? " · aceptable" : " · no aceptable");
    }

    private static TableColumn<RpnRowDto, String> column(String title, double width,
                                                         Function<RpnRowDto, String> value) {
        TableColumn<RpnRowDto, String> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> new ReadOnlyStringWrapper(Objects.toString(value.apply(c.getValue()), "")));
        col.setPrefWidth(width);
        return col;
    }
}
