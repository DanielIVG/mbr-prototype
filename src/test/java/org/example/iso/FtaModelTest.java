package org.example.iso;

import org.example.iso.FaultTree.Gate;
import org.example.iso.FtaModel.Basic;
import org.example.iso.FtaModel.GateSpec;
import org.example.iso.FtaModel.Ref;
import org.example.iso.FtaModel.Spec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FtaModelTest {

    private static Basic b(String key, double p, String unit, Double min, Double max) {
        return new Basic(key, "Evento " + key, new Amount(p, unit), min, max);
    }

    @Test
    void calculaUnaOConUnaYYDetectaElPuntoUnicoDeFalla() {
        Spec spec = new Spec("Exceso de fármaco", List.of(
                b("A", 1e-6, "por hora", null, null), b("B", 1e-3, "adimensional", null, null),
                b("C", 1e-4, "por hora", null, null)),
                new GateSpec("Exceso", Gate.OR, null, List.of(new Ref("A"),
                        new GateSpec("B y C", Gate.AND, "Fallas físicamente independientes",
                                List.of(new Ref("B"), new Ref("C"))))));
        var r = FtaModel.evaluate(spec);
        assertEquals("por hora", r.unit());
        assertEquals(1e-6 + 1e-7 - 1e-13, r.probability(), 1e-18);
        assertEquals(List.of("Evento A"), r.singlePoints());
        assertEquals(2, r.cutSets().size());
        assertTrue(r.exact());
    }

    @Test
    void unEventoRepetidoEsUnaSolaVariable() {
        // (X y Y) o (X y Z) = X·(Y + Z − Y·Z) = 1e-4 × 0.75, no 1 − (1 − X·Y)(1 − X·Z)
        Spec spec = new Spec("Top", List.of(b("X", 1e-4, "por hora", null, null),
                b("Y", 0.5, "adimensional", null, null), b("Z", 0.5, "adimensional", null, null)),
                new GateSpec("Top", Gate.OR, null, List.of(
                        new GateSpec("XY", Gate.AND, "Independientes", List.of(new Ref("X"), new Ref("Y"))),
                        new GateSpec("XZ", Gate.AND, "Independientes", List.of(new Ref("X"), new Ref("Z"))))));
        assertEquals(7.5e-5, FtaModel.evaluate(spec).probability(), 1e-15);
    }

    @Test
    void elRangoDelEventoSuperiorSaleDeEvaluarTodoEnSuMinimoYSuMaximo() {
        Spec spec = new Spec("Top", List.of(b("A", 1e-6, "por hora", 1e-7, 1e-5),
                b("B", 1e-3, "adimensional", 1e-4, 1e-2)),
                new GateSpec("Top", Gate.OR, "", List.of(new Ref("A"),
                        new GateSpec("g", Gate.AND, "ind", List.of(new Ref("B"), new Ref("A"))))));
        var r = FtaModel.evaluate(spec);
        assertTrue(r.min() < r.probability() && r.probability() < r.max());
        assertEquals(1e-7, r.min(), 1e-12);                      // A domina: A ∪ (A∩B) = A
        assertEquals(1e-5, r.max(), 1e-10);
    }

    @Test
    void laCompuertaYExigeJustificarLaIndependencia() {
        Spec spec = new Spec("Top", List.of(b("A", 1e-3, "adimensional", null, null),
                b("B", 1e-6, "por hora", null, null)),
                new GateSpec("Top", Gate.AND, " ", List.of(new Ref("A"), new Ref("B"))));
        assertThrows(IllegalArgumentException.class, () -> FtaModel.evaluate(spec));
    }

    @Test
    void rechazaEventosInexistentesRepetidosYRangosInconsistentes() {
        var oneBasic = List.of(b("A", 1e-3, "adimensional", null, null), b("B", 1e-3, "adimensional", null, null));
        assertThrows(IllegalArgumentException.class, () -> FtaModel.evaluate(new Spec("T", oneBasic,
                new GateSpec("T", Gate.OR, null, List.of(new Ref("A"), new Ref("Q"))))));
        assertThrows(IllegalArgumentException.class, () -> FtaModel.evaluate(new Spec("T",
                List.of(b("A", 1e-3, "adimensional", null, null), b("A", 1e-3, "adimensional", null, null)),
                new GateSpec("T", Gate.OR, null, List.of(new Ref("A"), new Ref("A"))))));
        assertThrows(IllegalArgumentException.class, () -> FtaModel.evaluate(new Spec("T",
                List.of(b("A", 1e-3, "adimensional", 1e-2, 1e-1), b("B", 1e-3, "adimensional", null, null)),
                new GateSpec("T", Gate.OR, null, List.of(new Ref("A"), new Ref("B"))))));
        assertThrows(IllegalArgumentException.class, () -> FtaModel.evaluate(new Spec("  ", oneBasic,
                new GateSpec("T", Gate.OR, null, List.of(new Ref("A"), new Ref("B"))))));
    }

    @Test
    void laRaizDebeSerUnaCompuerta() {
        assertThrows(IllegalArgumentException.class, () -> FtaModel.evaluate(new Spec("T",
                List.of(b("A", 1e-3, "adimensional", null, null)), new Ref("A"))));
    }
}
