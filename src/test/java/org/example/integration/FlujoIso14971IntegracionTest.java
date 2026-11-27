package org.example.integration;

import org.example.api.ApiServer;
import org.example.api.AssessmentDtos.CellDto;
import org.example.api.AssessmentDtos.CriteriaDto;
import org.example.api.AssessmentDtos.CriterionDto;
import org.example.api.AssessmentDtos.EstimationRequest;
import org.example.api.AssessmentDtos.ProbabilityDto;
import org.example.api.AssessmentDtos.QuantityDto;
import org.example.api.AssessmentDtos.SeverityDto;
import org.example.api.ComponentDto;
import org.example.api.CreateChainRequest;
import org.example.api.FtaDtos.BasicDto;
import org.example.api.FtaDtos.FtaDto;
import org.example.api.FtaDtos.NodeDto;
import org.example.api.FunctionDto;
import org.example.api.InteractionDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integración entre capas: cliente → API HTTP → servicios → Hibernate/JDBC → SQLite (migraciones reales).
 * Recorre el flujo ISO 14971 completo con el caso de la bomba de infusión.
 */
class FlujoIso14971IntegracionTest {

    @TempDir
    Path tmp;

    SessionFactory sf;
    ApiServer server;
    ApiClient client;

    @BeforeEach
    void up() {
        String url = Database.jdbcUrl(tmp.resolve("integracion.db"));
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

    // ------------------------------------------------------------------ utilidades

    static CriteriaDto criterios() {
        List<SeverityDto> sev = List.of(new SeverityDto(1, "Despreciable"), new SeverityDto(2, "Menor"),
                new SeverityDto(3, "Seria"), new SeverityDto(4, "Crítica"), new SeverityDto(5, "Catastrófica"));
        List<ProbabilityDto> prob = List.of(new ProbabilityDto(1, "Improbable", 0.0, 1e-7),
                new ProbabilityDto(2, "Remota", 1e-7, 1e-5), new ProbabilityDto(3, "Ocasional", 1e-5, 1e-3),
                new ProbabilityDto(4, "Probable", 1e-3, 1e-1), new ProbabilityDto(5, "Frecuente", 1e-1, 1.0));
        List<CellDto> cells = new ArrayList<>();
        for (int s = 1; s <= 5; s++) {
            for (int p = 1; p <= 5; p++) {
                int tolerableAt = 6 - s;
                cells.add(new CellDto(s, p, p < tolerableAt ? "Aceptable" : p == tolerableAt ? "Tolerable" : "Inaceptable"));
            }
        }
        return new CriteriaDto("por hora", sev, prob, cells, List.of(
                new CriterionDto("Aceptable", true, "Sin acción adicional"),
                new CriterionDto("Tolerable", false, "Requiere reducir mientras sea factible"),
                new CriterionDto("Inaceptable", false, "No se admite")), false);
    }

    static QuantityDto q(String role, double value, String unit, Double min, Double max) {
        return new QuantityDto(role, value, unit, min, max, "Fuente de prueba", "JUICIO_EXPERTO", "tester");
    }

    private static List<QuantityDto> bomba() {
        return List.of(q("RATE", 1e-6, "por hora", 1e-6, 1e-4), q("FRACTION", 0.2, "adimensional", 0.05, 0.5),
                q("BARRIER", 0.99, "adimensional", null, null), q("P2", 0.3, "adimensional", 0.05, 1.0));
    }

    /** Dispositivo con dos componentes y una cadena de dos pasos; devuelve el id del daño. */
    private long cadenaDeLaBomba(long[] ids) {
        long device = client.createDevice("Bomba de infusión", "Administrar fármacos", "Uso con otro fármaco").id();
        ComponentDto motor = client.createComponent(new ComponentDto(null, device, null, "Motor", "Impulsa el émbolo", true, "Si falla no hay infusión", true));
        ComponentDto sensor = client.createComponent(new ComponentDto(null, device, motor.id(), "Sensor", null, false, null, true));
        ids[0] = device;
        ids[1] = motor.id();
        ids[2] = sensor.id();
        var chain = client.createChain(new CreateChainRequest(motor.id(), "Exceso de fármaco", null, null,
                "El paciente recibe más dosis de la prescrita", "Sobredosis", "Lesión",
                List.of(new CreateChainRequest.Step("El motor no se detiene", motor.id()),
                        new CreateChainRequest.Step("El sensor no detecta el exceso", sensor.id()))));
        return chain.harm().id();
    }

    // ------------------------------------------------------------------ estructura y cadena

    @Test
    void estructuraYCadenaConPasosPorComponenteSePersisten() {
        long[] ids = new long[3];
        long harm = cadenaDeLaBomba(ids);

        // Crear dos funciones para poder crear la interacción
        FunctionDto f1 = client.createFunction(new FunctionDto(null, ids[0], "Impulsar", null, Set.of(ids[1]), true));
        assertEquals(Set.of(ids[1]), f1.componentIds());
        FunctionDto f2 = client.createFunction(new FunctionDto(null, ids[0], "Detectar", null, Set.of(ids[2]), true));
        assertEquals(Set.of(ids[2]), f2.componentIds());
        // La interacción ahora une funciones (no componentes directamente)
        InteractionDto i = client.createInteraction(new InteractionDto(null, ids[0], "Motor mueve el sensor",
                "Un fallo mecánico afecta la lectura", Set.of(f1.id(), f2.id()), Set.of(), true));
        assertEquals(2, i.functionIds().size());
        assertEquals(2, i.componentIds().size()); // derivados de las funciones
        assertEquals(2, client.listComponents(ids[0]).size());

        var chains = client.listChains();
        assertEquals(1, chains.size());
        var seq = client.listSequence(chains.get(0).hazard().id());
        assertEquals(2, seq.size());
        assertEquals(1, seq.get(0).stepOrder());
        assertEquals(ids[1], seq.get(0).modelElementId());
        assertEquals(ids[2], seq.get(1).modelElementId());
        assertEquals(harm, chains.get(0).harm().id());
    }

    @Test
    void unaInteraccionConUnaSolaFuncionSeRechazaPorHttp() {
        long[] ids = new long[3];
        cadenaDeLaBomba(ids);
        // Crear una sola función
        FunctionDto f = client.createFunction(new FunctionDto(null, ids[0], "Impulsar", null, Set.of(ids[1]), true));
        // Intentar crear interacción con solo esa función debe fallar
        var e = assertThrows(ApiException.class, () -> client.createInteraction(
                new InteractionDto(null, ids[0], "x", "y", Set.of(f.id()), Set.of(), true)));
        assertEquals(400, e.status());
    }

    // ------------------------------------------------------------------ criterios y estimación

    @Test
    void sinCriteriosNoSePuedeEstimar() {
        long harm = cadenaDeLaBomba(new long[3]);
        assertNull(client.getCriteria());
        var e = assertThrows(ApiException.class,
                () -> client.estimate(harm, new EstimationRequest("FAILURE_MODE", 5, bomba(), null, null)));
        assertEquals(409, e.status());
    }

    @Test
    void criteriosInvalidosSeRechazan() {
        CriteriaDto ok = criterios();
        List<ProbabilityDto> hueco = new ArrayList<>(ok.probabilities());
        hueco.set(1, new ProbabilityDto(2, "Remota", 5e-7, 1e-5));              // hueco entre 1e-7 y 5e-7
        var e = assertThrows(ApiException.class, () -> client.saveCriteria(
                new CriteriaDto(ok.exposureUnit(), ok.severities(), hueco, ok.cells(), ok.criteria(), false)));
        assertEquals(400, e.status());
        assertNull(client.getCriteria());
    }

    @Test
    void flujoCompletoEstimacionVeredictoControlYRiesgoResidual() {
        long harm = cadenaDeLaBomba(new long[3]);
        client.saveCriteria(criterios());
        assertFalse(client.getCriteria().locked());

        // Riesgo inicial: P = 1e-6 × 0.2 × (1 − 0.99) × 0.3 = 6e-10 → Improbable; severidad 5 → Tolerable
        var inicial = client.estimate(harm, new EstimationRequest("FAILURE_MODE", 5, bomba(), null, null));
        assertEquals(6e-10, inicial.pTotal(), 1e-20);
        assertEquals("Tolerable", inicial.riskLevel());
        assertFalse(inicial.acceptable());
        assertEquals(4, inicial.quantities().size());
        assertEquals("Fuente de prueba", inicial.quantities().get(0).source());
        assertTrue(client.getCriteria().locked());

        // Congelación de criterios tras estimar
        var bloqueo = assertThrows(ApiException.class, () -> client.saveCriteria(criterios()));
        assertEquals(409, bloqueo.status());

        // Sensibilidad: solo la severidad cambia el veredicto por sí sola
        var s = client.sensitivity(harm);
        assertEquals("Tolerable", s.baselineLevel());
        assertTrue(s.params().stream().anyMatch(p -> p.name().equals("Severidad") && p.decides()));
        assertTrue(s.params().stream().noneMatch(p -> p.name().equals("Tasa de falla") && p.decides()));
        assertFalse(s.corners().isEmpty());                                      // combinaciones extremas sí cambian

        // Control y verificación
        var control = client.addControl(harm, "Alarma de exceso de flujo", "Protección");
        assertThrows(ApiException.class, () -> client.addVerification(control.id(), " ", true, true, null));
        assertThrows(ApiException.class, () -> client.addVerification(control.id(), "Ensayo", false, true, null));
        var v = client.addVerification(control.id(), "Ensayo de banco", true, true, "Informe 12");
        assertTrue(v.implemented());
        assertEquals(1, client.listVerifications(control.id()).size());

        // Reevaluación: exige motivo; con motivo, queda el historial
        var sinMotivo = assertThrows(ApiException.class,
                () -> client.estimate(harm, new EstimationRequest("FAILURE_MODE", 4, bomba(), null, null)));
        assertEquals(400, sinMotivo.status());
        var residual = client.estimate(harm,
                new EstimationRequest("FAILURE_MODE", 4, bomba(), null, "Alarma implementada y verificada"));
        assertEquals("Aceptable", residual.riskLevel());
        assertTrue(residual.acceptable());
        assertEquals(2, client.estimationHistory(harm).size());
        assertEquals(residual.id(), client.latestEstimation(harm).id());
    }

    @Test
    void cantidadesSinProcedenciaOConUnidadIncompatibleSeRechazan() {
        long harm = cadenaDeLaBomba(new long[3]);
        client.saveCriteria(criterios());

        List<QuantityDto> sinFuente = new ArrayList<>(bomba());
        sinFuente.set(0, new QuantityDto("RATE", 1e-6, "por hora", null, null, " ", "SUPUESTO", "tester"));
        assertEquals(400, assertThrows(ApiException.class, () -> client.estimate(harm,
                new EstimationRequest("FAILURE_MODE", 5, sinFuente, null, null))).status());

        List<QuantityDto> otraUnidad = new ArrayList<>(bomba());
        otraUnidad.set(0, q("RATE", 1e-6, "por ciclo", null, null));
        assertEquals(400, assertThrows(ApiException.class, () -> client.estimate(harm,
                new EstimationRequest("FAILURE_MODE", 5, otraUnidad, null, null))).status());

        assertEquals(400, assertThrows(ApiException.class, () -> client.estimate(harm,
                new EstimationRequest("TOTAL_ONLY", 5, List.of(q("TOTAL", 1e-6, "por hora", null, null)), " ", null))).status());

        assertNull(client.latestEstimation(harm));                               // nada quedó guardado
    }

    @Test
    void probabilidadTotalConJustificacionSeAcepta() {
        long harm = cadenaDeLaBomba(new long[3]);
        client.saveCriteria(criterios());
        var e = client.estimate(harm, new EstimationRequest("TOTAL_ONLY", 3,
                List.of(q("TOTAL", 1e-6, "por hora", null, null)), "Dato agregado de campo", null));
        assertNull(e.p1());
        assertEquals("Dato agregado de campo", e.justification());
        assertEquals(2, e.probabilityLevel());
    }

    // ------------------------------------------------------------------ árbol de fallas

    private static BasicDto basic(String name, double v, String unit, String evidence) {
        return new BasicDto(name, new QuantityDto("P", v, unit, null, null, "Fuente " + name, evidence, "tester"), null);
    }

    private static NodeDto ref(String name) {
        return new NodeDto("BASIC", null, null, null, null, name);
    }

    /** Evento superior = A (por hora) Y (B O C). P1 = 1e-4 × (0,01 + 0,1 − 0,001) = 1,09e-5 por hora. */
    private static FtaDto arbol(String independencia) {
        var raiz = new NodeDto("GATE", "Sobredosis", "AND", independencia, List.of(ref("A"),
                new NodeDto("GATE", "Falla de detección", "OR", null, List.of(ref("B"), ref("C")), null)), null);
        return new FtaDto(null, "Sobredosis", List.of(
                basic("A", 1e-4, "por hora", "MEDIDO"),
                basic("B", 0.01, "adimensional", "JUICIO_EXPERTO"),
                basic("C", 0.1, "adimensional", "SUPUESTO")), raiz, null);
    }

    @Test
    void arbolDeFallasAlimentaP1DeLaEstimacion() {
        long harm = cadenaDeLaBomba(new long[3]);
        client.saveCriteria(criterios());

        // Sin árbol, el modo FTA no es posible
        var sinArbol = assertThrows(ApiException.class, () -> client.estimate(harm,
                new EstimationRequest("FTA", 5, List.of(q("P2", 0.3, "adimensional", null, null)), null, null)));
        assertEquals(409, sinArbol.status());
        assertNull(client.getFta(harm));

        // La compuerta Y exige justificar la independencia
        assertEquals(400, assertThrows(ApiException.class, () -> client.saveFta(harm, arbol(" "))).status());

        var guardado = client.saveFta(harm, arbol("A y los detectores fallan por causas distintas"));
        assertEquals(1.09e-5, guardado.result().probability(), 1e-12);
        assertEquals("por hora", guardado.result().unit());
        assertTrue(guardado.result().exact());
        assertEquals(2, guardado.result().cutSets().size());
        assertTrue(guardado.result().singlePoints().isEmpty());
        assertEquals(guardado.analysisId(), client.getFta(harm).analysisId());

        var e = client.estimate(harm, new EstimationRequest("FTA", 5,
                List.of(q("P2", 0.3, "adimensional", null, null)), null, null));
        assertEquals(1.09e-5 * 0.3, e.pTotal(), 1e-15);
        assertEquals(guardado.analysisId(), e.ftaAnalysisId());
        var p1 = e.quantities().stream().filter(x -> x.role().equals("P1")).findFirst().orElseThrow();
        assertEquals("SUPUESTO", p1.evidence());                                  // la más débil de los eventos
        assertTrue(p1.source().contains("Árbol de fallas"));
        assertEquals(2, e.probabilityLevel());
    }

    @Test
    void arbolSinUnidadDeExposicionEnElEventoSuperiorSeRechaza() {
        long harm = cadenaDeLaBomba(new long[3]);
        var raiz = new NodeDto("GATE", "Top", "OR", null, List.of(ref("B"), ref("C")), null);
        var dto = new FtaDto(null, "Top", List.of(basic("B", 0.01, "adimensional", "SUPUESTO"),
                basic("C", 0.1, "adimensional", "SUPUESTO")), raiz, null);
        assertEquals(400, assertThrows(ApiException.class, () -> client.saveFta(harm, dto)).status());
        assertNull(client.getFta(harm));
    }
}
