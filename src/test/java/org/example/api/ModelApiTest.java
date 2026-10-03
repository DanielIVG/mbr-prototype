package org.example.api;

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

import static org.junit.jupiter.api.Assertions.*;

class ModelApiTest {

    @TempDir
    Path tmp;

    SessionFactory sf;
    ApiServer api;
    long deviceId;
    final HttpClient http = HttpClient.newHttpClient();
    final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void up() {
        String url = Database.jdbcUrl(tmp.resolve("test.db"));
        Database.migrate(url);
        sf = HibernateConfig.build(url);
        var services = Services.create(sf);
        deviceId = services.devices().create("Bomba", "Infundir", null).getId();
        api = new ApiServer(services).start();
    }

    @AfterEach
    void down() {
        api.stop();
        sf.close();
    }

    private HttpResponse<String> call(String method, String path, String json) throws Exception {
        var publisher = json == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json);
        var request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + api.port() + path))
                .header("Content-Type", "application/json")
                .method(method, publisher)
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private long createComponent(String name) throws Exception {
        var r = call("POST", "/components",
                "{\"deviceId\":" + deviceId + ",\"name\":\"" + name + "\"}");
        assertEquals(201, r.statusCode());
        return mapper.readTree(r.body()).get("id").asLong();
    }

    @Test
    void componenteCrudYReglas() throws Exception {
        var created = call("POST", "/components",
                "{\"deviceId\":" + deviceId + ",\"name\":\"Motor\",\"essential\":true,"
                        + "\"essentialJustification\":\"Imprescindible\"}");
        assertEquals(201, created.statusCode());
        long id = mapper.readTree(created.body()).get("id").asLong();

        assertTrue(call("GET", "/components?deviceId=" + deviceId, null).body().contains("Motor"));
        assertEquals(409, call("DELETE", "/components/" + id, null).statusCode());
        assertEquals(204, call("POST", "/components/" + id + "/deactivate", null).statusCode());
        assertEquals("[]", call("GET", "/components?deviceId=" + deviceId, null).body());
    }

    @Test
    void esencialSinJustificacionDa400() throws Exception {
        var r = call("POST", "/components",
                "{\"deviceId\":" + deviceId + ",\"name\":\"Motor\",\"essential\":true}");
        assertEquals(400, r.statusCode());
        assertTrue(r.body().contains("error"));
    }

    @Test
    void interaccionNecesitaDosComponentes() throws Exception {
        long a = createComponent("A");
        long b = createComponent("B");

        var uno = call("POST", "/interactions",
                "{\"deviceId\":" + deviceId + ",\"description\":\"Fuga\","
                        + "\"relevanceJustification\":\"Relevante\",\"componentIds\":[" + a + "]}");
        assertEquals(400, uno.statusCode());

        var dos = call("POST", "/interactions",
                "{\"deviceId\":" + deviceId + ",\"description\":\"Fuga\","
                        + "\"relevanceJustification\":\"Relevante\",\"componentIds\":[" + a + "," + b + "]}");
        assertEquals(201, dos.statusCode());
        assertEquals(2, mapper.readTree(dos.body()).get("componentIds").size());
    }

    @Test
    void funcionVinculaComponentesYSeElimina() throws Exception {
        long a = createComponent("A");
        var created = call("POST", "/functions",
                "{\"deviceId\":" + deviceId + ",\"name\":\"Infundir\",\"componentIds\":[" + a + "]}");
        assertEquals(201, created.statusCode());
        long id = mapper.readTree(created.body()).get("id").asLong();

        assertTrue(call("GET", "/functions?deviceId=" + deviceId, null).body().contains("Infundir"));
        assertEquals(204, call("DELETE", "/functions/" + id, null).statusCode());
        assertEquals(404, call("GET", "/functions/" + id, null).statusCode());
    }
}