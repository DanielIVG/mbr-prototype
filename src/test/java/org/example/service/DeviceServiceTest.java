package org.example.service;

import org.example.config.Database;
import org.example.config.HibernateConfig;
import org.example.repository.DeviceRepository;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DeviceServiceTest {

    @TempDir
    Path tmp;

    SessionFactory sf;
    DeviceService service;

    @BeforeEach
    void setUp() {
        String url = Database.jdbcUrl(tmp.resolve("test.db"));
        Database.migrate(url);
        sf = HibernateConfig.build(url);
        service = new DeviceService(new DeviceRepository(sf));
    }

    @AfterEach
    void tearDown() {
        sf.close();
    }

    @Test
    void creaYLeeUnDispositivo() {
        var d = service.create("Bomba de infusión", "Administrar fluidos", null);
        assertNotNull(d.getId());
        assertEquals("Bomba de infusión", service.find(d.getId()).orElseThrow().getName());
    }

    @Test
    void rechazaNombreVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create("  ", "Uso", null));
    }

    @Test
    void actualizaCampos() {
        var d = service.create("A", "Uso A", null);
        service.update(d.getId(), "B", "Uso B", "Mal uso");
        var leido = service.find(d.getId()).orElseThrow();
        assertEquals("B", leido.getName());
        assertEquals("Uso B", leido.getIntendedUse());
    }

    @Test
    void desactivarNoBorraPeroLoSacaDelListado() {
        var d = service.create("A", "Uso A", null);
        service.deactivate(d.getId());
        assertTrue(service.find(d.getId()).isPresent());
        assertTrue(service.listActive().isEmpty());
    }
}