package org.example.client;

import org.example.api.ApiServer;
import org.example.config.Database;
import org.example.config.HibernateConfig;
import org.example.config.Services;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ApiClientTest {

    @TempDir
    Path tmp;

    SessionFactory sf;
    ApiServer server;
    ApiClient client;

    @BeforeEach
    void up() {
        String url = Database.jdbcUrl(tmp.resolve("test.db"));
        Database.migrate(url);
        sf = HibernateConfig.build(url);
        server = new ApiServer(Services.create(sf)).start();
        client = new ApiClient("http://127.0.0.1:" + server.port());
    }

    @AfterEach
    void down() {
        server.stop();
        sf.close();
    }

    @Test
    void actualizaLosDatosDeUnDispositivo() {
        var created = client.createDevice("Bomba", "Infundir", null);
        var updated = client.updateDevice(created.id(), "Bomba BI-200", "Infundir fármacos", "Dosis mal programada");
        assertEquals(created.id(), updated.id());
        assertEquals("Bomba BI-200", updated.name());
        assertEquals("Dosis mal programada", updated.reasonablyForeseeableMisuse());
        assertEquals("Bomba BI-200", client.listDevices().get(0).name());
    }

    @Test
    void creaListaYDesactiva() {
        var created = client.createDevice("Bomba", "Infundir", null);
        assertNotNull(created.id());
        assertEquals(1, client.listDevices().size());

        client.deactivateDevice(created.id());
        assertTrue(client.listDevices().isEmpty());
    }

    @Test
    void errorDeValidacionLlegaComoMensaje() {
        var ex = assertThrows(ApiException.class,
                () -> client.createDevice("", "Uso", null));
        assertTrue(ex.getMessage().contains("nombre"));
    }
}