package org.example.iso;

import org.example.iso.RiskEstimator.Estimate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskEstimatorTest {

    @Test
    void p1EsTasaPorFraccionPorLosComplementosDeLasBarreras() {
        // P1 = 1e-6 × 0.2 × (1 - 0.99) = 2e-9 ;  P = 2e-9 × 0.3 = 6e-10
        Estimate e = RiskEstimator.fromFailureMode(Amount.perHour(1e-6), Amount.dimensionless(0.2),
                List.of(Amount.dimensionless(0.99)), Amount.dimensionless(0.3));
        assertEquals(2e-9, e.p1(), 1e-21);
        assertEquals(0.3, e.p2(), 1e-12);
        assertEquals(6e-10, e.pTotal(), 1e-22);
        assertEquals("por hora", e.exposureUnit());
        assertNull(e.justification());
    }

    @Test
    void sinBarrerasP1EsTasaPorFraccion() {
        // 1e-6 × 0.2 = 2e-7 ;  × 0.3 = 6e-8
        Estimate e = RiskEstimator.fromFailureMode(Amount.perHour(1e-6), Amount.dimensionless(0.2),
                List.of(), Amount.dimensionless(0.3));
        assertEquals(2e-7, e.p1(), 1e-19);
        assertEquals(6e-8, e.pTotal(), 1e-20);
    }

    @Test
    void laTrazaExplicaCadaFactor() {
        Estimate e = RiskEstimator.fromFailureMode(Amount.perHour(1e-6), Amount.dimensionless(0.2),
                List.of(Amount.dimensionless(0.99)), Amount.dimensionless(0.3));
        List<String> names = e.trace().stream().map(RiskEstimator.Factor::name).toList();
        assertEquals(List.of("Tasa de falla", "Fracción del modo de falla",
                "Probabilidad de que falle la barrera 1", "P1", "P2"), names);
    }

    @Test
    void aceptaTasasEnFit() {
        // 1000 FIT = 1e-6 por hora
        Estimate e = RiskEstimator.fromFailureMode(new Amount(1000, "FIT"), Amount.dimensionless(0.2),
                List.of(), Amount.dimensionless(0.3));
        assertEquals(6e-8, e.pTotal(), 1e-20);
    }

    @Test
    void exigeUnidadDeExposicionEnLaTasa() {
        assertThrows(IllegalArgumentException.class, () -> RiskEstimator.fromFailureMode(
                Amount.dimensionless(1e-6), Amount.dimensionless(0.2), List.of(), Amount.dimensionless(0.3)));
    }

    @Test
    void fraccionCoberturaYP2DebenSerAdimensionalesEntreCeroYUno() {
        assertThrows(IllegalArgumentException.class, () -> RiskEstimator.fromFailureMode(
                Amount.perHour(1e-6), Amount.dimensionless(1.2), List.of(), Amount.dimensionless(0.3)));
        assertThrows(IllegalArgumentException.class, () -> RiskEstimator.fromFailureMode(
                Amount.perHour(1e-6), Amount.perHour(0.2), List.of(), Amount.dimensionless(0.3)));
        assertThrows(IllegalArgumentException.class, () -> RiskEstimator.fromFailureMode(
                Amount.perHour(1e-6), Amount.dimensionless(0.2), List.of(Amount.dimensionless(1.5)),
                Amount.dimensionless(0.3)));
        assertThrows(IllegalArgumentException.class, () -> RiskEstimator.fromFailureMode(
                Amount.perHour(1e-6), Amount.dimensionless(0.2), List.of(), Amount.perHour(0.3)));
    }

    @Test
    void p1NoPuedeSerMayorQueUno() {
        assertThrows(IllegalArgumentException.class, () -> RiskEstimator.fromFailureMode(
                Amount.perHour(5.0), Amount.dimensionless(1.0), List.of(), Amount.dimensionless(1.0)));
    }

    @Test
    void desdeP1MultiplicaPorP2() {
        Estimate e = RiskEstimator.fromP1(Amount.perHour(1.7e-8), Amount.dimensionless(0.3));
        assertEquals(5.1e-9, e.pTotal(), 1e-21);
    }

    @Test
    void soloProbabilidadTotalExigeJustificacion() {
        assertThrows(IllegalArgumentException.class,
                () -> RiskEstimator.fromTotalOnly(Amount.perHour(1e-7), "  "));
        assertThrows(IllegalArgumentException.class,
                () -> RiskEstimator.fromTotalOnly(Amount.perHour(1e-7), null));
        Estimate e = RiskEstimator.fromTotalOnly(Amount.perHour(1e-7), "Dato de campo agregado del fabricante");
        assertNull(e.p1());
        assertNull(e.p2());
        assertEquals(1e-7, e.pTotal(), 1e-19);
        assertEquals("Dato de campo agregado del fabricante", e.justification());
    }
}
