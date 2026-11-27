// Sensitivity.java
package org.example.iso;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Análisis de sensibilidad del veredicto: ¿qué valores, dentro del rango que el usuario
 * declaró como incierto, cambian el nivel de riesgo?
 *
 * Dos pruebas:
 * 1. Uno a la vez: cada parámetro recorre su rango con los demás en su valor registrado.
 * 2. Combinaciones de extremos: todos los parámetros en su mínimo o su máximo a la vez.
 *    Detecta cambios que solo aparecen cuando varias incertidumbres coinciden.
 *
 * Límite: no explora todas las combinaciones intermedias. "No decide" significa que no cambió
 * el veredicto en las combinaciones probadas, no que sea imposible.
 * No depende de la base de datos.
 */
public final class Sensitivity {

    private static final int GRID_POINTS = 25;
    private static final int MAX_CORNER_PARAMS = 12;
    private static final int MAX_CORNER_REPORT = 20;

    private Sensitivity() {
    }

    public record Param(String name, double base, List<Double> candidates) {
        public Param {
            candidates = List.copyOf(candidates);
        }
    }

    public record Outcome(String riskLevel, boolean acceptable) {}

    public record Change(double value, Outcome outcome) {}

    public record ParamResult(String name, boolean decides, List<Change> changes) {}

    public record Corner(Map<String, Double> values, Outcome outcome) {}

    public record Report(Outcome baseline, List<ParamResult> results,
                         List<Corner> cornerChanges, boolean cornersExplored) {

        /** Nombres de los parámetros que, solos, cambian el veredicto. */
        public List<String> decisive() {
            return results.stream().filter(ParamResult::decides).map(ParamResult::name).toList();
        }

        public boolean changesOnlyJointly() {
            return decisive().isEmpty() && !cornerChanges.isEmpty();
        }
    }

    /** Parámetro continuo: recorre [min, max] con una malla (logarítmica si el rango abarca ≥ 2 décadas). */
    public static Param range(String name, double base, double min, double max) {
        if (!(min <= base && base <= max)) {
            throw new IllegalArgumentException("El valor registrado de '" + name
                    + "' debe estar dentro de su rango [" + min + ", " + max + "]");
        }
        boolean log = min > 0 && max / min >= 100;
        TreeSet<Double> values = new TreeSet<>();
        for (int i = 0; i < GRID_POINTS; i++) {
            double t = i / (GRID_POINTS - 1.0);
            values.add(log ? min * Math.pow(max / min, t) : min + (max - min) * t);
        }
        values.add(min);
        values.add(max);
        values.add(base);
        return new Param(name, base, new ArrayList<>(values));
    }

    /** Parámetro discreto (p. ej. niveles de severidad). */
    public static Param discrete(String name, double base, List<Double> values) {
        TreeSet<Double> set = new TreeSet<>(values);
        set.add(base);
        return new Param(name, base, new ArrayList<>(set));
    }

    public static Report analyze(List<Param> params, Function<Map<String, Double>, Outcome> model) {
        Map<String, Double> baseValues = new LinkedHashMap<>();
        for (Param p : params) {
            baseValues.put(p.name(), p.base());
        }
        Outcome baseline = model.apply(baseValues);

        List<ParamResult> results = new ArrayList<>();
        for (Param p : params) {
            List<Change> changes = new ArrayList<>();
            for (double candidate : p.candidates()) {
                Map<String, Double> trial = new LinkedHashMap<>(baseValues);
                trial.put(p.name(), candidate);
                Outcome outcome = model.apply(trial);
                if (!outcome.riskLevel().equals(baseline.riskLevel())) {
                    changes.add(new Change(candidate, outcome));
                }
            }
            results.add(new ParamResult(p.name(), !changes.isEmpty(), List.copyOf(changes)));
        }

        List<Corner> corners = new ArrayList<>();
        boolean explored = params.size() <= MAX_CORNER_PARAMS;
        if (explored) {
            int k = params.size();
            for (int mask = 0; mask < (1 << k); mask++) {
                Map<String, Double> trial = new LinkedHashMap<>();
                for (int i = 0; i < k; i++) {
                    List<Double> c = params.get(i).candidates();
                    trial.put(params.get(i).name(),
                            (mask & (1 << i)) == 0 ? c.get(0) : c.get(c.size() - 1));
                }
                Outcome outcome = model.apply(trial);
                if (!outcome.riskLevel().equals(baseline.riskLevel()) && corners.size() < MAX_CORNER_REPORT) {
                    corners.add(new Corner(trial, outcome));
                }
            }
        }
        return new Report(baseline, List.copyOf(results), List.copyOf(corners), explored);
    }
}
