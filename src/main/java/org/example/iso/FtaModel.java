// FtaModel.java
package org.example.iso;

import org.example.iso.FaultTree.Gate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Árbol de fallas tal como lo edita el usuario: eventos básicos con clave y compuertas que los
 * referencian. Una misma clave usada en varias ramas es UN solo evento (no copias independientes).
 * Calcula la probabilidad del evento superior, su rango (todos los eventos en su mínimo / máximo) y
 * los conjuntos de corte con nombres legibles. No depende de la base de datos.
 *
 * Rango: un árbol sin negaciones es monótono (más probabilidad en un evento nunca baja la del evento
 * superior), por eso el mínimo y el máximo del evento superior salen de evaluar con todos los eventos
 * en su mínimo y en su máximo.
 */
public final class FtaModel {

    public record Basic(String key, String label, Amount probability, Double min, Double max) {}

    public sealed interface Node permits GateSpec, Ref {}

    /** Compuerta; las compuertas Y exigen justificar la independencia de sus entradas. */
    public record GateSpec(String label, Gate type, String independenceJustification, List<Node> children)
            implements Node {
        public GateSpec {
            children = List.copyOf(children);
        }
    }

    public record Ref(String key) implements Node {}

    public record Spec(String topEvent, List<Basic> basics, Node root) {
        public Spec {
            basics = List.copyOf(basics);
        }
    }

    public record LabeledCutSet(List<String> events, double probability) {}

    public record Evaluation(double probability, double min, double max, String unit, boolean exact,
                             List<LabeledCutSet> cutSets, List<String> singlePoints) {}

    private FtaModel() {
    }

    public static Evaluation evaluate(Spec spec) {
        validate(spec);
        Map<String, Basic> byKey = new LinkedHashMap<>();
        spec.basics().forEach(b -> byKey.put(b.key(), b));

        FaultTree.Result base = FaultTree.of(build(spec.root(), byKey, Mode.BASE, new int[] {0})).evaluate();
        FaultTree.Result low = FaultTree.of(build(spec.root(), byKey, Mode.MIN, new int[] {0})).evaluate();
        FaultTree.Result high = FaultTree.of(build(spec.root(), byKey, Mode.MAX, new int[] {0})).evaluate();

        List<LabeledCutSet> cuts = base.cutSets().stream()
                .map(c -> new LabeledCutSet(c.eventIds().stream().map(id -> byKey.get(id).label()).toList(),
                        c.probability()))
                .toList();
        List<String> singles = base.singlePointsOfFailure().stream().map(id -> byKey.get(id).label()).toList();
        return new Evaluation(base.probability(), low.probability(), high.probability(), base.unit(),
                base.exact(), cuts, singles);
    }

    private enum Mode { BASE, MIN, MAX }

    private static FaultTree.Node build(Node node, Map<String, Basic> basics, Mode mode, int[] counter) {
        if (node instanceof Ref r) {
            Basic b = basics.get(r.key());
            double value = switch (mode) {
                case MIN -> b.min() == null ? b.probability().value() : b.min();
                case MAX -> b.max() == null ? b.probability().value() : b.max();
                default -> b.probability().value();
            };
            return new FaultTree.Basic(b.key(), b.label(), new Amount(value, b.probability().unit()));
        }
        GateSpec g = (GateSpec) node;
        List<FaultTree.Node> children = new ArrayList<>();
        for (Node child : g.children()) {
            children.add(build(child, basics, mode, counter));
        }
        return new FaultTree.GateNode("G" + (++counter[0]), g.label(), g.type(), children);
    }

    private static void validate(Spec spec) {
        if (spec.topEvent() == null || spec.topEvent().isBlank()) {
            throw new IllegalArgumentException("El evento superior necesita una descripción");
        }
        Set<String> keys = new HashSet<>();
        for (Basic b : spec.basics()) {
            if (b.key() == null || b.key().isBlank()) {
                throw new IllegalArgumentException("Todo evento básico necesita un nombre");
            }
            if (!keys.add(b.key())) {
                throw new IllegalArgumentException("Evento básico repetido: " + b.key());
            }
            if ((b.min() == null) != (b.max() == null)) {
                throw new IllegalArgumentException("El rango de '" + b.label() + "' necesita mínimo y máximo");
            }
            if (b.min() != null && !(b.min() <= b.probability().value() && b.probability().value() <= b.max())) {
                throw new IllegalArgumentException("El valor de '" + b.label() + "' está fuera de su rango");
            }
        }
        if (!(spec.root() instanceof GateSpec)) {
            throw new IllegalArgumentException("El evento superior debe ser una compuerta con al menos dos entradas");
        }
        check(spec.root(), keys, new HashSet<>());
    }

    private static void check(Node node, Set<String> keys, Set<String> used) {
        if (node instanceof Ref r) {
            if (!keys.contains(r.key())) {
                throw new IllegalArgumentException("El árbol usa un evento inexistente: " + r.key());
            }
            used.add(r.key());
            return;
        }
        GateSpec g = (GateSpec) node;
        if (g.children().size() < 2) {
            throw new IllegalArgumentException("La compuerta '" + g.label() + "' necesita al menos dos entradas");
        }
        if (g.type() == Gate.AND && (g.independenceJustification() == null || g.independenceJustification().isBlank())) {
            throw new IllegalArgumentException("La compuerta Y '" + g.label()
                    + "' necesita justificar por qué sus entradas son independientes");
        }
        g.children().forEach(c -> check(c, keys, used));
    }

    /** Claves de los eventos que el árbol realmente usa. */
    public static Set<String> usedKeys(Node root) {
        Set<String> out = new HashSet<>();
        collect(root, out);
        return out;
    }

    private static void collect(Node node, Set<String> out) {
        if (node instanceof Ref r) {
            out.add(r.key());
        } else {
            ((GateSpec) node).children().forEach(c -> collect(c, out));
        }
    }
}
