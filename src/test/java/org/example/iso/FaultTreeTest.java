package org.example.iso;

import org.example.iso.FaultTree.Basic;
import org.example.iso.FaultTree.Gate;
import org.example.iso.FaultTree.GateNode;
import org.example.iso.FaultTree.Node;
import org.example.iso.FaultTree.Result;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FaultTreeTest {

    private static Basic dimless(String id, double p) {
        return new Basic(id, id, Amount.dimensionless(p));
    }

    private static Basic perHour(String id, double p) {
        return new Basic(id, id, Amount.perHour(p));
    }

    private static GateNode gate(String id, Gate type, Node... children) {
        return new GateNode(id, id, type, List.of(children));
    }

    @Test
    void compuertaOCombinaComoUnoMenosElProductoDeLosComplementos() {
        // 1 - (0.9 × 0.8) = 0.28
        Result r = FaultTree.of(gate("top", Gate.OR, dimless("a", 0.1), dimless("b", 0.2))).evaluate();
        assertEquals(0.28, r.probability(), 1e-12);
        assertTrue(r.exact());
        assertEquals(List.of("b", "a"), r.singlePointsOfFailure());
    }

    @Test
    void compuertaYMultiplica() {
        Result r = FaultTree.of(gate("top", Gate.AND, dimless("a", 0.1), dimless("b", 0.2))).evaluate();
        assertEquals(0.02, r.probability(), 1e-12);
        assertEquals(1, r.cutSets().size());
        assertEquals(List.of("a", "b"), r.cutSets().get(0).eventIds());
        assertTrue(r.singlePointsOfFailure().isEmpty());
    }

    @Test
    void unEventoRepetidoEsUnaSolaVariable() {
        // top = (a ∧ b) ∨ (a ∧ c), todos 0.5.  P = P(a) × P(b ∨ c) = 0.5 × 0.75 = 0.375.
        // La fórmula ingenua por compuertas daría 1 - (1 - 0.25)² = 0.4375, que es incorrecta.
        Node tree = gate("top", Gate.OR,
                gate("g1", Gate.AND, dimless("a", 0.5), dimless("b", 0.5)),
                gate("g2", Gate.AND, dimless("a", 0.5), dimless("c", 0.5)));
        Result r = FaultTree.of(tree).evaluate();
        assertEquals(0.375, r.probability(), 1e-12);
        assertEquals(2, r.cutSets().size());
    }

    @Test
    void laAbsorcionEliminaConjuntosRedundantes() {
        // a ∨ (a ∧ b) = a
        Node tree = gate("top", Gate.OR, dimless("a", 0.1),
                gate("g", Gate.AND, dimless("a", 0.1), dimless("b", 0.2)));
        Result r = FaultTree.of(tree).evaluate();
        assertEquals(1, r.cutSets().size());
        assertEquals(List.of("a"), r.cutSets().get(0).eventIds());
        assertEquals(0.1, r.probability(), 1e-12);
    }

    @Test
    void sobredosisDeLaBombaDeInfusion() {
        // Valores ilustrativos. Rama A: encoder 1e-6/h × 0.2 = 2e-7/h, Y la verificación falla (1 - 0.99).
        Node tree = gate("sobredosis", Gate.OR,
                gate("rama_a", Gate.AND, perHour("encoder_pierde_pulsos", 2e-7), dimless("verificacion_falla", 0.01)),
                perHour("error_calculo_dosis", 5e-9),
                perHour("error_operador", 1e-8));
        Result r = FaultTree.of(tree).evaluate();

        // 2e-9 + 5e-9 + 1e-8 = 1.7e-8 (los términos de intersección son del orden de 1e-16)
        assertEquals(1.7e-8, r.probability(), 1e-15);
        assertEquals("por hora", r.unit());
        assertEquals(3, r.cutSets().size());
        assertEquals(List.of("error_operador", "error_calculo_dosis"), r.singlePointsOfFailure());
        assertEquals(List.of("error_operador"), r.cutSets().get(0).eventIds());
    }

    @Test
    void rechazaCompuertaYConDosEventosConUnidadDeExposicion() {
        Node tree = gate("top", Gate.AND, perHour("a", 1e-6), perHour("b", 1e-6));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> FaultTree.of(tree).evaluate());
        assertTrue(e.getMessage().contains("Y"));
    }

    @Test
    void rechazaCompuertaOQueMezclaAdimensionalConPorHora() {
        Node tree = gate("top", Gate.OR, perHour("a", 1e-6), dimless("b", 0.01));
        assertThrows(IllegalArgumentException.class, () -> FaultTree.of(tree).evaluate());
    }

    @Test
    void rechazaCompuertaOConUnidadesDistintas() {
        Node tree = gate("top", Gate.OR, perHour("a", 1e-6), new Basic("b", "b", new Amount(1e-6, "por ciclo")));
        assertThrows(IllegalArgumentException.class, () -> FaultTree.of(tree).evaluate());
    }

    @Test
    void rechazaProbabilidadMayorQueUno() {
        Node tree = gate("top", Gate.OR, dimless("a", 1.5), dimless("b", 0.1));
        assertThrows(IllegalArgumentException.class, () -> FaultTree.of(tree).evaluate());
    }

    @Test
    void rechazaElMismoEventoConValoresDistintos() {
        Node tree = gate("top", Gate.OR,
                gate("g1", Gate.AND, dimless("a", 0.5), dimless("b", 0.5)),
                gate("g2", Gate.AND, dimless("a", 0.4), dimless("c", 0.5)));
        assertThrows(IllegalArgumentException.class, () -> FaultTree.of(tree).evaluate());
    }

    @Test
    void rechazaCompuertaConUnaSolaEntrada() {
        assertThrows(IllegalArgumentException.class,
                () -> new GateNode("g", "g", Gate.OR, List.of(dimless("a", 0.1))));
    }

    @Test
    void conMuchosConjuntosUsaCotaSuperiorYLoDice() {
        List<Node> events = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            events.add(dimless("e" + i, 0.01));
        }
        Result r = FaultTree.of(new GateNode("top", "top", Gate.OR, events)).evaluate();
        assertFalse(r.exact());
        // Con conjuntos de un solo evento, la cota coincide con el valor real: 1 - 0.99^21.
        assertEquals(1.0 - Math.pow(0.99, 21), r.probability(), 1e-12);
    }
}
