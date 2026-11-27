package org.example.view;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.api.AssessmentDtos.EvidenceLevelDto;
import org.example.api.AssessmentDtos.QuantityDto;

import java.util.List;

/** Un valor con unidad, rango opcional y procedencia obligatoria (fuente, evidencia, autor). */
final class QuantityForm {

    private record Opt(String code, String text) {
        @Override
        public String toString() {
            return text;
        }
    }

    private final String role;
    private final String title;
    private final TextField value = new TextField();
    private final TextField unit = new TextField();
    private final TextField min = new TextField();
    private final TextField max = new TextField();
    private final TextField source = new TextField();
    private final ComboBox<Opt> evidenceBox = new ComboBox<>();
    private final VBox box;

    /**
     * @param unitEditable false → la unidad queda fija en {@code defaultUnit} (p. ej. «adimensional»)
     */
    QuantityForm(String role, String title, String defaultUnit, boolean unitEditable, List<EvidenceLevelDto> evidence) {
        this.role = role;
        this.title = title;
        unit.setText(defaultUnit);
        unit.setDisable(!unitEditable);
        unit.setPrefWidth(120);
        value.setPromptText("valor (p. ej. 1e-6)");
        min.setPromptText("mín. (opcional)");
        max.setPromptText("máx. (opcional)");
        source.setPromptText("Fuente (obligatoria): norma, hoja de datos, ensayo, criterio del experto…");
        for (EvidenceLevelDto ev : evidence) {
            evidenceBox.getItems().add(new Opt(ev.code(), ev.code() + " — " + ev.descriptor()));
        }
        evidenceBox.setValue(evidenceBox.getItems().isEmpty() ? null : evidenceBox.getItems().get(0));
        evidenceBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(source, Priority.ALWAYS);
        HBox line1 = new HBox(8, value, unit, min, max);
        line1.setAlignment(Pos.CENTER_LEFT);
        box = new VBox(6, Ui.label(title, "field-label"), line1, source, evidenceBox);
    }

    Node node() {
        return box;
    }

    /** Rellena el formulario con un valor ya guardado. */
    void fill(QuantityDto q) {
        value.setText(Double.toString(q.value()));
        unit.setText(q.unit());
        min.setText(q.min() == null ? "" : Double.toString(q.min()));
        max.setText(q.max() == null ? "" : Double.toString(q.max()));
        source.setText(q.source());
        evidenceBox.getItems().stream().filter(o -> o.code().equals(q.evidence())).findFirst()
                .ifPresent(evidenceBox::setValue);
    }

    QuantityDto toDto(String author) {
        if (value.getText().isBlank()) {
            throw new IllegalArgumentException("Falta el valor de «" + title + "».");
        }
        if (source.getText().isBlank()) {
            throw new IllegalArgumentException("«" + title + "» necesita su fuente.");
        }
        if (evidenceBox.getValue() == null) {
            throw new IllegalArgumentException("«" + title + "» necesita su nivel de evidencia.");
        }
        Double lo = min.getText().isBlank() ? null : parse(min.getText());
        Double hi = max.getText().isBlank() ? null : parse(max.getText());
        if ((lo == null) != (hi == null)) {
            throw new IllegalArgumentException("El rango de «" + title + "» necesita mínimo y máximo.");
        }
        return new QuantityDto(role, parse(value.getText()), unit.getText().trim(), lo, hi,
                source.getText().trim(), evidenceBox.getValue().code(), author);
    }

    static double parse(String text) {
        try {
            return Double.parseDouble(text.trim().replace(',', '.'));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("'" + text + "' no es un número válido.");
        }
    }
}
