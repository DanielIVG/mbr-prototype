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
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class FmeaServiceTest {

    @TempDir
    Path tmp;

    SessionFactory sf;
    Services services;
    FmeaConfigService config;
    FmeaService fmea;
    Long deviceId;
    Long harmId;
    Long failureModeId;

    @BeforeEach
    void setUp() {
        String url = Database.jdbcUrl(tmp.resolve("test.db"));
        Database.migrate(url);
        sf = HibernateConfig.build(url);
        services = Services.create(sf);
        config = services.fmeaConfig();
        fmea = services.fmea();
        deviceId = services.devices().create("Bomba", "Infundir", null).getId();
        harmId = services.chains().createChain(deviceId, "Sobredosis", null,
                "Secuencia", "Situación", "Daño", null).harm().getId();
        failureModeId = fmea.createFailureMode(deviceId, "El motor no se detiene").getId();
    }

    @AfterEach
    void tearDown() {
        sf.close();
    }

    private void configureScales() {
        for (FactorType type : FactorType.values()) {
            for (int v = 1; v <= 10; v++) {
                config.addScaleLevel(type, v, type.name().toLowerCase() + " " + v);
            }
        }
    }

    private void configureCategories() {
        config.addCategory("A", 1, 100, "prueba");
        config.addCategory("B", 101, 300, "prueba");
        config.addCategory("C", 301, 1000, "prueba");
    }

    private void configure() {
        configureScales();
        configureCategories();
    }

    private org.example.model.FmeaView analysis(int s, int o, int d) {
        return fmea.createAnalysis(harmId, failureModeId, "Sobredosis", "Falla del sensor",
                "Alarma", "Válvula", "Arney 2009", s, o, d);
    }

    @Test
    void creaElAnalisisCalculaYClasifica() {
        configure();
        var view = analysis(5, 4, 3);
        assertNotNull(view.analysis().getId());
        assertEquals("FMEA", view.analysis().getAnalysisType());
        assertEquals(60, view.factor().getRpn());
        assertTrue(view.factor().isLatest());
        assertEquals("A", view.result().category().name());
        assertEquals("severidad 5", view.result().severityDescriptor());
        assertEquals("RPN = S × O × D = 5 × 4 × 3 = 60", view.result().rule());

        var found = fmea.find(view.analysis().getId()).orElseThrow();
        assertEquals(60, found.result().rpn());
        assertEquals(1, fmea.listByHarm(harmId).size());
    }

    @Test
    void valorFueraDeEscalaSeRechazaYNoSePersisteNada() {
        configure();
        assertThrows(IllegalArgumentException.class, () -> analysis(11, 4, 3));
        assertTrue(fmea.listByHarm(harmId).isEmpty());
    }

    @Test
    void sinEscalasNoSePuedeAnalizar() {
        assertThrows(ConflictException.class, () -> analysis(5, 4, 3));
    }

    @Test
    void categoriasConHuecoImpidenElCalculo() {
        configureScales();
        config.addCategory("A", 1, 100, "prueba");
        config.addCategory("B", 102, 1000, "prueba");
        var ex = assertThrows(ConflictException.class, () -> analysis(5, 4, 3));
        assertTrue(ex.getMessage().contains("hueco"));
    }

    @Test
    void sinCategoriasCalculaPeroNoClasifica() {
        configureScales();
        var view = analysis(5, 4, 3);
        assertEquals(60, view.factor().getRpn());
        assertNull(view.result().category());
        assertNull(view.factor().getPriorityCategoryId());
    }

    @Test
    void reevaluarConservaElHistorialYSoloUnaFilaEsVigente() {
        configure();
        var created = analysis(5, 4, 3);
        Long id = created.analysis().getId();

        var second = fmea.reevaluate(id, 5, 5, 5);
        assertEquals(125, second.result().rpn());
        assertEquals("B", second.result().category().name());

        fmea.reevaluate(id, 2, 2, 2);

        var history = fmea.history(id);
        assertEquals(3, history.size());
        assertEquals(60, history.get(0).getRpn());
        assertEquals(125, history.get(1).getRpn());
        assertEquals(8, history.get(2).getRpn());
        assertEquals(1, history.stream().filter(f -> f.isLatest()).count());
        assertTrue(history.get(2).isLatest());
        assertEquals(8, fmea.find(id).orElseThrow().result().rpn());
    }

    @Test
    void mismosValoresNoCreanUnaFilaNueva() {
        configure();
        var created = analysis(5, 4, 3);
        fmea.reevaluate(created.analysis().getId(), 5, 4, 3);
        assertEquals(1, fmea.history(created.analysis().getId()).size());
    }

    @Test
    void elRecalculoTardaMenosDeUnSegundo() {
        configure();
        Long id = analysis(5, 4, 3).analysis().getId();
        long start = System.nanoTime();
        fmea.reevaluate(id, 9, 9, 9);
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertTrue(millis < 1000, "El recálculo tardó " + millis + " ms");
    }

    @Test
    void noSeDuplicanNivelesDeLaEscala() {
        config.addScaleLevel(FactorType.SEVERIDAD, 1, "bajo");
        assertThrows(ConflictException.class,
                () -> config.addScaleLevel(FactorType.SEVERIDAD, 1, "otro"));
    }

    @Test
    void validaDanoModoDeFalloYEfecto() {
        configure();
        assertThrows(NoSuchElementException.class, () -> fmea.createAnalysis(
                999L, failureModeId, "Efecto", null, null, null, null, 5, 4, 3));
        assertThrows(NoSuchElementException.class, () -> fmea.createAnalysis(
                harmId, 999L, "Efecto", null, null, null, null, 5, 4, 3));
        assertThrows(IllegalArgumentException.class, () -> fmea.createAnalysis(
                harmId, failureModeId, " ", null, null, null, null, 5, 4, 3));
    }

    @Test
    void unModoDeFalloImpideEliminarElComponente() {
        var component = services.components()
                .create(deviceId, null, "Motor", null, false, null);
        fmea.createFailureMode(component.getId(), "No gira");
        assertThrows(ConflictException.class,
                () -> services.components().delete(component.getId()));
    }
}