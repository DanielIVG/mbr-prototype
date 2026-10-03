package org.example.rpn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Las escalas (1 a 10) y las categorías (A, B, C) son datos de PRUEBA para verificar
 * el motor. No son los valores del caso de estudio.
 */
class RpnCalculatorTest {

    private static List<ScaleLevel> levels() {
        List<ScaleLevel> list = new ArrayList<>();
        for (FactorType type : FactorType.values()) {
            for (int v = 1; v <= 10; v++) {
                list.add(new ScaleLevel(type, v, type.name().toLowerCase() + " " + v));
            }
        }
        return list;
    }

    private static List<PriorityCategory> categories() {
        return List.of(
                new PriorityCategory("A", 1, 100, "prueba"),
                new PriorityCategory("B", 101, 300, "prueba"),
                new PriorityCategory("C", 301, 1000, "prueba"));
    }

    private final RpnCalculator calculator = new RpnCalculator(levels(), categories());

    @ParameterizedTest(name = "S={0}, O={1}, D={2} → RPN={3}, categoría {4}")
    @CsvSource({
            "1,1,1,1,A",
            "5,4,3,60,A",
            "5,5,4,100,A",
            "10,10,1,100,A",
            "3,5,7,105,B",
            "5,5,5,125,B",
            "4,9,8,288,B",
            "10,10,3,300,B",
            "8,5,8,320,C",
            "10,10,4,400,C",
            "10,10,10,1000,C"
    })
    void calculaYClasifica(int s, int o, int d, int expectedRpn, String expectedCategory) {
        var result = calculator.evaluate(s, o, d);
        assertEquals(expectedRpn, result.rpn());
        assertEquals(expectedCategory, result.category().name());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 11, -1, 100})
    void rechazaValoresFueraDeLaEscala(int invalid) {
        assertThrows(IllegalArgumentException.class, () -> calculator.evaluate(invalid, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> calculator.evaluate(1, invalid, 1));
        assertThrows(IllegalArgumentException.class, () -> calculator.evaluate(1, 1, invalid));
    }

    @Test
    void elResultadoMuestraEntradasDescriptoresReglaYRango() {
        var r = calculator.evaluate(5, 4, 3);
        assertEquals("severidad 5", r.severityDescriptor());
        assertEquals("ocurrencia 4", r.occurrenceDescriptor());
        assertEquals("deteccion 3", r.detectionDescriptor());
        assertEquals("RPN = S × O × D = 5 × 4 × 3 = 60", r.rule());
        assertEquals(1, r.category().minRpn());
        assertEquals(100, r.category().maxRpn());
    }

    @Test
    void mismaEntradaMismoResultado() {
        assertEquals(calculator.evaluate(7, 3, 9), calculator.evaluate(7, 3, 9));
    }

    @Test
    void elOrdenDeLasCategoriasNoImporta() {
        var shuffled = new RpnCalculator(levels(), List.of(
                new PriorityCategory("C", 301, 1000, "prueba"),
                new PriorityCategory("A", 1, 100, "prueba"),
                new PriorityCategory("B", 101, 300, "prueba")));
        assertEquals("B", shuffled.evaluate(3, 5, 7).category().name());
    }

    @Test
    void sinCategoriasCalculaPeroNoClasifica() {
        var r = new RpnCalculator(levels(), List.of()).evaluate(5, 4, 3);
        assertEquals(60, r.rpn());
        assertNull(r.category());
    }

    @Test
    void rechazaCategoriasConHueco() {
        var ex = assertThrows(IllegalArgumentException.class, () -> new RpnCalculator(levels(), List.of(
                new PriorityCategory("A", 1, 100, "prueba"),
                new PriorityCategory("B", 102, 1000, "prueba"))));
        assertTrue(ex.getMessage().contains("hueco"));
    }

    @Test
    void rechazaCategoriasTraslapadas() {
        var ex = assertThrows(IllegalArgumentException.class, () -> new RpnCalculator(levels(), List.of(
                new PriorityCategory("A", 1, 100, "prueba"),
                new PriorityCategory("B", 100, 1000, "prueba"))));
        assertTrue(ex.getMessage().contains("traslapan"));
    }

    @Test
    void rechazaCategoriasQueNoCubrenElRangoCompleto() {
        assertThrows(IllegalArgumentException.class, () -> new RpnCalculator(levels(), List.of(
                new PriorityCategory("A", 1, 100, "prueba"),
                new PriorityCategory("B", 101, 999, "prueba"))));
        assertThrows(IllegalArgumentException.class, () -> new RpnCalculator(levels(), List.of(
                new PriorityCategory("A", 2, 1000, "prueba"))));
    }

    @Test
    void rechazaCategoriaSinJustificacion() {
        assertThrows(IllegalArgumentException.class,
                () -> new PriorityCategory("A", 1, 100, " "));
    }

    @Test
    void rechazaEscalasIncompletasOConNivelesDuplicados() {
        var sinDeteccion = levels().stream()
                .filter(l -> l.factor() != FactorType.DETECCION).toList();
        assertThrows(IllegalArgumentException.class,
                () -> new RpnCalculator(sinDeteccion, List.of()));

        var duplicados = new ArrayList<>(levels());
        duplicados.add(new ScaleLevel(FactorType.SEVERIDAD, 5, "repetido"));
        assertThrows(IllegalArgumentException.class,
                () -> new RpnCalculator(duplicados, List.of()));
    }
}