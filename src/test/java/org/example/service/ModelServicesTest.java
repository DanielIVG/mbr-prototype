package org.example.service;

import org.example.config.Database;
import org.example.config.HibernateConfig;
import org.example.model.Component;
import org.example.model.DeviceFunction;
import org.example.model.Hazard;
import org.example.model.Interaction;
import org.example.repository.DeviceRepository;
import org.example.repository.ModelElementRepository;
import org.example.repository.ModelLinksRepository;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ModelServicesTest {

    @TempDir
    Path tmp;

    SessionFactory sf;
    DeviceService devices;
    ComponentService components;
    DeviceFunctionService functions;
    InteractionService interactions;
    Long deviceId;

    @BeforeEach
    void setUp() {
        String url = Database.jdbcUrl(tmp.resolve("test.db"));
        Database.migrate(url);
        sf = HibernateConfig.build(url);
        var links = new ModelLinksRepository(sf);
        devices = new DeviceService(new DeviceRepository(sf));
        components = new ComponentService(
                new ModelElementRepository<>(sf, Component.class), links, devices);
        functions = new DeviceFunctionService(
                new ModelElementRepository<>(sf, DeviceFunction.class), links, devices, components);
        interactions = new InteractionService(
                new ModelElementRepository<>(sf, Interaction.class), links, devices, components);
        deviceId = devices.create("Bomba", "Infundir", null).getId();
    }

    @AfterEach
    void tearDown() {
        sf.close();
    }

    private Component component(String name) {
        return components.create(deviceId, null, name, null, false, null);
    }

    private Long hazardOn(Long elementId) {
        return sf.fromTransaction(s -> {
            Hazard h = new Hazard();
            h.setModelElementId(elementId);
            h.setDescription("Peligro");
            s.persist(h);
            return h.getId();
        });
    }

    private String reviewStatus(Long hazardId) {
        return sf.fromTransaction(s -> s.find(Hazard.class, hazardId).getReviewStatus());
    }

    @Test
    void componenteEsencialExigeJustificacion() {
        assertThrows(IllegalArgumentException.class,
                () -> components.create(deviceId, null, "Motor", null, true, " "));
        var c = components.create(deviceId, null, "Motor", null, true, "Sin él no infunde");
        assertTrue(components.find(c.getId()).orElseThrow().isEssential());
    }

    @Test
    void jerarquiaCircularSeRechaza() {
        var a = component("A");
        var b = components.create(deviceId, a.getId(), "B", null, false, null);
        assertThrows(IllegalArgumentException.class,
                () -> components.update(a.getId(), b.getId(), "A", null, false, null));
    }

    @Test
    void interaccionExigeDosComponentes() {
        var a = component("A");
        var b = component("B");
        assertThrows(IllegalArgumentException.class,
                () -> interactions.create(deviceId, "Fuga", "Relevante", Set.of(a.getId())));
        var i = interactions.create(deviceId, "Fuga", "Relevante", Set.of(a.getId(), b.getId()));
        assertEquals(2, interactions.find(i.getId()).orElseThrow().getComponentIds().size());
    }

    @Test
    void funcionVinculaComponentesDelMismoDispositivo() {
        var a = component("A");
        var f = functions.create(deviceId, "Infundir", null, Set.of(a.getId()));
        assertEquals(Set.of(a.getId()), functions.find(f.getId()).orElseThrow().getComponentIds());
        assertThrows(IllegalArgumentException.class,
                () -> functions.create(deviceId, "Otra", null, Set.of(999L)));
    }

    @Test
    void modificarMarcaElRiesgoComoPendienteDeRevision() {
        var c = component("A");
        Long hazard = hazardOn(c.getId());
        assertNull(reviewStatus(hazard));
        components.update(c.getId(), null, "A2", null, false, null);
        assertEquals(ModelElementRepository.PENDING_REVIEW, reviewStatus(hazard));
    }

    @Test
    void conRiesgoAsociadoNoSeEliminaPeroSiSeDesactiva() {
        var c = component("A");
        hazardOn(c.getId());
        assertThrows(ConflictException.class, () -> components.delete(c.getId()));
        components.deactivate(c.getId());
        assertFalse(components.find(c.getId()).orElseThrow().isActive());
        assertTrue(components.listActive(deviceId).isEmpty());
    }

    @Test
    void componenteEsencialNoSeElimina() {
        var c = components.create(deviceId, null, "Motor", null, true, "Imprescindible");
        assertThrows(ConflictException.class, () -> components.delete(c.getId()));
    }

    @Test
    void componenteVinculadoNoSeElimina() {
        var a = component("A");
        functions.create(deviceId, "Infundir", null, Set.of(a.getId()));
        assertThrows(ConflictException.class, () -> components.delete(a.getId()));
    }

    @Test
    void sinReferenciasSeEliminaFisicamente() {
        var c = component("A");
        components.delete(c.getId());
        assertTrue(components.find(c.getId()).isEmpty());
    }

    @Test
    void funcionDesactivadaNoApareceEnElListado() {
        var f = functions.create(deviceId, "Infundir", null, Set.of());
        assertEquals(1, functions.listActive(deviceId).size());
        functions.deactivate(f.getId());
        assertTrue(functions.listActive(deviceId).isEmpty());
    }
}