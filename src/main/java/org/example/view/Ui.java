package org.example.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/** Piezas pequeñas compartidas por las pantallas. Los estilos están en css/mbr.css. */
final class Ui {

    static final String VERSION = "0.1.0";

    // Iconos existentes
    static final String ICON_PLUS = "M12 5v14 M5 12h14";
    static final String ICON_GRID = "M3 3h18v18H3z M9 3v18 M3 9h18";
    static final String ICON_DOC = "M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z M14 3v5h5";
    static final String ICON_BLOCKS = "M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M17.5 14v7 M14 17.5h7";

    // Iconos nuevos para estados vacios y acciones
    static final String ICON_INBOX = "M22 12h-6l-2 3H10l-2-3H2 M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z";
    static final String ICON_ALERT_TRIANGLE = "M12 9v4 M12 17h.01 M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z";
    static final String ICON_CHECK_CIRCLE = "M22 11.08V12a10 10 0 1 1-5.93-9.14 M22 4L12 14.01l-3-3";
    static final String ICON_X_CIRCLE = "M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z M15 9l-6 6 M9 9l6 6";
    static final String ICON_EDIT = "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7 M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z";
    static final String ICON_TRASH = "M3 6h18 M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2";
    static final String ICON_LINK = "M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71 M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71";
    static final String ICON_CLIPBOARD = "M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2 M15 2H9a1 1 0 0 0-1 1v2a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V3a1 1 0 0 0-1-1z";
    static final String ICON_LAYERS = "M12 2L2 7l10 5 10-5-10-5z M2 17l10 5 10-5 M2 12l10 5 10-5";
    static final String ICON_SEARCH = "M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z M21 21l-4.35-4.35";

    private Ui() {
    }

    static Label label(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }

    static Button button(String text, String styleClass, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setOnAction(e -> action.run());
        return button;
    }

    /** Barra superior: contenido a la izquierda y a la derecha. */
    static HBox topBar(Node left, Node right) {
        HBox bar = new HBox();
        bar.getStyleClass().add("topbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bar.getChildren().addAll(left, spacer, right);
        return bar;
    }

    static SVGPath icon(String content, Color stroke) {
        SVGPath path = new SVGPath();
        path.setContent(content);
        path.setFill(Color.TRANSPARENT);
        path.setStroke(stroke);
        path.setStrokeWidth(2);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return path;
    }

    /** Icono grande para estados vacios (64x64px). */
    static Node emptyIcon(String content, Color stroke) {
        SVGPath path = icon(content, stroke);
        path.setScaleX(2.5);
        path.setScaleY(2.5);
        HBox box = new HBox(path);
        box.getStyleClass().add("empty-icon-box");
        box.setAlignment(Pos.CENTER);
        box.setMinSize(64, 64);
        box.setPrefSize(64, 64);
        box.setMaxSize(64, 64);
        return box;
    }

    /** Estado vacio con icono, titulo y mensaje explicativo. */
    static VBox emptyState(String iconPath, String title, String message) {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(32));

        Node icon = emptyIcon(iconPath, Color.web("#15616D"));
        Label titleLabel = label(title, "empty-title");
        Label messageLabel = label(message, "hint");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(400);
        messageLabel.setAlignment(Pos.CENTER);

        box.getChildren().addAll(icon, titleLabel, messageLabel);
        return box;
    }

    /** Label con texto que hace wrap automaticamente. */
    static Label wrappedLabel(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        label.setWrapText(true);
        return label;
    }

    static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    /** Confirmación para acciones que descartan o desactivan algo. */
    static boolean confirm(String message, String confirmText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ButtonType yes = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(yes, no);
        return alert.showAndWait().filter(b -> b == yes).isPresent();
    }
}
