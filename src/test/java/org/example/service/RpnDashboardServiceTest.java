package org.example.service;

import org.example.config.Database;
import org.example.config.HibernateConfig;
import org.example.config.Services;
import org.example.rpn.FactorType;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RpnDashboardServiceTest {

    @TempDir
    Path tmp;

    SessionFactory sf;
    Services services;
    Long deviceId;
    Long motorId;
    Long harmId;

    @BeforeEach
    void setUp() {
        String url = Database.jdbcUrl(tmp.resolve("test.db"));
        Database.migrate(url);
        sf = HibernateConfig.build(url);
        services = Services.create(sf);
        deviceId = services.devices().create("Bomba", "Infundir", null).getId();
        motorId = services.components().create(deviceId, null, "Motor", null, false, null).getId();
        harmId = services.chains().createChain(motorId, "Sobredosis", null, "Secuencia", "Situación", "Daño", null)
                .harm().getId();
    }

    @AfterEach
    void tearDown() {
        sf.close();
    }

    private void configure() {
        for (FactorType type : FactorType.values()) {
            for (int v = 1; v <= 10; v++) {
                services.fmeaConfig().addScaleLevel(type, v, type.name().toLowerCase() + " " + v);
            }
        }
        services.fmeaConfig().addCategory("Baja", 1, 100, "prueba");
        services.fmeaConfig().addCategory("Media", 101, 300, "prueba");
        services.fmeaConfig().addCategory("Alta", 301, 1000, "prueba");
    }

    private void analysis(Long element, String failureMode, int s, int o, int d) {
        Long fm = services.fmea().createFailureMode(element, failureMode).getId();
        services.fmea().createAnalysis(harmId, fm, "Efecto", null, null, null, null, s, o, d);
    }

    @Test
    void sinAnalisisElDashboardEstaVacio() {
        var d = services.dashboard().dashboard(deviceId);
        assertTrue(d.rows().isEmpty());
        assertNull(d.maxRpn());
        assertNull(d.averageRpn());
        assertEquals(0, d.uncategorized());
    }

    @Test
    void ordenaPorRpnDescendenteYCuentaPorCategoria() {
        configure();
        analysis(motorId, "El motor no se detiene", 2, 3, 4);      // 24 → Baja
        analysis(motorId, "El sensor se descalibra", 9, 8, 7);     // 504 → Alta
        analysis(motorId, "Se agota la batería", 5, 5, 5);         // 125 → Media

        var d = services.dashboard().dashboard(deviceId);
        assertEquals(List.of(504, 125, 24), d.rows().stream().map(r -> r.rpn()).toList());
        assertEquals(List.of("Alta", "Media", "Baja"), d.rows().stream().map(r -> r.category()).toList());
        assertEquals(504, d.maxRpn());
        assertEquals(217.7, d.averageRpn());                        // (504 + 125 + 24) / 3 = 217.67
        assertEquals(0, d.uncategorized());
        assertEquals(List.of("Baja", "Media", "Alta"), d.categories().stream().map(c -> c.name()).toList());
        assertEquals(List.of(1, 1, 1), d.categories().stream().map(c -> c.count()).toList());
    }

    @Test
    void cadaFilaTraeDanoModoDeFalloComponenteYFactores() {
        configure();
        analysis(motorId, "El motor no se detiene", 2, 3, 4);
        var row = services.dashboard().dashboard(deviceId).rows().get(0);
        assertEquals("Daño", row.harm());
        assertEquals("Sobredosis", row.hazard());
        assertEquals("El motor no se detiene", row.failureMode());
        assertEquals("Motor", row.component());
        assertEquals(2, row.severity());
        assertEquals(3, row.occurrence());
        assertEquals(4, row.detection());
        assertNull(row.isoRiskLevel());        // sin estimación ISO 14971 todavía
        assertNull(row.isoAcceptable());
    }

    @Test
    void laReevaluacionActualizaElRpnVigenteYNoDuplicaLaFila() {
        configure();
        Long fm = services.fmea().createFailureMode(motorId, "El motor no se detiene").getId();
        var created = services.fmea().createAnalysis(harmId, fm, "Efecto", null, null, null, null, 2, 3, 4);
        services.fmea().reevaluate(created.analysis().getId(), 2, 3, 2);
        var d = services.dashboard().dashboard(deviceId);
        assertEquals(1, d.rows().size());
        assertEquals(12, d.rows().get(0).rpn());
    }

    @Test
    void noMezclaAnalisisDeOtroDispositivo() {
        configure();
        analysis(motorId, "El motor no se detiene", 2, 3, 4);
        Long other = services.devices().create("Monitor", "Medir", null).getId();
        assertTrue(services.dashboard().dashboard(other).rows().isEmpty());
        assertEquals(1, services.dashboard().dashboard(deviceId).rows().size());
    }

    @Test
    void sinCategoriasLasFilasQuedanSinCategoria() {
        for (FactorType type : FactorType.values()) {
            for (int v = 1; v <= 10; v++) {
                services.fmeaConfig().addScaleLevel(type, v, "n" + v);
            }
        }
        analysis(motorId, "El motor no se detiene", 2, 3, 4);
        var d = services.dashboard().dashboard(deviceId);
        assertEquals(1, d.uncategorized());
        assertTrue(d.categories().isEmpty());
        assertNull(d.rows().get(0).category());
    }

    @Test
    void dispositivoInexistenteSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> services.dashboard().dashboard(999999L));
        assertThrows(IllegalArgumentException.class, () -> services.dashboard().dashboard(null));
    }
}
