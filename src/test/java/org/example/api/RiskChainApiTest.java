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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RiskChainApiTest {

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

    private JsonNode createChain() throws Exception {
        var body = Map.<String, Object>of(
                "modelElementId", deviceId,
                "hazardDescription", "Sobredosis",
                "source", "Arney 2009",
                "sequenceDescription", "El sensor falla y el motor sigue girando",
                "situationDescription", "El paciente recibe exceso de fármaco",
                "harmDescription", "Daño por sobredosis",
                "harmType", "Lesión");
        var response = call("POST", "/risk-chains", body);
        assertEquals(201, response.statusCode());
        return mapper.readTree(response.body());
    }

    @Test
    void creaYConsultaLaCadena() throws Exception {
        var chain = createChain();
        long harmId = chain.get("harm").get("id").asLong();
        assertEquals(1, chain.get("sequence").get("stepOrder").asInt());

        var found = call("GET", "/risk-chains/" + harmId, null);
        assertEquals(200, found.statusCode());
        assertTrue(found.body().contains("Sobredosis"));
        assertTrue(found.body().contains("Daño por sobredosis"));
    }

    @Test
    void camposObligatoriosVaciosDan400() throws Exception {
        var body = Map.<String, Object>of(
                "hazardDescription", "Sobredosis",
                "sequenceDescription", "Secuencia",
                "situationDescription", "Situación");
        var response = call("POST", "/risk-chains", body);
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("error"));
    }

    @Test
    void elementoInexistenteDa400() throws Exception {
        var body = Map.<String, Object>of(
                "modelElementId", 999999L,
                "hazardDescription", "Sobredosis",
                "sequenceDescription", "Secuencia",
                "situationDescription", "Situación",
                "harmDescription", "Daño");
        assertEquals(400, call("POST", "/risk-chains", body).statusCode());
    }

    @Test
    void cadenaInexistenteDa404() throws Exception {
        assertEquals(404, call("GET", "/risk-chains/999", null).statusCode());
    }

    @Test
    void agregaSecuenciaSituacionYDano() throws Exception {
        var chain = createChain();
        long hazardId = chain.get("hazard").get("id").asLong();

        var sequence = call("POST", "/hazards/" + hazardId + "/sequences",
                Map.of("description", "Otra secuencia"));
        assertEquals(201, sequence.statusCode());
        var sequenceNode = mapper.readTree(sequence.body());
        assertEquals(2, sequenceNode.get("stepOrder").asInt());

        var situation = call("POST", "/sequences/" + sequenceNode.get("id").asLong() + "/situations",
                Map.of("description", "Otra situación"));
        assertEquals(201, situation.statusCode());
        long situationId = mapper.readTree(situation.body()).get("id").asLong();

        var harm = call("POST", "/situations/" + situationId + "/harms",
                Map.of("description", "Otro daño", "type", "Lesión"));
        assertEquals(201, harm.statusCode());

        var list = mapper.readTree(call("GET", "/risk-chains", null).body());
        assertEquals(2, list.size());
    }

    @Test
    void actualizaElDano() throws Exception {
        var chain = createChain();
        long harmId = chain.get("harm").get("id").asLong();

        var response = call("PUT", "/harms/" + harmId,
                Map.of("description", "Daño grave", "type", "Muerte"));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Daño grave"));
        assertTrue(call("GET", "/risk-chains/" + harmId, null).body().contains("Muerte"));
    }
}