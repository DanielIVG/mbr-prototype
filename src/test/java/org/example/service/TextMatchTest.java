package org.example.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextMatchTest {

    @Test
    void normalizaMayusculasTildesYSignos() {
        assertEquals("cable grip tie", TextMatch.normalize("  Cable-Grip/Tie "));
        assertEquals("valvula", TextMatch.normalize("VÁLVULA"));
        assertEquals("", TextMatch.normalize(null));
    }

    @Test
    void igualesTrasNormalizarValenUno() {
        assertEquals(1.0, TextMatch.similarity("Subcomponente A", "subcomponente  a"));
        assertEquals(1.0, TextMatch.similarity("Válvula", "valvula"));
    }

    @Test
    void contenerComoPalabraValeCeroNueve() {
        assertEquals(0.9, TextMatch.similarity("cable", "Cable Grip/Tie"));
        assertEquals(0.9, TextMatch.similarity("Bomba de infusión peristáltica", "bomba de infusión"));
    }

    @Test
    void erroresDeEscrituraPuntuanAlto_losDistintosBajo() {
        assertTrue(TextMatch.similarity("compresor", "Compressor") > 0.7);
        assertTrue(TextMatch.similarity("Motor", "Alarm, Audible") < 0.3);
    }

    @Test
    void vacioNoCoincideConNada() {
        assertEquals(0.0, TextMatch.similarity("", "algo"));
        assertEquals(0.0, TextMatch.similarity("algo", null));
    }
}
