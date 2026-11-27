package org.example.iso;

import org.example.iso.Sensitivity.Outcome;
import org.example.iso.Sensitivity.Param;
import org.example.iso.Sensitivity.Report;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SensitivityTest {

    /** P = tasa × fracción × (1 − 0.99) × P2, evaluada en la matriz de la bomba. */
    private static Outcome model(Map<String, Double> v) {
        double p = v.get("Tasa de falla") * v.get("Fracción") * 0.01 * v.get("P2");
        RiskMatrix.Verdict verdict = RiskMatrixTest.matrix()
                .evaluate((int) Math.round(v.get("Severidad")), p, "por hora");
        return new Outcome(verdict.riskLevel(), verdict.acceptable());
    }

    private static List<Param> params() {
        return List.of(
                Sensitivity.range("Fracción", 0.2, 0.05, 0.5),
                Sensitivity.range("P2", 0.3, 0.05, 1.0),
                Sensitivity.range("Tasa de falla", 1e-6, 1e-6, 1e-4),
                Sensitivity.discrete("Severidad", 5, List.of(4.0, 5.0)));
    }

    @Test
    void soloLaSeveridadDecideCuandoCadaValorSeMueveSolo() {
        Report r = Sensitivity.analyze(params(), SensitivityTest::model);
        assertEquals("Tolerable", r.baseline().riskLevel());
        assertEquals(List.of("Severidad"), r.decisive());
    }

    @Test
    void detectaCambiosQueSoloAparecenCuandoLasIncertidumbresCoinciden() {
        // Tasa 1e-4, fracción 0.5 y P2 1.0 a la vez dan P = 5e-7 (nivel 2) → Inaceptable con severidad 5,
        // aunque ninguno de ellos cambie el veredicto por separado.
        Report r = Sensitivity.analyze(params(), SensitivityTest::model);
        assertTrue(r.cornersExplored());
        assertTrue(r.cornerChanges().stream().anyMatch(c ->
                c.values().get("Severidad") == 5.0
                        && c.values().get("Tasa de falla") == 1e-4
                        && c.outcome().riskLevel().equals("Inaceptable")));
    }

    @Test
    void sinSeveridadVariableElCambioSoloEsConjunto() {
        List<Param> sinSeveridad = List.of(
                Sensitivity.range("Fracción", 0.2, 0.05, 0.5),
                Sensitivity.range("P2", 0.3, 0.05, 1.0),
                Sensitivity.range("Tasa de falla", 1e-6, 1e-6, 1e-4),
                Sensitivity.discrete("Severidad", 5, List.of(5.0)));
        Report r = Sensitivity.analyze(sinSeveridad, SensitivityTest::model);
        assertTrue(r.decisive().isEmpty());
        assertTrue(r.changesOnlyJointly());
    }

    @Test
    void laMallaIncluyeExtremosYValorRegistrado() {
        Param p = Sensitivity.range("Tasa de falla", 3e-6, 1e-6, 1e-4);
        assertEquals(1e-6, p.candidates().get(0), 1e-24);
        assertEquals(1e-4, p.candidates().get(p.candidates().size() - 1), 1e-18);
        assertTrue(p.candidates().contains(3e-6));
    }

    @Test
    void rechazaUnValorRegistradoFueraDeSuRango() {
        assertThrows(IllegalArgumentException.class, () -> Sensitivity.range("x", 0.9, 0.1, 0.5));
    }

    @Test
    void unParametroQueNoAfectaAlModeloNoDecide() {
        List<Param> ps = List.of(Sensitivity.range("Irrelevante", 1.0, 0.0, 10.0));
        Report r = Sensitivity.analyze(ps, v -> new Outcome("Aceptable", true));
        assertTrue(r.decisive().isEmpty());
        assertFalse(r.changesOnlyJointly());
    }
}
