package org.example.client;

import org.example.api.ApiServer;
import org.example.api.TraceabilityDtos.RequirementRequest;
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

/** Flujo que sigue la pantalla de trazabilidad, pero a través del cliente HTTP de la interfaz. */
class TraceabilityClientTest {

    @TempDir
    Path tmp;

    SessionFactory sf;
    ApiServer server;
    ApiClient client;
    long deviceId;
    long motorId;
    long harmId;
    long controlId;

    @BeforeEach
    void up() {
        String url = Database.jdbcUrl(tmp.resolve("test.db"));
        Database.migrate(url);
        sf = HibernateConfig.build(url);
        var services = Services.create(sf);
        deviceId = services.devices().create("Bomba", "Infundir", null).getId();
        motorId = services.components().create(deviceId, null, "Motor", null, false, null).getId();
        harmId = services.chains().createChain(motorId, "Sobredosis", null, "El motor sigue girando",
                "El paciente recibe exceso", "Daño por sobredosis", null).harm().getId();
        controlId = services.assessment().addControl(harmId, "Alarma de flujo excesivo", null);
        server = new ApiServer(services).start();
        client = new ApiClient("http://127.0.0.1:" + server.port());
    }

    @AfterEach
    void down() {
        server.stop();
        sf.close();
    }

    @Test
    void flujoCompletoRequisitoEnlaceMatrizYBaja() {
        var created = client.createRequirement(new RequirementRequest(deviceId, "REQ-001",
                "Alarmar flujo excesivo", "SEGURIDAD", "IEC 60601-2-24"));
        assertNotNull(created.id());
        assertEquals(1, client.listRequirements(deviceId).size());

        var updated = client.updateRequirement(created.id(), new RequirementRequest(deviceId, "REQ-001",
                "Alarmar y detener el flujo excesivo", "SEGURIDAD", null));
        assertEquals("Alarmar y detener el flujo excesivo", updated.description());

        assertEquals(1, client.listDeviceControls(deviceId).size());
        client.linkRequirement(created.id(), controlId);
        var matrix = client.traceability(deviceId);
        assertEquals(1, matrix.rows().size());
        assertEquals("Alarma de flujo excesivo", matrix.rows().get(0).controlDescription());

        client.unlinkRequirement(created.id(), controlId);
        assertTrue(client.traceability(deviceId).rows().isEmpty());

        client.deactivateRequirement(created.id());
        assertTrue(client.listRequirements(deviceId).isEmpty());
    }

    @Test
    void consultasDeTrazabilidadYDashboardVacio() {
        assertEquals("Motor", client.componentsOfHarm(harmId).get(0).name());
        assertEquals(harmId, client.harmsOfComponent(motorId).get(0).harmId());
        var dashboard = client.rpnDashboard(deviceId);
        assertTrue(dashboard.rows().isEmpty());
        assertNull(dashboard.maxRpn());
    }

    @Test
    void erroresDeLaApiLlegaranComoApiException() {
        var ex = assertThrows(ApiException.class, () -> client.createRequirement(
                new RequirementRequest(deviceId, "", "Texto", "SEGURIDAD", null)));
        assertTrue(ex.getMessage().contains("código"));
        assertThrows(ApiException.class, () -> client.linkRequirement(999999L, controlId));
    }
}
