// RiskMatrix.java
package org.example.iso;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Matriz de riesgo severidad × probabilidad con criterios de aceptabilidad.
 * Convierte una probabilidad numérica en un nivel de la escala y busca la celda correspondiente.
 * Las bandas de probabilidad son [mínimo, máximo); la última incluye 1.
 * No depende de la base de datos.
 */
public final class RiskMatrix {

    public record SeverityLevel(int value, String descriptor) {}

    public record ProbabilityLevel(int value, String descriptor, double min, double max) {}

    public record Cell(int severity, int probability, String riskLevel) {}

    public record Criterion(String riskLevel, boolean acceptable) {}

    public record Verdict(SeverityLevel severity, ProbabilityLevel probability,
                          String riskLevel, boolean acceptable) {}

    private final String exposureUnit;
    private final Map<Integer, SeverityLevel> severities = new HashMap<>();
    private final List<ProbabilityLevel> probabilities;
    private final Map<String, Cell> cells = new HashMap<>();
    private final Map<String, Boolean> criteria = new HashMap<>();

    public RiskMatrix(String exposureUnit, List<SeverityLevel> severityLevels,
                      List<ProbabilityLevel> probabilityLevels, List<Cell> cellList,
                      List<Criterion> criterionList) {
        this.exposureUnit = new Amount(0, exposureUnit).canonical().unit();
        if (Amount.ADIMENSIONAL.equals(this.exposureUnit)) {
            throw new IllegalArgumentException(
                    "La escala de probabilidad debe declarar su unidad de exposición");
        }
        if (severityLevels.isEmpty() || probabilityLevels.isEmpty()) {
            throw new IllegalArgumentException("Las escalas de severidad y probabilidad no pueden estar vacías");
        }
        for (SeverityLevel s : severityLevels) {
            if (severities.put(s.value(), s) != null) {
                throw new IllegalArgumentException("Nivel de severidad duplicado: " + s.value());
            }
        }
        this.probabilities = validateBands(probabilityLevels);

        for (Cell c : cellList) {
            if (!severities.containsKey(c.severity())) {
                throw new IllegalArgumentException("La celda usa una severidad inexistente: " + c.severity());
            }
            if (probabilities.stream().noneMatch(p -> p.value() == c.probability())) {
                throw new IllegalArgumentException("La celda usa una probabilidad inexistente: " + c.probability());
            }
            if (cells.put(key(c.severity(), c.probability()), c) != null) {
                throw new IllegalArgumentException("Celda duplicada: severidad " + c.severity()
                        + ", probabilidad " + c.probability());
            }
        }
        for (SeverityLevel s : severityLevels) {
            for (ProbabilityLevel p : probabilities) {
                if (!cells.containsKey(key(s.value(), p.value()))) {
                    throw new IllegalArgumentException("Falta la celda de severidad " + s.value()
                            + " y probabilidad " + p.value());
                }
            }
        }
        for (Criterion c : criterionList) {
            if (criteria.put(c.riskLevel(), c.acceptable()) != null) {
                throw new IllegalArgumentException("Criterio duplicado para el nivel: " + c.riskLevel());
            }
        }
        Set<String> levelsUsed = new HashSet<>();
        cells.values().forEach(c -> levelsUsed.add(c.riskLevel()));
        for (String level : levelsUsed) {
            if (!criteria.containsKey(level)) {
                throw new IllegalArgumentException(
                        "El nivel de riesgo '" + level + "' no tiene criterio de aceptabilidad");
            }
        }
    }

    /** Niveles de severidad de la escala, en orden ascendente. */
    public List<Integer> severityValues() {
        return severities.keySet().stream().sorted().toList();
    }

    public String exposureUnit() {
        return exposureUnit;
    }

    public ProbabilityLevel levelOf(double p) {
        if (!(p >= 0.0 && p <= 1.0)) {
            throw new IllegalArgumentException("La probabilidad debe estar entre 0 y 1: " + p);
        }
        for (int i = 0; i < probabilities.size(); i++) {
            ProbabilityLevel level = probabilities.get(i);
            boolean last = i == probabilities.size() - 1;
            if (p >= level.min() && (p < level.max() || (last && p <= level.max()))) {
                return level;
            }
        }
        throw new IllegalStateException("La probabilidad no cae en ninguna banda: " + p);
    }

    public Verdict evaluate(int severity, double probability, String unit) {
        String canon = new Amount(0, unit).canonical().unit();
        if (!canon.equals(exposureUnit)) {
            throw new IllegalArgumentException("La probabilidad está en '" + canon
                    + "' pero la escala está definida '" + exposureUnit + "'");
        }
        SeverityLevel s = severities.get(severity);
        if (s == null) {
            throw new IllegalArgumentException("La severidad " + severity + " no existe en la escala");
        }
        ProbabilityLevel p = levelOf(probability);
        Cell cell = cells.get(key(severity, p.value()));
        return new Verdict(s, p, cell.riskLevel(), criteria.get(cell.riskLevel()));
    }

    private static List<ProbabilityLevel> validateBands(List<ProbabilityLevel> input) {
        List<ProbabilityLevel> sorted = new ArrayList<>(input);
        sorted.sort(Comparator.comparingDouble(ProbabilityLevel::min));
        for (ProbabilityLevel p : sorted) {
            if (!(p.min() >= 0.0 && p.max() <= 1.0 && p.min() < p.max())) {
                throw new IllegalArgumentException("Banda inválida en el nivel " + p.value()
                        + ": debe cumplirse 0 ≤ mínimo < máximo ≤ 1");
            }
        }
        if (sorted.get(0).min() != 0.0) {
            throw new IllegalArgumentException("La primera banda debe comenzar en 0");
        }
        if (sorted.get(sorted.size() - 1).max() != 1.0) {
            throw new IllegalArgumentException("La última banda debe terminar en 1");
        }
        Set<Integer> values = new HashSet<>();
        for (int i = 0; i < sorted.size(); i++) {
            if (!values.add(sorted.get(i).value())) {
                throw new IllegalArgumentException("Nivel de probabilidad duplicado: " + sorted.get(i).value());
            }
            if (i > 0) {
                double prevMax = sorted.get(i - 1).max();
                double min = sorted.get(i).min();
                if (min < prevMax) {
                    throw new IllegalArgumentException("Las bandas de los niveles "
                            + sorted.get(i - 1).value() + " y " + sorted.get(i).value() + " se traslapan");
                }
                if (min > prevMax) {
                    throw new IllegalArgumentException("Hay un hueco entre las bandas de los niveles "
                            + sorted.get(i - 1).value() + " y " + sorted.get(i).value());
                }
            }
        }
        return List.copyOf(sorted);
    }

    private static String key(int severity, int probability) {
        return severity + ":" + probability;
    }
}
