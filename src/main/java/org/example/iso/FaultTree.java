// FaultTree.java
package org.example.iso;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Árbol de fallas (FTA) con compuertas Y/O y eventos básicos con probabilidad.
 * Calcula los conjuntos de corte mínimos, los puntos únicos de falla y la probabilidad del
 * evento superior. No depende de la base de datos.
 *
 * Supuestos que el usuario debe conocer:
 * - Los eventos básicos son estadísticamente independientes. Un mismo evento (mismo id) que
 *   aparece en varias ramas se trata como UNA sola variable, no como copias independientes.
 * - Una probabilidad "por hora" se usa como probabilidad en una hora de operación
 *   (válido cuando la tasa por hora es muy pequeña).
 * - Una compuerta Y no puede combinar dos eventos con unidad de exposición: su producto no
 *   tiene unidad coherente (p. ej. dos tasas "por hora"). Uno debe expresarse como
 *   probabilidad adimensional en el intervalo de interés.
 */
public final class FaultTree {

    public enum Gate { AND, OR }

    public sealed interface Node permits Basic, GateNode {
        String id();
        String label();
    }

    public record Basic(String id, String label, Amount probability) implements Node {
        public Basic {
            requireId(id);
            Objects.requireNonNull(probability, "El evento básico necesita una probabilidad");
        }
    }

    public record GateNode(String id, String label, Gate gate, List<Node> children) implements Node {
        public GateNode {
            requireId(id);
            Objects.requireNonNull(gate, "La compuerta necesita un tipo");
            if (children == null || children.size() < 2) {
                throw new IllegalArgumentException(
                        "La compuerta '" + id + "' necesita al menos dos entradas");
            }
            children = List.copyOf(children);
        }
    }

    /** Combinación mínima de eventos básicos que causa el evento superior. */
    public record CutSet(List<String> eventIds, double probability) {}

    public record Result(double probability, String unit, boolean exact,
                         List<CutSet> cutSets, List<String> singlePointsOfFailure) {}

    /** Con más conjuntos de corte que esto, el cálculo exacto se reemplaza por una cota superior. */
    static final int MAX_CUT_SETS_EXACT = 20;
    private static final int MAX_CUT_SETS_BUILD = 100_000;

    private final Node top;

    private FaultTree(Node top) {
        this.top = top;
    }

    public static FaultTree of(Node top) {
        Objects.requireNonNull(top, "El árbol necesita un evento superior");
        return new FaultTree(top);
    }

    public Result evaluate() {
        Map<String, Double> probabilities = basicProbabilities();
        String unit = resolveUnit(top);

        List<Set<String>> sets = cutSets(top);
        List<String> ids = new ArrayList<>(new TreeSet<>(probabilities.keySet()));

        List<CutSet> cutSets = new ArrayList<>();
        for (Set<String> s : sets) {
            double p = 1.0;
            for (String id : s) {
                p *= probabilities.get(id);
            }
            cutSets.add(new CutSet(List.copyOf(s), p));
        }
        cutSets.sort(Comparator.comparingDouble(CutSet::probability).reversed()
                .thenComparingInt(c -> c.eventIds().size()));

        boolean exact = sets.size() <= MAX_CUT_SETS_EXACT && ids.size() <= 62;
        double probability = exact
                ? inclusionExclusion(sets, ids, probabilities)
                : upperBound(cutSets);
        probability = Math.max(0.0, Math.min(1.0, probability));

        List<String> singlePoints = cutSets.stream()
                .filter(c -> c.eventIds().size() == 1)
                .map(c -> c.eventIds().get(0))
                .toList();

        return new Result(probability, unit, exact, List.copyOf(cutSets), singlePoints);
    }

    // ---------------------------------------------------------------- validación y unidades

    private Map<String, Double> basicProbabilities() {
        Map<String, Amount> seen = new LinkedHashMap<>();
        collectBasics(top, seen);
        Map<String, Double> out = new TreeMap<>();
        seen.forEach((id, amount) -> out.put(id, amount.canonicalValue()));
        return out;
    }

    private void collectBasics(Node node, Map<String, Amount> seen) {
        if (node instanceof Basic b) {
            Amount canon = b.probability().canonical();
            if (canon.value() > 1.0) {
                throw new IllegalArgumentException("El evento '" + b.id()
                        + "' tiene probabilidad mayor que 1 (" + canon.value() + ")");
            }
            Amount previous = seen.put(b.id(), canon);
            if (previous != null && !previous.equals(canon)) {
                throw new IllegalArgumentException("El evento '" + b.id()
                        + "' aparece con valores distintos en el árbol");
            }
        } else {
            for (Node child : ((GateNode) node).children()) {
                collectBasics(child, seen);
            }
        }
    }

    /** Devuelve la unidad canónica del nodo o lanza error si la combinación no es coherente. */
    private String resolveUnit(Node node) {
        if (node instanceof Basic b) {
            return b.probability().canonical().unit();
        }
        GateNode g = (GateNode) node;
        Set<String> withUnit = new LinkedHashSet<>();
        boolean hasDimensionless = false;
        for (Node child : g.children()) {
            String u = resolveUnit(child);
            if (Amount.ADIMENSIONAL.equals(u)) {
                hasDimensionless = true;
            } else {
                withUnit.add(u);
            }
        }
        if (g.gate() == Gate.AND) {
            if (withUnit.size() > 1 || (withUnit.size() == 1 && countWithUnit(g) > 1)) {
                throw new IllegalArgumentException("La compuerta Y '" + g.id()
                        + "' combina más de un evento con unidad de exposición " + withUnit
                        + ": exprese uno como probabilidad adimensional en el intervalo de interés");
            }
            return withUnit.isEmpty() ? Amount.ADIMENSIONAL : withUnit.iterator().next();
        }
        if (withUnit.size() > 1) {
            throw new IllegalArgumentException("La compuerta O '" + g.id()
                    + "' mezcla unidades de exposición distintas: " + withUnit);
        }
        if (!withUnit.isEmpty() && hasDimensionless) {
            throw new IllegalArgumentException("La compuerta O '" + g.id()
                    + "' mezcla una probabilidad adimensional con una por " + withUnit);
        }
        return withUnit.isEmpty() ? Amount.ADIMENSIONAL : withUnit.iterator().next();
    }

    private int countWithUnit(GateNode g) {
        int n = 0;
        for (Node child : g.children()) {
            if (!Amount.ADIMENSIONAL.equals(resolveUnit(child))) {
                n++;
            }
        }
        return n;
    }

    // ---------------------------------------------------------------- conjuntos de corte

    private List<Set<String>> cutSets(Node node) {
        if (node instanceof Basic b) {
            Set<String> single = new TreeSet<>();
            single.add(b.id());
            return List.of(single);
        }
        GateNode g = (GateNode) node;
        List<List<Set<String>>> parts = new ArrayList<>();
        for (Node child : g.children()) {
            parts.add(cutSets(child));
        }
        if (g.gate() == Gate.OR) {
            List<Set<String>> union = new ArrayList<>();
            parts.forEach(union::addAll);
            return minimize(union);
        }
        List<Set<String>> acc = parts.get(0);
        for (int i = 1; i < parts.size(); i++) {
            List<Set<String>> next = new ArrayList<>();
            for (Set<String> a : acc) {
                for (Set<String> b : parts.get(i)) {
                    Set<String> merged = new TreeSet<>(a);
                    merged.addAll(b);
                    next.add(merged);
                }
            }
            if (next.size() > MAX_CUT_SETS_BUILD) {
                throw new IllegalStateException(
                        "El árbol genera demasiados conjuntos de corte; simplifique su estructura");
            }
            acc = minimize(next);
        }
        return acc;
    }

    /** Elimina duplicados y conjuntos que contienen a otro más pequeño (absorción). */
    private static List<Set<String>> minimize(Collection<Set<String>> input) {
        List<Set<String>> sorted = new ArrayList<>(new LinkedHashSet<>(input));
        sorted.sort(Comparator.comparingInt(Set::size));
        List<Set<String>> kept = new ArrayList<>();
        for (Set<String> candidate : sorted) {
            boolean absorbed = false;
            for (Set<String> k : kept) {
                if (candidate.containsAll(k)) {
                    absorbed = true;
                    break;
                }
            }
            if (!absorbed) {
                kept.add(candidate);
            }
        }
        return kept;
    }

    // ---------------------------------------------------------------- probabilidad

    /** Exacta: P(∪ Cᵢ) por inclusión–exclusión sobre los conjuntos de corte mínimos. */
    private static double inclusionExclusion(List<Set<String>> sets, List<String> ids,
                                             Map<String, Double> p) {
        int m = sets.size();
        long[] masks = new long[m];
        for (int i = 0; i < m; i++) {
            long mask = 0L;
            for (String id : sets.get(i)) {
                mask |= 1L << ids.indexOf(id);
            }
            masks[i] = mask;
        }
        double[] pv = new double[ids.size()];
        for (int j = 0; j < pv.length; j++) {
            pv[j] = p.get(ids.get(j));
        }
        double sum = 0.0;
        for (int subset = 1; subset < (1 << m); subset++) {
            long union = 0L;
            for (int i = 0; i < m; i++) {
                if ((subset & (1 << i)) != 0) {
                    union |= masks[i];
                }
            }
            double product = 1.0;
            for (int j = 0; j < pv.length; j++) {
                if ((union & (1L << j)) != 0) {
                    product *= pv[j];
                }
            }
            sum += (Integer.bitCount(subset) % 2 == 1) ? product : -product;
        }
        return sum;
    }

    /** Cota superior (Esary–Proschan): 1 − Π(1 − P(Cᵢ)). Nunca subestima la probabilidad. */
    private static double upperBound(List<CutSet> cutSets) {
        double none = 1.0;
        for (CutSet c : cutSets) {
            none *= (1.0 - c.probability());
        }
        return 1.0 - none;
    }

    private static void requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El nodo necesita un identificador");
        }
    }
}
