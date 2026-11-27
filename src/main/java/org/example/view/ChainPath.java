package org.example.view;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.util.List;

/** Dibujo vertical del camino de la falla: cajas con una flecha entre ellas. */
final class ChainPath {

    /** Un paso del camino: título pequeño (tipo) y texto (qué ocurre o dónde). */
    record Step(String kind, String text, boolean emphasis) {}

    private ChainPath() {
    }

    static Node build(List<Step> steps) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.TOP_CENTER);
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            VBox card = new VBox(2, Ui.label(s.kind(), "table-head"), bounded(s.text()));
            card.getStyleClass().addAll("path-box", s.emphasis() ? "path-box-end" : "path-box-mid");
            card.setMaxWidth(Double.MAX_VALUE);
            box.getChildren().add(card);
            if (i < steps.size() - 1) {
                box.getChildren().add(Ui.label("↓", "path-arrow"));
            }
        }
        return box;
    }

    private static javafx.scene.control.Label bounded(String text) {
        javafx.scene.control.Label l = Ui.label(text == null || text.isBlank() ? "—" : text, "path-text");
        l.setWrapText(true);
        l.setMaxHeight(60);                       // el texto completo está en el formulario
        l.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        return l;
    }
}
