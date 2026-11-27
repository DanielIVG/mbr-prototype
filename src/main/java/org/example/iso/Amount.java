// Amount.java
package org.example.iso;

import java.util.Locale;
import java.util.Set;

/**
 * Un valor numérico con su unidad de exposición (o "adimensional").
 * Toda probabilidad, tasa, fracción o cobertura del cálculo ISO 14971 se maneja con esta
 * unidad para poder rechazar mezclas incompatibles (por hora vs. por ciclo, etc.).
 * No depende de la base de datos.
 */
public record Amount(double value, String unit) {

    public static final String ADIMENSIONAL = "adimensional";

    private static final Set<String> DIMENSIONLESS =
            Set.of("adimensional", "1", "-", "fraccion", "fracción", "razon", "razón");

    public Amount {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(
                    "El valor debe ser un número finito mayor o igual a 0: " + value);
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException(
                    "La unidad es obligatoria (use 'adimensional' si no tiene unidad)");
        }
        unit = unit.trim();
    }

    public static Amount dimensionless(double value) {
        return new Amount(value, ADIMENSIONAL);
    }

    public static Amount perHour(double value) {
        return new Amount(value, "por hora");
    }

    /**
     * Forma canónica: "1/h", "/h", "por hora" → "por hora"; FIT (fallas por 10⁹ h) se convierte
     * a "por hora". Unidades sin patrón reconocible se rechazan para no aceptar texto libre.
     */
    public Amount canonical() {
        String u = unit.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (DIMENSIONLESS.contains(u)) {
            return new Amount(value, ADIMENSIONAL);
        }
        if (u.equals("fit")) {
            return new Amount(value * 1e-9, "por hora");
        }
        String base;
        if (u.startsWith("1/")) {
            base = u.substring(2);
        } else if (u.startsWith("/")) {
            base = u.substring(1);
        } else if (u.startsWith("por ")) {
            base = u.substring(4);
        } else {
            throw new IllegalArgumentException("Unidad no reconocida: '" + unit
                    + "'. Use 'adimensional', 'por hora', 'por ciclo', 'por uso' o 'FIT'");
        }
        base = base.trim();
        if (base.isEmpty()) {
            throw new IllegalArgumentException("Unidad no reconocida: '" + unit + "'");
        }
        base = switch (base) {
            case "h", "hr", "hora", "horas" -> "hora";
            case "ciclos" -> "ciclo";
            case "usos" -> "uso";
            default -> base;
        };
        return new Amount(value, "por " + base);
    }

    public boolean isDimensionless() {
        return ADIMENSIONAL.equals(canonical().unit());
    }

    /** Valor ya convertido a la unidad canónica. */
    public double canonicalValue() {
        return canonical().value();
    }
}
