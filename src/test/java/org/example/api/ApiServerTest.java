package org.example.api;

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

class ApiServerTest {

    @TempDir
    Path tmp;

    SessionFactory sf;
    ApiServer api;
    final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void up() {
        String url = Database.jdbcUrl(tmp.resolve("test.db"));
        Database.migrate(url);
        sf = HibernateConfig.build(url);
        api = new ApiServer(Services.create(sf)).start();
    }

    @AfterEach
    void down() {
        api.stop();
        sf.close();
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + api.port() + path);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postJson(String path, String json) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthResponde() throws Exception {
        assertTrue(api.port() > 0);
        var r = get("/health");
        assertEquals(200, r.statusCode());
        assertEquals("ok", r.body());
    }

    @Test
    void creaYListaDispositivos() throws Exception {
        var created = postJson("/devices",
                "{\"name\":\"Bomba\",\"intendedUse\":\"Infundir\"}");
        assertEquals(201, created.statusCode());
        assertTrue(created.body().contains("\"name\":\"Bomba\""));

        var list = get("/devices");
        assertEquals(200, list.statusCode());
        assertTrue(list.body().contains("Bomba"));
    }

    @Test
    void nombreVacioDa400() throws Exception {
        var r = postJson("/devices", "{\"name\":\"\",\"intendedUse\":\"Uso\"}");
        assertEquals(400, r.statusCode());
    }

    @Test
    void dispositivoInexistenteDa404() throws Exception {
        assertEquals(404, get("/devices/999").statusCode());
    }
}