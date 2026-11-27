package org.example.iso;

import org.example.iso.FaultTree.Basic;
import org.example.iso.FaultTree.Gate;
import org.example.iso.FaultTree.GateNode;
import org.example.iso.FaultTree.Node;
import org.example.iso.FaultTree.Result;
import org.example.iso.RiskEstimator.Estimate;
import org.example.iso.RiskMatrix.Verdict;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Flujo completo con la bomba de infusión: FMEA → FTA → P1 × P2 → matriz. Valores ilustrativos. */
class BombaInfusionIsoTest {

    @Test
    void delModoDeFallaAlVeredicto() {
        RiskMatrix matrix = RiskMatrixTest.matrix();

        // 1. FMEA: modo de falla "pierde pulsos" → 1e-6/h × 0.2 = 2e-7/h
        Estimate fmea = RiskEstimator.fromFailureMode(Amount.perHour(1e-6), Amount.dimensionless(0.2),
                List.of(), Amount.dimensionless(0.3));
        assertEquals(2e-7, fmea.p1(), 1e-19);

        // 2. FTA: la rama A hereda esa tasa; la barrera (cobertura 0.99) entra como "la verificación falla = 0.01"
        Node tree = new GateNode("sobredosis", "Sobredosis", Gate.OR, List.of(
                new GateNode("rama_a", "Encoder pierde pulsos y la verificación falla", Gate.AND, List.of(
                        new Basic("encoder", "Encoder pierde pulsos", Amount.perHour(fmea.p1())),
                        new Basic("verif", "La verificación falla", Amount.dimensionless(0.01)))),
                new Basic("calculo", "Error de cálculo de dosis", Amount.perHour(5e-9)),
                new Basic("operador", "Operador programa mal la tasa", Amount.perHour(1e-8))));
        Result top = FaultTree.of(tree).evaluate();
        assertEquals(1.7e-8, top.probability(), 1e-15);

        // 3. ISO 14971: P = P1 × P2 con P2 = 0.3
        Estimate est = RiskEstimator.fromP1(new Amount(top.probability(), top.unit()), Amount.dimensionless(0.3));
        // El valor exacto es 5.09999999976e-9: la compuerta O descuenta las intersecciones (≈8e-17 en P1).
        assertEquals(5.1e-9, est.pTotal(), 1e-15);

        // 4. Evaluación: severidad Catastrófica (5) con probabilidad Improbable → Tolerable (requiere control)
        Verdict v = matrix.evaluate(5, est.pTotal(), est.exposureUnit());
        assertEquals("Tolerable", v.riskLevel());
        assertFalse(v.acceptable());

        // 5. Trazabilidad: los puntos únicos de falla son las dos ramas sin barrera
        assertEquals(List.of("operador", "calculo"), top.singlePointsOfFailure());
    }
}
