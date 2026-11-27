package org.example.integration;

import org.example.api.AssessmentDtos.EstimationRequest;
import org.example.api.BehaviorDtos.CreateNodeRequest;
import org.example.api.BehaviorDtos.NodeDto;
import org.example.api.ApiServer;
import org.example.api.ComponentDto;
import org.example.client.ApiClient;
import org.example.client.ApiException;
import org.example.config.Database;
import org.example.config.HibernateConfig;
import org.example.config.Services;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.example.integration.FlujoIso14971IntegracionTest.criterios;
import static org.example.integration.FlujoIso14971IntegracionTest.q;
import static org.junit.jupiter.api.Assertions.*;

/** Árbol de comportamiento (SÍ/NO) por componente y la cadena de riesgo que genera. */
class ArbolComportamientoIntegracionTest {

    @TempDir
    Path tmp;

    SessionFactory sf;
    ApiServer server;
    ApiClient client;
    long device;
    long motor;
    long sensor;

    @BeforeEach
    void up() {
        String url = Database.jdbcUrl(tmp.resolve("arbol.db"));
        Database.migrate(url);
        sf = HibernateConfig.build(url);
        server = new ApiServer(Services.create(sf)).start();
        client = new ApiClient("http://127.0.0.1:" + server.port());
        device = client.createDevice("Bomba de infusión", "Administrar fármacos", null).id();
        motor = client.createComponent(new ComponentDto(null, device, null, "Motor", "Impulsa el émbolo", true, "Si falla no hay infusión", true)).id();
        sensor = client.createComponent(new ComponentDto(null, device, motor, "Sensor", null, false, null, true)).id();
    }

    @AfterEach
    void down() {
        server.stop();
        sf.close();
    }

    private NodeDto nodo(Long element, Long parent, String kind, String text, String branch) {
        return client.createBehaviorNode(new CreateNodeRequest(element, parent, kind, text, branch, null, null, null));
    }

    private NodeDto peligroso(Long parent, String branch) {
        return client.createBehaviorNode(new CreateNodeRequest(null, parent, "OUTCOME_HAZARD", "Sobredosis", branch,
                "El paciente recibe más dosis de la prescrita", "Lesión por sobredosis", "Lesión"));
    }

    /** Raíz (motor) → condición (sensor) → NO: peligroso / SÍ: seguro. */
    private NodeDto[] arbolDeLaBomba() {
        NodeDto raiz = nodo(motor, null, "ACTION", "El motor no se detiene", null);
        NodeDto cond = nodo(sensor, raiz.id(), "CONDITION", "¿El sensor detecta el exceso?", null);
        NodeDto seguro = nodo(null, cond.id(), "OUTCOME_SAFE", "Alarma y parada", "YES");
        NodeDto hazard = peligroso(cond.id(), "NO");
        return new NodeDto[] {raiz, cond, seguro, hazard};
    }

    @Test
    void unaHojaPeligrosaGeneraLaCadenaConLosPasosDelCamino() {
        NodeDto[] t = arbolDeLaBomba();

        var chains = client.listChains();
        assertEquals(1, chains.size());
        assertEquals("El motor no se detiene", chains.get(0).hazard().description());
        assertEquals("Lesión por sobredosis", chains.get(0).harm().description());
        var seq = client.listSequence(chains.get(0).hazard().id());
        assertEquals(2, seq.size());
        assertEquals("El motor no se detiene", seq.get(0).description());
        assertEquals(motor, seq.get(0).modelElementId());
        assertEquals("¿El sensor detecta el exceso? → NO", seq.get(1).description());
        assertEquals(sensor, seq.get(1).modelElementId());
        assertEquals(t[3].harmId(), chains.get(0).harm().id());
    }

    @Test
    void laCadenaGeneradaSeEstimaConElFlujoNormal() {
        arbolDeLaBomba();
        client.saveCriteria(criterios());
        long harm = client.listChains().get(0).harm().id();
        var e = client.estimate(harm, new EstimationRequest("P1_P2", 5,
                List.of(q("P1", 1e-6, "por hora", null, null), q("P2", 0.3, "adimensional", null, null)), null, null));
        assertEquals(3e-7, e.pTotal(), 1e-18);
    }

    @Test
    void listaLosNodosDelComponenteYMarcaLosBloqueados() {
        NodeDto[] t = arbolDeLaBomba();
        var nodos = client.listBehavior(motor);                        // los pasos en el sensor cuelgan de la raíz del motor
        assertEquals(4, nodos.size());
        assertFalse(nodos.stream().filter(n -> n.id().equals(t[2].id())).findFirst().orElseThrow().locked());
        assertTrue(nodos.stream().filter(n -> n.id().equals(t[3].id())).findFirst().orElseThrow().locked());
        assertTrue(nodos.stream().filter(n -> n.id().equals(t[0].id())).findFirst().orElseThrow().locked());
        assertEquals(4, client.listBehavior(sensor).size());           // el árbol pasa por el sensor (nodos suyos)
    }

    @Test
    void elArbolDelDispositivoIncluyeSoloSusPropiosNodos() {
        arbolDeLaBomba();
        long otro = client.createDevice("Otro", "Otro uso", null).id();
        var c = client.createComponent(new ComponentDto(null, otro, null, "Pump", null, false, null, true));
        nodo(c.id(), null, "ACTION", "Falla ajena", null);
        assertEquals(4, client.listDeviceBehavior(device).size());
        assertEquals(1, client.listDeviceBehavior(otro).size());
    }

    @Test
    void unComponentePuedeTenerVariasRaices() {
        nodo(motor, null, "CONDITION", "¿Se pierde la alimentación?", null);
        nodo(motor, null, "ACTION", "El émbolo se atasca", null);
        assertEquals(2, client.listBehavior(motor).size());
    }

    @Test
    void reglasDeLaEstructuraSeAplicanPorHttp() {
        NodeDto raiz = nodo(motor, null, "ACTION", "Falla", null);
        nodo(null, raiz.id(), "ACTION", "Siguiente", null);
        assertEquals(409, assertThrows(ApiException.class, () -> nodo(null, raiz.id(), "ACTION", "Otra", null)).status());

        NodeDto cond = nodo(motor, null, "CONDITION", "¿Algo?", null);
        assertEquals(400, assertThrows(ApiException.class, () -> nodo(null, cond.id(), "ACTION", "x", null)).status());
        NodeDto si = nodo(null, cond.id(), "OUTCOME_SAFE", "ok", "YES");
        assertEquals(409, assertThrows(ApiException.class, () -> nodo(null, cond.id(), "OUTCOME_SAFE", "otra", "YES")).status());
        assertEquals(400, assertThrows(ApiException.class, () -> nodo(null, si.id(), "ACTION", "x", null)).status());
        assertEquals(400, assertThrows(ApiException.class, () -> nodo(motor, null, "OUTCOME_SAFE", "raíz", null)).status());
        assertEquals(400, assertThrows(ApiException.class, () -> nodo(motor, null, "ACTION", " ", null)).status());
        assertEquals(400, assertThrows(ApiException.class, () -> client.createBehaviorNode(new CreateNodeRequest(
                null, cond.id(), "OUTCOME_HAZARD", "Peligro", "NO", "situación", null, null))).status());
        assertEquals(0, client.listChains().size());                   // lo rechazado no dejó cadenas
    }

    @Test
    void unCaminoConCadenaNoSeEditaNiSeElimina_unaHojaSeguraSi() {
        NodeDto[] t = arbolDeLaBomba();
        assertEquals(409, assertThrows(ApiException.class, () -> client.updateBehaviorNode(t[0].id(), "Otro texto")).status());
        assertEquals(409, assertThrows(ApiException.class, () -> client.deactivateBehaviorNode(t[1].id())).status());
        assertEquals(409, assertThrows(ApiException.class, () -> client.deactivateBehaviorNode(t[3].id())).status());

        assertEquals("Parada segura", client.updateBehaviorNode(t[2].id(), "Parada segura").description());
        client.deactivateBehaviorNode(t[2].id());
        assertEquals(3, client.listBehavior(motor).size());
        // la rama SÍ quedó libre y se puede volver a crear
        assertNotNull(nodo(null, t[1].id(), "OUTCOME_SAFE", "Alarma", "YES").id());
    }
}
