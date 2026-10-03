// RiskChainService.java
package org.example.service;

import org.example.model.Harm;
import org.example.model.Hazard;
import org.example.model.HazardousSituation;
import org.example.model.RiskChain;
import org.example.model.SequenceOfEvents;
import org.example.repository.RiskChainRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class RiskChainService {

    private final RiskChainRepository repo;

    public RiskChainService(RiskChainRepository repo) {
        this.repo = repo;
    }

    /** Registra una cadena completa: peligro → secuencia → situación peligrosa → daño (RF-09). */
    public RiskChain createChain(Long modelElementId, String hazardDescription, String source,
                                 String sequenceDescription, String situationDescription,
                                 String harmDescription, String harmType) {
        Validation.requireText(hazardDescription, "La descripción del peligro es obligatoria");
        Validation.requireText(sequenceDescription, "La secuencia de eventos es obligatoria");
        Validation.requireText(situationDescription, "La situación peligrosa es obligatoria");
        Validation.requireText(harmDescription, "La descripción del daño es obligatoria");
        requireElementIfPresent(modelElementId);

        Hazard hazard = new Hazard();
        hazard.setModelElementId(modelElementId);
        hazard.setDescription(hazardDescription.trim());
        hazard.setSource(source);
        return repo.createChain(hazard, sequenceDescription.trim(), situationDescription.trim(),
                harmDescription.trim(), harmType);
    }

    public SequenceOfEvents addSequence(Long hazardId, String description) {
        Validation.requireText(description, "La secuencia de eventos es obligatoria");
        require(Hazard.class, hazardId, "Peligro");
        SequenceOfEvents s = new SequenceOfEvents();
        s.setHazardId(hazardId);
        s.setDescription(description.trim());
        s.setStepOrder(repo.nextStepOrder(hazardId));
        return repo.save(s);
    }

    public HazardousSituation addSituation(Long sequenceId, String description) {
        Validation.requireText(description, "La situación peligrosa es obligatoria");
        require(SequenceOfEvents.class, sequenceId, "Secuencia de eventos");
        HazardousSituation s = new HazardousSituation();
        s.setSequenceOfEventsId(sequenceId);
        s.setDescription(description.trim());
        return repo.save(s);
    }

    public Harm addHarm(Long situationId, String description, String type) {
        Validation.requireText(description, "La descripción del daño es obligatoria");
        require(HazardousSituation.class, situationId, "Situación peligrosa");
        Harm h = new Harm();
        h.setHazardousSituationId(situationId);
        h.setDescription(description.trim());
        h.setType(type);
        return repo.save(h);
    }

    public Hazard updateHazard(Long id, Long modelElementId, String description, String source) {
        Validation.requireText(description, "La descripción del peligro es obligatoria");
        requireElementIfPresent(modelElementId);
        Hazard h = require(Hazard.class, id, "Peligro");
        h.setModelElementId(modelElementId);
        h.setDescription(description.trim());
        h.setSource(source);
        return repo.update(h);
    }

    public SequenceOfEvents updateSequence(Long id, String description) {
        Validation.requireText(description, "La secuencia de eventos es obligatoria");
        SequenceOfEvents s = require(SequenceOfEvents.class, id, "Secuencia de eventos");
        s.setDescription(description.trim());
        return repo.update(s);
    }

    public HazardousSituation updateSituation(Long id, String description) {
        Validation.requireText(description, "La situación peligrosa es obligatoria");
        HazardousSituation s = require(HazardousSituation.class, id, "Situación peligrosa");
        s.setDescription(description.trim());
        return repo.update(s);
    }

    public Harm updateHarm(Long id, String description, String type) {
        Validation.requireText(description, "La descripción del daño es obligatoria");
        Harm h = require(Harm.class, id, "Daño");
        h.setDescription(description.trim());
        h.setType(type);
        return repo.update(h);
    }

    public Optional<RiskChain> findChain(Long harmId) {
        return repo.findChain(harmId);
    }

    public List<RiskChain> listChains() {
        return repo.listChains();
    }

    private <E> E require(Class<E> type, Long id, String label) {
        return repo.find(type, id).orElseThrow(
                () -> new NoSuchElementException(label + " no encontrado: " + id));
    }

    private void requireElementIfPresent(Long modelElementId) {
        if (modelElementId != null && !repo.elementExists(modelElementId)) {
            throw new IllegalArgumentException(
                    "El elemento del modelo asociado no existe: " + modelElementId);
        }
    }
}