package org.example.iso;

import org.example.iso.RiskAssessor.Input;
import org.example.iso.RiskAssessor.Mode;
import org.example.iso.RiskAssessor.Q;
import org.example.iso.Sensitivity.Report;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskAssessorTest {

    /** Bomba de infusión: tasa 1e-6/h, fracción 0.2, una barrera de 99 %, P2 0.3, severidad 5. */
    private static Input pump() {
        return new Input(Mode.FAILURE_MODE, 5, List.of(
                new Q(RiskAssessor.RATE, Amount.perHour(1e-6), 1e-6, 1e-4),
                new Q(RiskAssessor.FRACTION, Amount.dimensionless(0.2), 0.05, 0.5),
                new Q(RiskAssessor.BARRIER, Amount.dimensionless(0.99), null, null),
                new Q(RiskAssessor.P2, Amount.dimensionless(0.3), 0.05, 1.0)), null);
    }

    @Test
    void calculaElVeredictoDeLaBomba() {
        var a = RiskAssessor.assess(RiskMatrixTest.matrix(), pump());
        assertEquals(6e-10, a.estimate().pTotal(), 1e-20);    // 1e-6 × 0.2 × 0.01 × 0.3
        assertEquals("Tolerable", a.verdict().riskLevel());   // severidad 5, nivel 1
    }

    @Test
    void laSensibilidadDetectaSoloLaSeveridadYLosExtremosConjuntos() {
        Report r = RiskAssessor.sensitivity(RiskMatrixTest.matrix(), pump());
        assertEquals(List.of("Severidad"), r.decisive());
        assertFalse(r.cornerChanges().isEmpty());             // tasa 1e-4, fracción 0.5, P2 1.0 → nivel 2
    }

    @Test
    void sinRangosSoloVaLaSeveridad() {
        Input in = new Input(Mode.P1_P2, 4, List.of(
                new Q(RiskAssessor.P1, Amount.perHour(1e-8), null, null),
                new Q(RiskAssessor.P2, Amount.dimensionless(0.5), null, null)), null);
        Report r = RiskAssessor.sensitivity(RiskMatrixTest.matrix(), in);
        assertEquals(1, r.results().size());
        assertEquals("Aceptable", r.baseline().riskLevel());
    }

    @Test
    void soloProbabilidadTotalExigeJustificacion() {
        Input sin = new Input(Mode.TOTAL_ONLY, 3, List.of(
                new Q(RiskAssessor.TOTAL, Amount.perHour(1e-6), null, null)), " ");
        assertThrows(IllegalArgumentException.class, () -> RiskAssessor.assess(RiskMatrixTest.matrix(), sin));
        Input con = new Input(Mode.TOTAL_ONLY, 3, List.of(
                new Q(RiskAssessor.TOTAL, Amount.perHour(1e-6), null, null)), "Dato de campo agregado");
        assertEquals(2, RiskAssessor.assess(RiskMatrixTest.matrix(), con).verdict().probability().value());   // 1e-6 ∈ [1e-7, 1e-5)
    }

    @Test
    void rechazaUnidadDistintaALaDeLaEscala() {
        Input in = new Input(Mode.P1_P2, 3, List.of(
                new Q(RiskAssessor.P1, new Amount(1e-6, "por ciclo"), null, null),
                new Q(RiskAssessor.P2, Amount.dimensionless(0.5), null, null)), null);
        assertThrows(IllegalArgumentException.class, () -> RiskAssessor.assess(RiskMatrixTest.matrix(), in));
    }

    @Test
    void rechazaFaltaDeCantidades() {
        Input in = new Input(Mode.P1_P2, 3, List.of(
                new Q(RiskAssessor.P1, Amount.perHour(1e-6), null, null)), null);
        assertThrows(IllegalArgumentException.class, () -> RiskAssessor.assess(RiskMatrixTest.matrix(), in));
    }
}
