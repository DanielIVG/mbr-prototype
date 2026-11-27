package org.example.iso;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AmountTest {

    @Test
    void normalizaLasFormasDeEscribirPorHora() {
        assertEquals("por hora", new Amount(1, "1/h").canonical().unit());
        assertEquals("por hora", new Amount(1, "/h").canonical().unit());
        assertEquals("por hora", new Amount(1, "por hora").canonical().unit());
        assertEquals("por hora", new Amount(1, "Por  Horas").canonical().unit());
        assertEquals("por ciclo", new Amount(1, "1/ciclo").canonical().unit());
    }

    @Test
    void convierteFitAPorHora() {
        Amount a = new Amount(5000, "FIT").canonical();
        assertEquals("por hora", a.unit());
        assertEquals(5e-6, a.value(), 1e-18);
    }

    @Test
    void reconoceLoAdimensional() {
        assertTrue(Amount.dimensionless(0.2).isDimensionless());
        assertFalse(Amount.perHour(1e-6).isDimensionless());
    }

    @Test
    void rechazaUnidadesNoReconocidas() {
        assertThrows(IllegalArgumentException.class, () -> new Amount(1, "kg").canonical());
        assertThrows(IllegalArgumentException.class, () -> new Amount(1, "1/").canonical());
    }

    @Test
    void rechazaValoresInvalidosYUnidadVacia() {
        assertThrows(IllegalArgumentException.class, () -> new Amount(-1, "1/h"));
        assertThrows(IllegalArgumentException.class, () -> new Amount(Double.NaN, "1/h"));
        assertThrows(IllegalArgumentException.class, () -> new Amount(1, "  "));
        assertThrows(IllegalArgumentException.class, () -> new Amount(1, null));
    }
}
