package org.example.service;

import org.example.config.Database;
import org.example.config.HibernateConfig;
import org.example.config.Services;
import org.example.model.RiskChain;
import org.example.repository.ModelElementRepository;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RiskChainServiceTest {

    @TempDir
    Path tmp;

    SessionFactory sf;
    Services services;
    RiskChainService chains;
    Long deviceId;
    Long componentId;

    @BeforeEach
    void setUp() {
        String url = Database.jdbcUrl(tmp.resolve("test.db"));
        Database.migrate(url);
        sf = HibernateConfig.build(url);
        services = Services.create(sf);
        chains = services.chains();
        deviceId = services.devices().create("Bomba", "Infundir", null).getId();
        componentId = services.components()
                .create(deviceId, null, "Motor", null, false, null).getId();
    }

    @AfterEach
    void tearDown() {
        sf.close();
    }

    private RiskChain newChain() {
        return chains.createChain(componentId, "Sobredosis", "Manual",
                "Falla el sensor y el motor sigue girando",
                "El paciente recibe exceso de fármaco",
                "Daño por sobredosis", "Lesión");
    }

    @Test
    void creaLaCadenaCompletaYLaRecupera() {
        var chain = newChain();
        assertNotNull(chain.harm().getId());
        assertEquals(chain.hazard().getId(), chain.sequence().getHazardId());
        assertEquals(chain.sequence().getId(), chain.situation().getSequenceOfEventsId());
        assertEquals(chain.situation().getId(), chain.harm().getHazardousSituationId());
        assertEquals(1, chain.sequence().getStepOrder());

        var found = chains.findChain(chain.harm().getId()).orElseThrow();
        assertEquals("Sobredosis", found.hazard().getDescription());
        assertEquals(componentId, found.hazard().getModelElementId());
        assertEquals("Daño por sobredosis", found.harm().getDescription());
    }

    @Test
    void rechazaCamposObligatoriosVaciosYElementosInexistentes() {
        assertThrows(IllegalArgumentException.class, () -> chains.createChain(
                componentId, " ", null, "s", "sit", "daño", null));
        assertThrows(IllegalArgumentException.class, () -> chains.createChain(
                componentId, "p", null, "s", "sit", "", null));
        assertThrows(IllegalArgumentException.class, () -> chains.createChain(
                999999L, "p", null, "s", "sit", "daño", null));
    }

    @Test
    void unPeligroPuedeTenerVariasSecuenciasYSusOrdenesCrecen() {
        var chain = newChain();
        var second = chains.addSequence(chain.hazard().getId(), "Otra secuencia");
        assertEquals(2, second.getStepOrder());
    }

    @Test
    void unaSecuenciaPuedeTenerVariasSituacionesYUnaSituacionVariosDanos() {
        var chain = newChain();
        var situation2 = chains.addSituation(chain.sequence().getId(), "Otra situación");
        chains.addHarm(situation2.getId(), "Daño B", "Lesión");
        chains.addHarm(chain.situation().getId(), "Daño C", "Lesión");
        assertEquals(3, chains.listChains().size());
    }

    @Test
    void agregarEslabonesAPadresInexistentesDa404() {
        assertThrows(java.util.NoSuchElementException.class, () -> chains.addSequence(999L, "x"));
        assertThrows(java.util.NoSuchElementException.class, () -> chains.addSituation(999L, "x"));
        assertThrows(java.util.NoSuchElementException.class, () -> chains.addHarm(999L, "x", null));
    }

    @Test
    void actualizaPeligroYDano() {
        var chain = newChain();
        chains.updateHazard(chain.hazard().getId(), deviceId, "Sobredosis grave", "Arney 2009");
        chains.updateHarm(chain.harm().getId(), "Daño grave", "Muerte");

        var found = chains.findChain(chain.harm().getId()).orElseThrow();
        assertEquals("Sobredosis grave", found.hazard().getDescription());
        assertEquals(deviceId, found.hazard().getModelElementId());
        assertEquals("Muerte", found.harm().getType());
    }

    @Test
    void modificarElComponenteMarcaElPeligroPendienteDeRevision() {
        var chain = newChain();
        assertNull(chain.hazard().getReviewStatus());

        services.components().update(componentId, null, "Motor v2", null, false, null);

        var found = chains.findChain(chain.harm().getId()).orElseThrow();
        assertEquals(ModelElementRepository.PENDING_REVIEW, found.hazard().getReviewStatus());
    }

    @Test
    void conUnPeligroAsociadoElComponenteNoSeEliminaPeroSiSeDesactiva() {
        newChain();
        assertThrows(ConflictException.class, () -> services.components().delete(componentId));
        services.components().deactivate(componentId);
        assertFalse(services.components().find(componentId).orElseThrow().isActive());
    }
}