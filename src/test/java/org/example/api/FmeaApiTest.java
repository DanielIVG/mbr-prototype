package org.example.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.Database;
import org.example.config.HibernateConfig;
import org.example.config.Services;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FmeaApiTest {

    @TempDir
    Path tmp;

    SessionFactory sf;
    ApiServer api;
    long harmId;
    long failureModeId;
    final HttpClient http = HttpClient.newHttpClient();
    final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void up() {
        String url = Database.jdbcUrl(tmp.resolve("test.db"));
        Database.migrate(url);
        sf = HibernateConfig.build(url);
        var services = Services.create(sf);
        long deviceId = services.devices().create("Bomba", "Infundir", null).getId();
        harmId = services.chains().createChain(deviceId, "Sobredosis", null,
                "Secuencia", "Situación", "Daño", null).harm().getId();
        failureModeId = services.fmea().createFailureMode(deviceId, "El motor no se detiene").getId();
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
        var request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + api.port() + path))
                .header("Content-Type", "application/json")
                .method(method, publisher)
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> callRaw(String method, String path, String rawBody) throws Exception {
        var request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + api.port() + path))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(rawBody))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static Map<String, Object> category(String name, int min, int max) {
        return Map.<String, Object>of("name", name, "minRpn", min, "maxRpn", max,
                "justification", "prueba");
    }

    private void configureViaApi() throws Exception {
        for (String factor : List.of("SEVERIDAD", "OCURRENCIA", "DETECCION")) {
            for (int v = 1; v <= 10; v++) {
                var r = call("POST", "/fmea/scale-levels", Map.<String, Object>of(
                        "factorType", factor, "value", v,
                        "descriptor", factor.toLowerCase() + " " + v));
                assertEquals(201, r.statusCode());
            }
        }
        for (var c : List.of(category("A", 1, 100), category("B", 101, 300), category("C", 301, 1000))) {
            assertEquals(201, call("POST", "/fmea/categories", c).statusCode());
        }
    }

    private Map<String, Object> analysisBody(int s, int o, int d) {
        return Map.<String, Object>of("harmId", harmId, "failureModeId", failureModeId,
                "effect", "Sobredosis", "cause", "Falla del sensor", "severity", s,
                "occurrence", o, "detection", d);
    }

    private long createAnalysis(int s, int o, int d) throws Exception {
        var r = call("POST", "/fmea-analyses", analysisBody(s, o, d));
        assertEquals(201, r.statusCode());
        return mapper.readTree(r.body()).get("id").asLong();
    }

    @Test
    void sinConfiguracionNoSePuedeAnalizar() throws Exception {
        assertEquals(409, call("GET", "/fmea/config/check", null).statusCode());
        var r = call("POST", "/fmea-analyses", analysisBody(5, 4, 3));
        assertEquals(409, r.statusCode());
        assertTrue(r.body().contains("error"));
    }

    @Test
    void flujoCompletoConfiguraCreaYReevalua() throws Exception {
        configureViaApi();
        assertEquals(204, call("GET", "/fmea/config/check", null).statusCode());
        assertEquals(30, mapper.readTree(call("GET", "/fmea/scale-levels", null).body()).size());
        assertEquals(3, mapper.readTree(call("GET", "/fmea/categories", null).body()).size());

        var created = call("POST", "/fmea-analyses", analysisBody(5, 4, 3));
        assertEquals(201, created.statusCode());
        JsonNode view = mapper.readTree(created.body());
        long id = view.get("id").asLong();
        assertEquals(60, view.get("rpn").get("rpn").asInt());
        assertEquals("A", view.get("rpn").get("category").get("name").asText());
        assertEquals("severidad 5", view.get("rpn").get("severityDescriptor").asText());
        assertEquals("RPN = S × O × D = 5 × 4 × 3 = 60", view.get("rpn").get("rule").asText());

        var updated = call("PUT", "/fmea-analyses/" + id + "/factors",
                Map.of("severity", 5, "occurrence", 5, "detection", 5));
        assertEquals(200, updated.statusCode());
        JsonNode updatedView = mapper.readTree(updated.body());
        assertEquals(125, updatedView.get("rpn").get("rpn").asInt());
        assertEquals("B", updatedView.get("rpn").get("category").get("name").asText());

        JsonNode history = mapper.readTree(call("GET", "/fmea-analyses/" + id + "/history", null).body());
        assertEquals(2, history.size());
        assertFalse(history.get(0).get("current").asBoolean());
        assertTrue(history.get(1).get("current").asBoolean());
        assertEquals(60, history.get(0).get("rpn").asInt());

        assertEquals(125, mapper.readTree(call("GET", "/fmea-analyses/" + id, null).body())
                .get("rpn").get("rpn").asInt());
        assertEquals(1, mapper.readTree(
                call("GET", "/fmea-analyses?harmId=" + harmId, null).body()).size());
    }

    @Test
    void validacionesDeLaConfiguracion() throws Exception {
        var invalidFactor = call("POST", "/fmea/scale-levels", Map.<String, Object>of(
                "factorType", "INVENTADO", "value", 1, "descriptor", "x"));
        assertEquals(400, invalidFactor.statusCode());

        assertEquals(201, call("POST", "/fmea/scale-levels", Map.<String, Object>of(
                "factorType", "SEVERIDAD", "value", 1, "descriptor", "bajo")).statusCode());
        assertEquals(409, call("POST", "/fmea/scale-levels", Map.<String, Object>of(
                "factorType", "SEVERIDAD", "value", 1, "descriptor", "otro")).statusCode());

        var noJustification = call("POST", "/fmea/categories", Map.<String, Object>of(
                "name", "A", "minRpn", 1, "maxRpn", 100, "justification", " "));
        assertEquals(400, noJustification.statusCode());
    }

    @Test
    void jsonMalFormadoDa400() throws Exception {
        var r = callRaw("POST", "/fmea/scale-levels", "{esto no es json");
        assertEquals(400, r.statusCode());
        assertTrue(r.body().contains("error"));
    }

    @Test
    void validacionesDelAnalisis() throws Exception {
        configureViaApi();
        assertEquals(400, call("POST", "/fmea-analyses", analysisBody(11, 4, 3)).statusCode());

        var withoutSeverity = Map.<String, Object>of("harmId", harmId,
                "failureModeId", failureModeId, "effect", "Efecto",
                "occurrence", 4, "detection", 3);
        assertEquals(400, call("POST", "/fmea-analyses", withoutSeverity).statusCode());

        var unknownHarm = Map.<String, Object>of("harmId", 999999L,
                "failureModeId", failureModeId, "effect", "Efecto",
                "severity", 5, "occurrence", 4, "detection", 3);
        assertEquals(404, call("POST", "/fmea-analyses", unknownHarm).statusCode());

        assertEquals(404, call("GET", "/fmea-analyses/999999", null).statusCode());
    }

    @Test
    void modoDeFalloSeCreaYSeConsulta() throws Exception {
        var created = call("POST", "/failure-modes", Map.<String, Object>of(
                "description", "Fuga en la línea"));
        assertEquals(201, created.statusCode());
        long id = mapper.readTree(created.body()).get("id").asLong();
        assertTrue(call("GET", "/failure-modes/" + id, null).body().contains("Fuga"));
        assertEquals(400, call("POST", "/failure-modes", Map.<String, Object>of(
                "description", " ")).statusCode());
        assertEquals(404, call("GET", "/failure-modes/999999", null).statusCode());
    }

    /** RD-03: el recálculo del RPN, de petición a respuesta, tarda menos de 1 segundo. */
    @Test
    void elRecalculoDePuntaAPuntaTardaMenosDeUnSegundo() throws Exception {
        configureViaApi();
        long id = createAnalysis(5, 4, 3);
        int[][] values = {{9, 9, 9}, {1, 1, 1}, {5, 5, 5}, {10, 10, 10}, {2, 3, 4}};
        long worst = 0;
        for (int[] v : values) {
            long start = System.nanoTime();
            var r = call("PUT", "/fmea-analyses/" + id + "/factors",
                    Map.of("severity", v[0], "occurrence", v[1], "detection", v[2]));
            long millis = (System.nanoTime() - start) / 1_000_000;
            assertEquals(200, r.statusCode());
            worst = Math.max(worst, millis);
        }
        System.out.println("RD-03: peor recálculo = " + worst + " ms");
        assertTrue(worst < 1000, "El recálculo más lento tardó " + worst + " ms");
    }
}