package org.example.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.example.api.DeviceDto;
import org.example.client.ApiClient;

import java.util.List;
import java.util.Objects;

/** Pantalla 1 · Inicio: acciones principales y lista de dispositivos registrados. */
public class HomeView extends BorderPane {

    private static final Color TEAL = Color.web("#15616D");

    private final ApiClient api;
    private final Navigator nav;

    private final Label count = Ui.label("", "hint");
    private final VBox listBody = new VBox();
    private final VBox errorNotice = new VBox(6);
    private final Label errorText = new Label();

    public HomeView(ApiClient api, Navigator nav) {
        this.api = api;
        this.nav = nav;

        setTop(buildTopBar());
        ScrollPane scroll = new ScrollPane(buildContent());
        scroll.setFitToWidth(true);
        setCenter(scroll);

        refresh();
    }

    private Node buildTopBar() {
        Region mark = new Region();
        mark.getStyleClass().add("logo-mark");
        HBox left = new HBox(10, mark, Ui.label("MBR", "app-name"),
                Ui.label("Gestión de riesgos de dispositivos médicos", "app-subtitle"));
        left.setAlignment(Pos.CENTER_LEFT);
        Label right = Ui.label("v" + Ui.VERSION + " · Universidad Tecnológica de Panamá", "version-label");
        return Ui.topBar(left, right);
    }

    private Node buildContent() {
        VBox title = new VBox(6,
                Ui.label("Gestión de riesgos basada en modelos", "page-title"),
                Ui.label("Crea un dispositivo nuevo o abre uno registrado para continuar su análisis.", "page-lead"));

        GridPane tiles = new GridPane();
        tiles.setHgap(16);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(100.0 / 3);
            tiles.getColumnConstraints().add(c);
        }
        Button create = tile("Crear dispositivo", "Registrar un dispositivo médico nuevo",
                Ui.icon(Ui.ICON_PLUS, Color.WHITE), true, nav::showCreateDevice);
        Button criteria = tile("Configurar criterios", "Escalas, matriz y aceptabilidad",
                Ui.icon(Ui.ICON_GRID, TEAL), false, null);
        Button reports = tile("Reportes", "Expediente de riesgos en PDF",
                Ui.icon(Ui.ICON_DOC, TEAL), false, null);
        tiles.add(create, 0, 0);
        tiles.add(criteria, 1, 0);
        tiles.add(reports, 2, 0);

        errorNotice.getStyleClass().add("notice-error");
        errorNotice.getChildren().addAll(Ui.label("No se pudo cargar la lista de dispositivos", "notice-title"),
                errorText, Ui.button("Reintentar", "btn-secondary", this::refresh));
        errorText.setWrapText(true);
        Ui.show(errorNotice, false);

        VBox list = new VBox(12, buildListHeader(), buildListCard());

        VBox content = new VBox(28, title, tiles, errorNotice, list);
        content.setPadding(new Insets(40, 64, 40, 64));
        return content;
    }

    private Node buildListHeader() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(Ui.label("Dispositivos registrados", "section-title"), spacer, count);
        header.setAlignment(Pos.BASELINE_LEFT);
        return header;
    }

    private Node buildListCard() {
        GridPane head = grid();
        head.getStyleClass().add("card-head");
        head.add(Ui.label("DISPOSITIVO", "table-head"), 0, 0);
        head.add(Ui.label("FINALIDAD PREVISTA", "table-head"), 1, 0);

        VBox card = new VBox(head, listBody);
        card.getStyleClass().add("card");
        return card;
    }

    private static GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(16);
        double[] widths = {32, 48, 20};
        for (double w : widths) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(w);
            g.getColumnConstraints().add(c);
        }
        return g;
    }

    /** Mosaico de acción. Sin acción queda deshabilitado y lo dice, en vez de parecer roto. */
    private Button tile(String title, String text, Node icon, boolean primary, Runnable action) {
        Label description = Ui.label(text, "tile-text");
        description.setWrapText(true);
        VBox texts = new VBox(4, Ui.label(title, "tile-title"), description);
        VBox box = new VBox(10, icon, texts);
        if (action == null) {
            box.getChildren().add(Ui.label("Próximamente", "pill-soon"));
        }
        box.setAlignment(Pos.TOP_LEFT);

        Button button = new Button();
        button.setGraphic(box);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setAlignment(Pos.TOP_LEFT);
        button.getStyleClass().add("tile");
        if (primary) {
            button.getStyleClass().add("tile-primary");
        }
        button.setMaxWidth(Double.MAX_VALUE);
        if (action == null) {
            button.setDisable(true);
        } else {
            button.setOnAction(e -> action.run());
        }
        return button;
    }

    private void refresh() {
        Ui.show(errorNotice, false);
        count.setText("Cargando…");
        Async.run(api::listDevices, this::showDevices, message -> {
            count.setText("");
            errorText.setText(message);
            Ui.show(errorNotice, true);
        });
    }

    private void showDevices(List<DeviceDto> devices) {
        listBody.getChildren().clear();
        count.setText(devices.size() == 1 ? "1 registrado" : devices.size() + " registrados");

        if (devices.isEmpty()) {
            VBox empty = Ui.emptyState(Ui.ICON_INBOX,
                    "Todavía no hay dispositivos",
                    "Usa «Crear dispositivo» para registrar el primer dispositivo médico.");
            listBody.getChildren().add(empty);
            return;
        }
        for (int i = 0; i < devices.size(); i++) {
            boolean last = i == devices.size() - 1;
            listBody.getChildren().add(row(devices.get(i), last));
        }
    }

    private Node row(DeviceDto device, boolean last) {
        GridPane row = grid();
        row.getStyleClass().add(last ? "card-row-last" : "card-row");
        row.setAlignment(Pos.CENTER_LEFT);

        Label name = Ui.label(device.name(), "row-title");
        Label use = Ui.label(Objects.toString(device.intendedUse(), ""), "muted");
        use.setMaxWidth(Double.MAX_VALUE);
        use.setWrapText(false);

        Button open = Ui.button("Abrir", "btn-link", () -> nav.showWorkspace(device));
        Button deactivate = Ui.button("Desactivar", "btn-link", () -> deactivate(device));
        deactivate.getStyleClass().add("btn-danger-link");
        HBox actions = new HBox(4, open, deactivate);
        actions.setAlignment(Pos.CENTER_RIGHT);

        row.add(name, 0, 0);
        row.add(use, 1, 0);
        row.add(actions, 2, 0);
        return row;
    }

    private void deactivate(DeviceDto device) {
        boolean confirmed = Ui.confirm("«" + device.name() + "» dejará de aparecer en la lista. "
                + "Sus datos no se borran.", "Desactivar");
        if (!confirmed) {
            return;
        }
        Async.run(() -> {
            api.deactivateDevice(device.id());
            return Boolean.TRUE;
        }, ok -> refresh(), message -> {
            errorText.setText(message);
            Ui.show(errorNotice, true);
        });
    }
}
