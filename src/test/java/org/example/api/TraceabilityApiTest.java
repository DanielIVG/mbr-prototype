package org.example.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.Database;
import org.example.config.HibernateConfig;
import org.example.config.Services;
import org.example.rpn.FactorType;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TraceabilityApiTest {

    @TempDir
    Path tmp;

    SessionFactory sf;
    Services services;
    ApiServer api;
    long deviceId;
    long motorId;
    long harmId;
    long controlId;
    final HttpClient http = HttpClient.newHttpClient();
    final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void up() {
        String url = Database.jdbcUrl(tmp.resolve("test.db"));
        Database.migrate(url);
        sf = HibernateConfig.build(url);
        services = Services.create(sf);
        deviceId = services.devices().create("Bomba", "Infundir", null).getId();
        motorId = services.components().create(deviceId, null, "Motor", null, false, null).getId();
        harmId = services.chains().createChain(motorId, "Sobredosis", null, "El motor sigue girando",
                "El paciente recibe exceso", "Daño por sobredosis", null).harm().getId();
        controlId = services.assessment().addControl(harmId, "Alarma de flujo excesivo", null);
        api = new ApiServer(services).start();
    }

    @AfterEach
    void down() {
        api.stop();
        sf.close();
    }

    private HttpResponse<String> call(String method, String path, Object body) throws Exception {
        var publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body));
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + api.port() + path))
                .header("Content-Type", "application/json")
                .method(method, publisher)
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private long createRequirement(String code) throws Exception {
        var response = call("POST", "/requirements", Map.of("deviceId", deviceId, "code", code,
                "description", "Alarmar flujo excesivo", "type", "SEGURIDAD"));
        assertEquals(201, response.statusCode());
        return mapper.readTree(response.body()).get("id").asLong();
    }

    @Test
    void creaListaYActualizaRequisitos() throws Exception {
        long id = createRequirement("REQ-001");
        var list = mapper.readTree(call("GET", "/requirements?deviceId=" + deviceId, null).body());
        assertEquals(1, list.size());
        assertEquals("REQ-001", list.get(0).get("code").asText());

        var put = call("PUT", "/requirements/" + id, Map.of("deviceId", deviceId, "code", "REQ-001",
                "description", "Texto nuevo", "type", "funcional"));
        assertEquals(200, put.statusCode());
        assertEquals("FUNCIONAL", mapper.readTree(put.body()).get("type").asText());
    }

    @Test
    void validacionesDan400Conflicto409YNoEncontrado404() throws Exception {
        assertEquals(400, call("POST", "/requirements", Map.of("deviceId", deviceId, "code", "",
                "description", "x", "type", "SEGURIDAD")).statusCode());
        assertEquals(400, call("GET", "/requirements", null).statusCode());          // falta deviceId
        createRequirement("REQ-001");
        assertEquals(409, call("POST", "/requirements", Map.of("deviceId", deviceId, "code", "REQ-001",
                "description", "x", "type", "SEGURIDAD")).statusCode());
        assertEquals(404, call("PUT", "/requirements/999999", Map.of("deviceId", deviceId, "code", "R",
                "description", "x", "type", "SEGURIDAD")).statusCode());
        assertEquals(400, call("GET", "/devices/999999/traceability", null).statusCode());
    }

    @Test
    void enlazaYLaMatrizMuestraElCaminoCompleto() throws Exception {
        long id = createRequirement("REQ-001");
        services.assessment().addVerification(controlId, "Ensayo en banco", true, true, null);

        assertEquals(201, call("POST", "/requirements/" + id + "/controls", Map.of("controlId", controlId)).statusCode());
        assertEquals(409, call("POST", "/requirements/" + id + "/controls", Map.of("controlId", controlId)).statusCode());
        assertEquals(404, call("POST", "/requirements/" + id + "/controls", Map.of("controlId", 999999)).statusCode());

        JsonNode matrix = mapper.readTree(call("GET", "/devices/" + deviceId + "/traceability", null).body());
        assertEquals(1, matrix.get("rows").size());
        JsonNode row = matrix.get("rows").get(0);
        assertEquals("REQ-001", row.get("requirementCode").asText());
        assertEquals("Daño por sobredosis", row.get("harmDescription").asText());
        assertEquals(1, row.get("verifications").asInt());
        assertEquals(1, matrix.get("summary").get("requirementsWithControl").asInt());
        assertEquals(1, matrix.get("summary").get("controlsVerified").asInt());
        assertEquals(0, matrix.get("requirementsWithoutControl").size());

        var controls = mapper.readTree(call("GET", "/devices/" + deviceId + "/controls", null).body());
        assertEquals(1, controls.get(0).get("requirements").asInt());
    }

    @Test
    void desenlazaYDesactiva() throws Exception {
        long id = createRequirement("REQ-001");
        call("POST", "/requirements/" + id + "/controls", Map.of("controlId", controlId));
        assertEquals(204, call("DELETE", "/requirements/" + id + "/controls/" + controlId, null).statusCode());
        assertEquals(404, call("DELETE", "/requirements/" + id + "/controls/" + controlId, null).statusCode());
        assertEquals(204, call("POST", "/requirements/" + id + "/deactivate", null).statusCode());
        assertEquals(0, mapper.readTree(call("GET", "/requirements?deviceId=" + deviceId, null).body()).size());
    }

    @Test
    void consultasHaciaAtrasYHaciaDelante() throws Exception {
        var back = mapper.readTree(call("GET", "/harms/" + harmId + "/components", null).body());
        assertEquals(1, back.size());
        assertEquals("Motor", back.get(0).get("name").asText());

        var forward = mapper.readTree(call("GET", "/components/" + motorId + "/harms", null).body());
        assertEquals(1, forward.size());
        assertEquals(harmId, forward.get(0).get("harmId").asLong());
        assertEquals(0, mapper.readTree(call("GET", "/components/999999/harms", null).body()).size());
    }

    @Test
    void dashboardRpnDevuelveFilasCategoriasEIndicadores() throws Exception {
        for (FactorType type : FactorType.values()) {
            for (int v = 1; v <= 10; v++) {
                services.fmeaConfig().addScaleLevel(type, v, "n" + v);
            }
        }
        services.fmeaConfig().addCategory("Baja", 1, 100, "prueba");
        services.fmeaConfig().addCategory("Alta", 101, 1000, "prueba");
        long fm = services.fmea().createFailureMode(motorId, "El motor no se detiene").getId();
        services.fmea().createAnalysis(harmId, fm, "Efecto", null, null, null, null, 9, 8, 7);

        var response = call("GET", "/devices/" + deviceId + "/rpn-dashboard", null);
        assertEquals(200, response.statusCode());
        JsonNode d = mapper.readTree(response.body());
        assertEquals(1, d.get("rows").size());
        assertEquals(504, d.get("rows").get(0).get("rpn").asInt());
        assertEquals("Alta", d.get("rows").get(0).get("category").asText());
        assertEquals(504, d.get("maxRpn").asInt());
        assertEquals(2, d.get("categories").size());
        assertEquals(0, d.get("categories").get(0).get("count").asInt());
        assertEquals(1, d.get("categories").get(1).get("count").asInt());
        assertEquals(400, call("GET", "/devices/999999/rpn-dashboard", null).statusCode());
    }
}
