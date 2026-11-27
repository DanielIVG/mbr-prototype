package org.example.service;

import org.example.config.Database;
import org.example.config.HibernateConfig;
import org.example.config.Services;
import org.example.repository.RiskChainRepository.Step;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class TraceabilityServiceTest {

    @TempDir
    Path tmp;

    SessionFactory sf;
    Services services;
    TraceabilityService trace;
    Long deviceId;
    Long motorId;
    Long batteryId;
    Long harmId;
    long controlId;

    @BeforeEach
    void setUp() {
        String url = Database.jdbcUrl(tmp.resolve("test.db"));
        Database.migrate(url);
        sf = HibernateConfig.build(url);
        services = Services.create(sf);
        trace = services.traceability();
        deviceId = services.devices().create("Bomba", "Infundir", null).getId();
        motorId = services.components().create(deviceId, null, "Motor", null, false, null).getId();
        batteryId = services.components().create(deviceId, null, "Battery", null, false, null).getId();
        harmId = services.chains().createChain(motorId, "Sobredosis", null,
                List.of(new Step("El sensor falla", motorId), new Step("La batería se agota", batteryId)),
                "El paciente recibe exceso", "Daño por sobredosis", null).harm().getId();
        controlId = services.assessment().addControl(harmId, "Alarma de flujo excesivo", "Protección");
    }

    @AfterEach
    void tearDown() {
        sf.close();
    }

    @Test
    void creaYListaRequisitosNormalizandoCodigoYTipo() {
        var r = trace.createRequirement(deviceId, "  REQ-001 ", " Detener la infusión ante flujo excesivo ",
                "seguridad", "  ");
        assertEquals("REQ-001", r.code());
        assertEquals("Detener la infusión ante flujo excesivo", r.description());
        assertEquals("SEGURIDAD", r.type());
        assertNull(r.source());
        trace.createRequirement(deviceId, "REQ-000", "Otro", "FUNCIONAL", "IEC 60601-2-24");
        var list = trace.listRequirements(deviceId);
        assertEquals(List.of("REQ-000", "REQ-001"), list.stream().map(x -> x.code()).toList());
    }

    @Test
    void rechazaCodigoDescripcionTipoYDispositivoInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> trace.createRequirement(deviceId, " ", "Desc", "FUNCIONAL", null));
        assertThrows(IllegalArgumentException.class,
                () -> trace.createRequirement(deviceId, "R1", " ", "FUNCIONAL", null));
        assertThrows(IllegalArgumentException.class,
                () -> trace.createRequirement(deviceId, "R1", "Desc", "OTRO", null));
        assertThrows(IllegalArgumentException.class,
                () -> trace.createRequirement(deviceId, "X".repeat(31), "Desc", "FUNCIONAL", null));
        assertThrows(IllegalArgumentException.class,
                () -> trace.createRequirement(999999L, "R1", "Desc", "FUNCIONAL", null));
        assertThrows(IllegalArgumentException.class,
                () -> trace.createRequirement(null, "R1", "Desc", "FUNCIONAL", null));
    }

    @Test
    void codigoDuplicadoDaConflictoPeroUnoDesactivadoSePuedeReutilizar() {
        var first = trace.createRequirement(deviceId, "R1", "Uno", "FUNCIONAL", null);
        assertThrows(ConflictException.class,
                () -> trace.createRequirement(deviceId, "R1", "Otro", "FUNCIONAL", null));
        trace.deactivateRequirement(first.id());
        assertDoesNotThrow(() -> trace.createRequirement(deviceId, "R1", "Otro", "FUNCIONAL", null));
    }

    @Test
    void actualizaYRespetaLaUnicidadDelCodigo() {
        var a = trace.createRequirement(deviceId, "R1", "Uno", "FUNCIONAL", null);
        trace.createRequirement(deviceId, "R2", "Dos", "FUNCIONAL", null);
        var updated = trace.updateRequirement(a.id(), "R1", "Uno editado", "usabilidad", "IEC 62366-1");
        assertEquals("Uno editado", updated.description());
        assertEquals("USABILIDAD", updated.type());
        assertThrows(ConflictException.class,
                () -> trace.updateRequirement(a.id(), "R2", "Uno", "FUNCIONAL", null));
        assertThrows(NoSuchElementException.class,
                () -> trace.updateRequirement(999999L, "R9", "x", "FUNCIONAL", null));
    }

    @Test
    void enlazaRequisitoConMedidaYApareceEnLaMatrizConSusPruebas() {
        var r = trace.createRequirement(deviceId, "R1", "Alarmar flujo excesivo", "SEGURIDAD", null);
        services.assessment().addVerification(controlId, "Ensayo en banco", true, true, null);
        services.assessment().addVerification(controlId, "Inspección", true, false, null);
        trace.link(r.id(), controlId);

        var matrix = trace.matrix(deviceId);
        assertEquals(1, matrix.rows().size());
        var row = matrix.rows().get(0);
        assertEquals("R1", row.requirementCode());
        assertEquals("Alarma de flujo excesivo", row.controlDescription());
        assertEquals("Daño por sobredosis", row.harmDescription());
        assertEquals("Sobredosis", row.hazardDescription());
        assertEquals("Motor", row.elementName());
        assertEquals(2, row.verifications());
        assertEquals(1, row.effectiveVerifications());
    }

    @Test
    void noEnlazaMedidasInexistentesNiDeOtroDispositivoNiDuplicadas() {
        var r = trace.createRequirement(deviceId, "R1", "Uno", "FUNCIONAL", null);
        assertThrows(NoSuchElementException.class, () -> trace.link(r.id(), 999999L));
        assertThrows(IllegalArgumentException.class, () -> trace.link(r.id(), null));
        assertThrows(NoSuchElementException.class, () -> trace.link(999999L, controlId));

        Long other = services.devices().create("Monitor", "Medir", null).getId();
        Long otherHarm = services.chains().createChain(other, "Choque", null, "Falla", "Contacto", "Quemadura", null)
                .harm().getId();
        long otherControl = services.assessment().addControl(otherHarm, "Aislamiento", null);
        assertThrows(IllegalArgumentException.class, () -> trace.link(r.id(), otherControl));

        trace.link(r.id(), controlId);
        assertThrows(ConflictException.class, () -> trace.link(r.id(), controlId));
    }

    @Test
    void desenlazaYLaSegundaVezNoExiste() {
        var r = trace.createRequirement(deviceId, "R1", "Uno", "FUNCIONAL", null);
        trace.link(r.id(), controlId);
        trace.unlink(r.id(), controlId);
        assertTrue(trace.matrix(deviceId).rows().isEmpty());
        assertThrows(NoSuchElementException.class, () -> trace.unlink(r.id(), controlId));
    }

    @Test
    void resumenIndicaLosHuecosDeCobertura() {
        var covered = trace.createRequirement(deviceId, "R1", "Con control", "SEGURIDAD", null);
        trace.createRequirement(deviceId, "R2", "Sin control", "FUNCIONAL", null);
        long second = services.assessment().addControl(harmId, "Sin requisito ni prueba", null);
        services.assessment().addVerification(controlId, "Ensayo", true, true, null);
        trace.link(covered.id(), controlId);

        var m = trace.matrix(deviceId);
        assertEquals(2, m.summary().requirements());
        assertEquals(1, m.summary().requirementsWithControl());
        assertEquals(2, m.summary().controls());
        assertEquals(1, m.summary().controlsWithRequirement());
        assertEquals(1, m.summary().controlsVerified());
        assertEquals(List.of("R2"), m.requirementsWithoutControl().stream().map(x -> x.code()).toList());
        assertEquals(List.of(second), m.controlsWithoutRequirement().stream().map(x -> x.controlId()).toList());
    }

    @Test
    void desactivarElRequisitoLoSacaDeLaMatriz() {
        var r = trace.createRequirement(deviceId, "R1", "Uno", "FUNCIONAL", null);
        trace.link(r.id(), controlId);
        trace.deactivateRequirement(r.id());
        assertTrue(trace.listRequirements(deviceId).isEmpty());
        assertTrue(trace.matrix(deviceId).rows().isEmpty());
        assertEquals(0, trace.controls(deviceId).get(0).requirements());
        assertThrows(NoSuchElementException.class, () -> trace.deactivateRequirement(r.id()));
    }

    @Test
    void haciaAtrasListaLosComponentesDelDanoEnOrdenYSinRepetir() {
        var names = trace.componentsOfHarm(harmId).stream().map(e -> e.name()).toList();
        assertEquals(List.of("Motor", "Battery"), names);   // origen y paso 1 son el motor; el paso 2, la batería
        assertTrue(trace.componentsOfHarm(999999L).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> trace.componentsOfHarm(null));
    }

    @Test
    void haciaDelanteListaLosDanosDeUnComponenteComoOrigenOComoPaso() {
        assertEquals(List.of(harmId), trace.harmsOfComponent(motorId).stream().map(h -> h.harmId()).toList());
        assertEquals(List.of(harmId), trace.harmsOfComponent(batteryId).stream().map(h -> h.harmId()).toList());
        Long lonely = services.components().create(deviceId, null, "Pump", null, false, null).getId();
        assertTrue(trace.harmsOfComponent(lonely).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> trace.harmsOfComponent(null));
    }

    @Test
    void lasMedidasDeUnPeligroSobreElDispositivoMismoTambienSeResuelven() {
        Long hid = services.chains().createChain(deviceId, "Fuga eléctrica", null, "Falla", "Contacto", "Quemadura", null)
                .harm().getId();
        long k = services.assessment().addControl(hid, "Aislamiento doble", null);
        var ids = trace.controls(deviceId).stream().map(c -> c.controlId()).toList();
        assertTrue(ids.contains(k));
        assertTrue(ids.contains(controlId));
    }
}
