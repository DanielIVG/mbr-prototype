// RpnCalculator.java
package org.example.rpn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Calcula RPN = S × O × D (RF-22) y lo clasifica en las categorías definidas (RF-25).
 * No depende de la base de datos: recibe las escalas y categorías ya cargadas.
 * La misma entrada produce siempre el mismo resultado (RNF-04).
 */
public final class RpnCalculator {

    private final Map<FactorType, NavigableMap<Integer, ScaleLevel>> scales =
            new EnumMap<>(FactorType.class);
    private final List<PriorityCategory> categories;

    public RpnCalculator(Collection<ScaleLevel> levels, Collection<PriorityCategory> categories) {
        for (FactorType type : FactorType.values()) {
            scales.put(type, new TreeMap<>());
        }
        for (ScaleLevel level : levels) {
            if (scales.get(level.factor()).put(level.value(), level) != null) {
                throw new IllegalArgumentException("Nivel duplicado en la escala de "
                        + level.factor() + ": " + level.value());
            }
        }
        for (FactorType type : FactorType.values()) {
            if (scales.get(type).isEmpty()) {
                throw new IllegalArgumentException("La escala de " + type + " no tiene niveles");
            }
        }
        this.categories = validateCategories(categories);
    }

    public RpnResult evaluate(int severity, int occurrence, int detection) {
        ScaleLevel s = level(FactorType.SEVERIDAD, severity);
        ScaleLevel o = level(FactorType.OCURRENCIA, occurrence);
        ScaleLevel d = level(FactorType.DETECCION, detection);

        int rpn = Math.multiplyExact(Math.multiplyExact(severity, occurrence), detection);
        PriorityCategory category = categories.stream()
                .filter(c -> c.contains(rpn))
                .findFirst()
                .orElse(null);
        String rule = rule(severity, occurrence, detection);

        return new RpnResult(severity, occurrence, detection, rpn,
                s.descriptor(), o.descriptor(), d.descriptor(), category, rule);
    }

    private ScaleLevel level(FactorType type, int value) {
        ScaleLevel level = scales.get(type).get(value);
        if (level == null) {
            throw new IllegalArgumentException("El valor " + value
                    + " no existe en la escala de " + type + " definida para el caso de estudio");
        }
        return level;
    }

    private int lowestPossible() {
        return Math.multiplyExact(Math.multiplyExact(
                        scales.get(FactorType.SEVERIDAD).firstKey(),
                        scales.get(FactorType.OCURRENCIA).firstKey()),
                scales.get(FactorType.DETECCION).firstKey());
    }

    private int highestPossible() {
        return Math.multiplyExact(Math.multiplyExact(
                        scales.get(FactorType.SEVERIDAD).lastKey(),
                        scales.get(FactorType.OCURRENCIA).lastKey()),
                scales.get(FactorType.DETECCION).lastKey());
    }

    /** Las categorías deben cubrir todo el rango posible, sin huecos ni traslapes. */
    private List<PriorityCategory> validateCategories(Collection<PriorityCategory> input) {
        if (input.isEmpty()) {
            return List.of();
        }
        List<PriorityCategory> sorted = new ArrayList<>(input);
        sorted.sort(Comparator.comparingInt(PriorityCategory::minRpn));

        if (sorted.get(0).minRpn() > lowestPossible()) {
            throw new IllegalArgumentException(
                    "Las categorías no cubren el RPN mínimo posible (" + lowestPossible() + ")");
        }
        if (sorted.get(sorted.size() - 1).maxRpn() < highestPossible()) {
            throw new IllegalArgumentException(
                    "Las categorías no cubren el RPN máximo posible (" + highestPossible() + ")");
        }
        for (int i = 1; i < sorted.size(); i++) {
            PriorityCategory prev = sorted.get(i - 1);
            PriorityCategory current = sorted.get(i);
            if (current.minRpn() <= prev.maxRpn()) {
                throw new IllegalArgumentException("Las categorías '" + prev.name()
                        + "' y '" + current.name() + "' se traslapan");
            }
            if (current.minRpn() > prev.maxRpn() + 1) {
                throw new IllegalArgumentException("Hay un hueco entre las categorías '"
                        + prev.name() + "' y '" + current.name() + "'");
            }
        }
        return List.copyOf(sorted);
    }

    public static String rule(int severity, int occurrence, int detection) {
        int rpn = Math.multiplyExact(Math.multiplyExact(severity, occurrence), detection);
        return "RPN = S × O × D = " + severity + " × " + occurrence + " × "
                + detection + " = " + rpn;
    }
}