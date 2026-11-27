// RiskEstimator.java
package org.example.iso;

import java.util.ArrayList;
import java.util.List;

/**
 * Estimación de probabilidad según ISO 14971: P = P1 × P2.
 * P1 = probabilidad de que ocurra la situación peligrosa, por unidad de exposición.
 * P2 = probabilidad de que la situación peligrosa produzca el daño (adimensional).
 * Cada cálculo devuelve la traza de factores para poder explicar de dónde salió el número.
 * No depende de la base de datos.
 */
public final class RiskEstimator {

    private RiskEstimator() {
    }

    public record Factor(String name, double value, String unit) {}

    /**
     * @param p1               null si solo se declaró la probabilidad total
     * @param p2               null si solo se declaró la probabilidad total
     * @param exposureUnit     unidad de exposición de p1 y pTotal
     * @param justification    obligatoria cuando solo hay probabilidad total (RF-15)
     */
    public record Estimate(Double p1, Double p2, double pTotal, String exposureUnit,
                           List<Factor> trace, String justification) {}

    /**
     * P1 = tasa de falla × fracción del modo × Π(1 − cobertura de cada barrera); P = P1 × P2.
     * La tasa lleva unidad de exposición; fracción, coberturas y P2 son adimensionales en [0, 1].
     */
    public static Estimate fromFailureMode(Amount failureRate, Amount modeFraction,
                                           List<Amount> barrierCoverages, Amount p2) {
        Amount rate = requireExposure(failureRate, "La tasa de falla");
        double fraction = requireFraction(modeFraction, "La fracción del modo de falla");
        double conditional = requireFraction(p2, "P2");

        List<Factor> trace = new ArrayList<>();
        trace.add(new Factor("Tasa de falla", rate.value(), rate.unit()));
        trace.add(new Factor("Fracción del modo de falla", fraction, Amount.ADIMENSIONAL));

        double p1 = rate.value() * fraction;
        int i = 1;
        for (Amount coverage : barrierCoverages == null ? List.<Amount>of() : barrierCoverages) {
            double c = requireFraction(coverage, "La cobertura de la barrera " + i);
            trace.add(new Factor("Probabilidad de que falle la barrera " + i, 1.0 - c,
                    Amount.ADIMENSIONAL));
            p1 *= (1.0 - c);
            i++;
        }
        requireProbability(p1, "P1");
        trace.add(new Factor("P1", p1, rate.unit()));
        trace.add(new Factor("P2", conditional, Amount.ADIMENSIONAL));
        return new Estimate(p1, conditional, p1 * conditional, rate.unit(), List.copyOf(trace), null);
    }

    /** P1 dado directamente (p. ej. la probabilidad del evento superior de un FTA); P = P1 × P2. */
    public static Estimate fromP1(Amount p1, Amount p2) {
        Amount first = requireExposure(p1, "P1");
        requireProbability(first.value(), "P1");
        double conditional = requireFraction(p2, "P2");
        List<Factor> trace = List.of(
                new Factor("P1", first.value(), first.unit()),
                new Factor("P2", conditional, Amount.ADIMENSIONAL));
        return new Estimate(first.value(), conditional, first.value() * conditional,
                first.unit(), trace, null);
    }

    /** Solo probabilidad total: exige constancia escrita de por qué no se descompone en P1 y P2. */
    public static Estimate fromTotalOnly(Amount pTotal, String justification) {
        if (justification == null || justification.isBlank()) {
            throw new IllegalArgumentException(
                    "Sin P1 y P2 se debe dejar constancia de por qué no se descompone la probabilidad");
        }
        Amount total = requireExposure(pTotal, "La probabilidad total");
        requireProbability(total.value(), "La probabilidad total");
        List<Factor> trace = List.of(new Factor("Probabilidad total", total.value(), total.unit()));
        return new Estimate(null, null, total.value(), total.unit(), trace, justification.trim());
    }

    private static Amount requireExposure(Amount amount, String name) {
        if (amount == null) {
            throw new IllegalArgumentException(name + " es obligatoria");
        }
        Amount canon = amount.canonical();
        if (canon.isDimensionless()) {
            throw new IllegalArgumentException(name
                    + " debe declarar su unidad de exposición (por hora, por ciclo, por uso...)");
        }
        return canon;
    }

    private static double requireFraction(Amount amount, String name) {
        if (amount == null) {
            throw new IllegalArgumentException(name + " es obligatoria");
        }
        Amount canon = amount.canonical();
        if (!canon.isDimensionless()) {
            throw new IllegalArgumentException(name + " debe ser adimensional (valor entre 0 y 1)");
        }
        if (canon.value() > 1.0) {
            throw new IllegalArgumentException(name + " debe estar entre 0 y 1: " + canon.value());
        }
        return canon.value();
    }

    private static void requireProbability(double p, String name) {
        if (p > 1.0) {
            throw new IllegalArgumentException(name + " no puede ser mayor que 1: " + p);
        }
    }
}
