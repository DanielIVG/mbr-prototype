// RiskAssessor.java
package org.example.iso;

import org.example.iso.RiskEstimator.Estimate;
import org.example.iso.RiskMatrix.Verdict;
import org.example.iso.Sensitivity.Outcome;
import org.example.iso.Sensitivity.Param;
import org.example.iso.Sensitivity.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Une la estimación (P = P1 × P2) con la matriz de riesgo: dado el modo de estimación, las
 * cantidades con su incertidumbre y la severidad, produce el veredicto y el análisis de
 * sensibilidad. No depende de la base de datos.
 */
public final class RiskAssessor {

    public enum Mode { P1_P2, FAILURE_MODE, TOTAL_ONLY }

    /** Roles de las cantidades. */
    public static final String P1 = "P1";
    public static final String P2 = "P2";
    public static final String RATE = "RATE";
    public static final String FRACTION = "FRACTION";
    public static final String BARRIER = "BARRIER";
    public static final String TOTAL = "TOTAL";

    /** Cantidad con rango de incertidumbre opcional (min y max en la misma unidad que el valor). */
    public record Q(String role, Amount amount, Double min, Double max) {
        public boolean hasRange() {
            return min != null && max != null;
        }
    }

    public record Input(Mode mode, int severity, List<Q> quantities, String justification) {
        public Input {
            quantities = List.copyOf(quantities);
        }
    }

    public record Assessment(Estimate estimate, Verdict verdict) {}

    private RiskAssessor() {
    }

    public static Assessment assess(RiskMatrix matrix, Input in) {
        Estimate estimate = estimate(in);
        if (!estimate.exposureUnit().equals(matrix.exposureUnit())) {
            throw new IllegalArgumentException("La estimación está en '" + estimate.exposureUnit()
                    + "' pero la escala de probabilidad está definida '" + matrix.exposureUnit() + "'");
        }
        Verdict verdict = matrix.evaluate(in.severity(), estimate.pTotal(), estimate.exposureUnit());
        return new Assessment(estimate, verdict);
    }

    static Estimate estimate(Input in) {
        return switch (in.mode()) {
            case P1_P2 -> RiskEstimator.fromP1(one(in, P1), one(in, P2));
            case FAILURE_MODE -> RiskEstimator.fromFailureMode(one(in, RATE), one(in, FRACTION),
                    all(in, BARRIER), one(in, P2));
            case TOTAL_ONLY -> RiskEstimator.fromTotalOnly(one(in, TOTAL), in.justification());
        };
    }

    private static Amount one(Input in, String role) {
        List<Amount> found = all(in, role);
        if (found.size() != 1) {
            throw new IllegalArgumentException("Se esperaba exactamente un valor para " + label(role, 1)
                    + " y hay " + found.size());
        }
        return found.get(0);
    }

    private static List<Amount> all(Input in, String role) {
        return in.quantities().stream().filter(q -> q.role().equals(role)).map(Q::amount).toList();
    }

    public static String label(String role, int index) {
        return switch (role) {
            case RATE -> "Tasa de falla";
            case FRACTION -> "Fracción del modo de falla";
            case BARRIER -> "Barrera " + index;
            case TOTAL -> "Probabilidad total";
            default -> role;
        };
    }

    /**
     * Sensibilidad del nivel de riesgo: cada cantidad con rango y la severidad (todos sus niveles).
     * "Uno a la vez" más las combinaciones de extremos (ver {@link Sensitivity}).
     */
    public static Report sensitivity(RiskMatrix matrix, Input in) {
        Assessment base = assess(matrix, in);
        List<Param> params = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();      // posición de cada parámetro en quantities
        int barrierIndex = 0;
        List<String> names = new ArrayList<>();
        for (int i = 0; i < in.quantities().size(); i++) {
            Q q = in.quantities().get(i);
            if (q.role().equals(BARRIER)) {
                barrierIndex++;
            }
            if (!q.hasRange()) {
                continue;
            }
            double factor = new Amount(1, q.amount().unit()).canonical().value();
            double baseValue = q.amount().value();
            if (!(q.min() <= baseValue && baseValue <= q.max())) {
                throw new IllegalArgumentException("El valor de '" + label(q.role(), barrierIndex)
                        + "' está fuera de su rango");
            }
            String name = label(q.role(), barrierIndex);
            params.add(Sensitivity.range(name, baseValue * factor, q.min() * factor, q.max() * factor));
            indexes.add(i);
            names.add(name);
        }
        List<Double> sev = matrix.severityValues().stream().map(Integer::doubleValue).toList();
        params.add(Sensitivity.discrete("Severidad", in.severity(), sev));

        return Sensitivity.analyze(params, values -> {
            List<Q> trial = new ArrayList<>(in.quantities());
            for (int k = 0; k < indexes.size(); k++) {
                Q q = trial.get(indexes.get(k));
                double factor = new Amount(1, q.amount().unit()).canonical().value();
                double v = values.get(names.get(k)) / factor;
                trial.set(indexes.get(k), new Q(q.role(), new Amount(v, q.amount().unit()), q.min(), q.max()));
            }
            int severity = (int) Math.round(values.get("Severidad"));
            Verdict v = assess(matrix, new Input(in.mode(), severity, trial, in.justification())).verdict();
            return new Outcome(v.riskLevel(), v.acceptable());
        });
    }

    /** Resumen legible para la interfaz. */
    public static String describe(Map<String, Double> values) {
        StringBuilder sb = new StringBuilder();
        values.forEach((k, v) -> sb.append(sb.length() == 0 ? "" : "; ").append(k).append(" = ")
                .append(String.format("%.3g", v)));
        return sb.toString();
    }
}
