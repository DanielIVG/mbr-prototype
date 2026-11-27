package org.example.iso;

import org.example.iso.RiskMatrix.Cell;
import org.example.iso.RiskMatrix.Criterion;
import org.example.iso.RiskMatrix.ProbabilityLevel;
import org.example.iso.RiskMatrix.SeverityLevel;
import org.example.iso.RiskMatrix.Verdict;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskMatrixTest {

    static final List<SeverityLevel> SEVERITIES = List.of(
            new SeverityLevel(1, "Despreciable"), new SeverityLevel(2, "Menor"),
            new SeverityLevel(3, "Seria"), new SeverityLevel(4, "Crítica"),
            new SeverityLevel(5, "Catastrófica"));

    static final List<ProbabilityLevel> PROBABILITIES = List.of(
            new ProbabilityLevel(1, "Improbable", 0.0, 1e-7),
            new ProbabilityLevel(2, "Remota", 1e-7, 1e-5),
            new ProbabilityLevel(3, "Ocasional", 1e-5, 1e-3),
            new ProbabilityLevel(4, "Probable", 1e-3, 1e-1),
            new ProbabilityLevel(5, "Frecuente", 1e-1, 1.0));

    static final List<Criterion> CRITERIA = List.of(
            new Criterion("Aceptable", true), new Criterion("Tolerable", false),
            new Criterion("Inaceptable", false));

    /**
     * Diagonal "Tolerable"; con menos probabilidad Aceptable; con más, Inaceptable.
     * Severidad 5 → Tolerable en probabilidad 1; severidad 4 → en 2; … severidad 1 → en 5.
     */
    static List<Cell> cells() {
        List<Cell> cells = new ArrayList<>();
        for (int s = 1; s <= 5; s++) {
            int tolerableAt = 6 - s;
            for (int p = 1; p <= 5; p++) {
                String level = p < tolerableAt ? "Aceptable" : p == tolerableAt ? "Tolerable" : "Inaceptable";
                cells.add(new Cell(s, p, level));
            }
        }
        return cells;
    }

    static RiskMatrix matrix() {
        return new RiskMatrix("por hora", SEVERITIES, PROBABILITIES, cells(), CRITERIA);
    }

    @Test
    void asignaElNivelDeProbabilidadEnLosLimites() {
        RiskMatrix m = matrix();
        assertEquals(1, m.levelOf(0.0).value());
        assertEquals(1, m.levelOf(9.99e-8).value());
        assertEquals(2, m.levelOf(1e-7).value());          // el límite inferior pertenece a la banda de arriba
        assertEquals(3, m.levelOf(3e-5).value());          // 3×10⁻⁵ es Ocasional, no Remota
        assertEquals(5, m.levelOf(0.1).value());
        assertEquals(5, m.levelOf(1.0).value());           // la última banda incluye 1
    }

    @Test
    void severidadCincoConProbabilidadImprobableEsTolerableYRequiereControl() {
        Verdict v = matrix().evaluate(5, 6e-8, "por hora");
        assertEquals("Tolerable", v.riskLevel());
        assertFalse(v.acceptable());
        assertEquals("Catastrófica", v.severity().descriptor());
        assertEquals("Improbable", v.probability().descriptor());
    }

    @Test
    void severidadCuatroConLaMismaProbabilidadEsAceptable() {
        Verdict v = matrix().evaluate(4, 6e-8, "por hora");
        assertEquals("Aceptable", v.riskLevel());
        assertTrue(v.acceptable());
    }

    @Test
    void masProbabilidadConSeveridadCincoEsInaceptable() {
        assertEquals("Inaceptable", matrix().evaluate(5, 5e-7, "por hora").riskLevel());
    }

    @Test
    void aceptaLaUnidadEscritaDeOtraFormaPeroRechazaOtraUnidad() {
        assertEquals("Tolerable", matrix().evaluate(5, 6e-8, "1/h").riskLevel());
        assertThrows(IllegalArgumentException.class, () -> matrix().evaluate(5, 6e-8, "por ciclo"));
    }

    @Test
    void rechazaSeveridadYProbabilidadFueraDeEscala() {
        assertThrows(IllegalArgumentException.class, () -> matrix().evaluate(9, 6e-8, "por hora"));
        assertThrows(IllegalArgumentException.class, () -> matrix().levelOf(1.5));
        assertThrows(IllegalArgumentException.class, () -> matrix().levelOf(-0.1));
    }

    @Test
    void rechazaBandasConHuecoTraslapeOSinCubrirElRango() {
        List<ProbabilityLevel> hueco = List.of(
                new ProbabilityLevel(1, "a", 0.0, 1e-7), new ProbabilityLevel(2, "b", 1e-6, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new RiskMatrix("por hora", SEVERITIES,
                hueco, List.of(), CRITERIA));

        List<ProbabilityLevel> traslape = List.of(
                new ProbabilityLevel(1, "a", 0.0, 1e-5), new ProbabilityLevel(2, "b", 1e-6, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new RiskMatrix("por hora", SEVERITIES,
                traslape, List.of(), CRITERIA));

        List<ProbabilityLevel> noEmpieza = List.of(
                new ProbabilityLevel(1, "a", 1e-9, 1e-7), new ProbabilityLevel(2, "b", 1e-7, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new RiskMatrix("por hora", SEVERITIES,
                noEmpieza, List.of(), CRITERIA));

        List<ProbabilityLevel> noTermina = List.of(
                new ProbabilityLevel(1, "a", 0.0, 1e-7), new ProbabilityLevel(2, "b", 1e-7, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new RiskMatrix("por hora", SEVERITIES,
                noTermina, List.of(), CRITERIA));
    }

    @Test
    void rechazaMatrizConCeldaFaltanteOCriterioFaltante() {
        List<Cell> incompleta = new ArrayList<>(cells());
        incompleta.remove(0);
        assertThrows(IllegalArgumentException.class, () -> new RiskMatrix("por hora", SEVERITIES,
                PROBABILITIES, incompleta, CRITERIA));

        List<Criterion> sinTolerable = List.of(new Criterion("Aceptable", true),
                new Criterion("Inaceptable", false));
        assertThrows(IllegalArgumentException.class, () -> new RiskMatrix("por hora", SEVERITIES,
                PROBABILITIES, cells(), sinTolerable));
    }

    @Test
    void laEscalaDebeDeclararUnidadDeExposicion() {
        assertThrows(IllegalArgumentException.class, () -> new RiskMatrix("adimensional", SEVERITIES,
                PROBABILITIES, cells(), CRITERIA));
    }
}
