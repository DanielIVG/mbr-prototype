// RiskChainRepository.java
package org.example.repository;

import org.example.model.Harm;
import org.example.model.Hazard;
import org.example.model.HazardousSituation;
import org.example.model.ModelElement;
import org.example.model.RiskChain;
import org.example.model.SequenceOfEvents;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class RiskChainRepository {

    private final SessionFactory sf;

    public RiskChainRepository(SessionFactory sf) {
        this.sf = sf;
    }

    public <E> Optional<E> find(Class<E> type, Long id) {
        return sf.fromTransaction(s -> Optional.ofNullable(s.find(type, id)));
    }

    public <E> E save(E entity) {
        return sf.fromTransaction(s -> {
            s.persist(entity);
            return entity;
        });
    }

    public <E> E update(E entity) {
        return sf.fromTransaction(s -> s.merge(entity));
    }

    public boolean elementExists(Long id) {
        return sf.fromTransaction(s -> s.find(ModelElement.class, id) != null);
    }

    public int nextStepOrder(Long hazardId) {
        return sf.fromTransaction(s -> {
            Integer max = s.createSelectionQuery(
                            "select max(e.stepOrder) from SequenceOfEvents e where e.hazardId = :id",
                            Integer.class)
                    .setParameter("id", hazardId)
                    .getSingleResultOrNull();
            return (max == null ? 0 : max) + 1;
        });
    }

    /** Crea peligro, secuencia, situación y daño en una sola transacción (flujo guiado, RU-01). */
    public RiskChain createChain(Hazard hazard, String sequenceDescription,
                                 String situationDescription, String harmDescription,
                                 String harmType) {
        return createChain(hazard, List.of(new Step(sequenceDescription, null)),
                situationDescription, harmDescription, harmType);
    }

    /** Un paso de la secuencia con el componente donde ocurre (opcional). */
    public record Step(String description, Long modelElementId) {}

    /** Varios pasos; la situación peligrosa se asocia al último (misma transacción). */
    public RiskChain createChain(Hazard hazard, List<Step> steps, String situationDescription,
                                 String harmDescription, String harmType) {
        return sf.fromTransaction(s -> {
            s.persist(hazard);

            SequenceOfEvents sequence = null;
            int order = 1;
            for (Step step : steps) {
                sequence = new SequenceOfEvents();
                sequence.setHazardId(hazard.getId());
                sequence.setDescription(step.description());
                sequence.setModelElementId(step.modelElementId());
                sequence.setStepOrder(order++);
                s.persist(sequence);
            }

            HazardousSituation situation = new HazardousSituation();
            situation.setSequenceOfEventsId(sequence.getId());
            situation.setDescription(situationDescription);
            s.persist(situation);

            Harm harm = new Harm();
            harm.setHazardousSituationId(situation.getId());
            harm.setDescription(harmDescription);
            harm.setType(harmType);
            s.persist(harm);

            return new RiskChain(hazard, sequence, situation, harm);
        });
    }

    /** Pasos de la secuencia de un peligro, en orden. */
    public List<SequenceOfEvents> listSequence(Long hazardId) {
        return sf.fromTransaction(s -> s.createSelectionQuery(
                        "from SequenceOfEvents e where e.hazardId = :id order by e.stepOrder",
                        SequenceOfEvents.class)
                .setParameter("id", hazardId)
                .getResultList());
    }

    public Optional<RiskChain> findChain(Long harmId) {
        return sf.fromTransaction(s -> chain(s, harmId));
    }

    /** Una cadena por cada daño registrado. */
    public List<RiskChain> listChains() {
        return sf.fromTransaction(s -> {
            List<Long> harmIds = s.createSelectionQuery(
                    "select h.id from Harm h order by h.id", Long.class).getResultList();
            return harmIds.stream()
                    .map(id -> chain(s, id))
                    .flatMap(Optional::stream)
                    .toList();
        });
    }

    private static Optional<RiskChain> chain(Session s, Long harmId) {
        Harm harm = s.find(Harm.class, harmId);
        if (harm == null) {
            return Optional.empty();
        }
        HazardousSituation situation = s.find(HazardousSituation.class, harm.getHazardousSituationId());
        SequenceOfEvents sequence = s.find(SequenceOfEvents.class, situation.getSequenceOfEventsId());
        Hazard hazard = s.find(Hazard.class, sequence.getHazardId());
        return Optional.of(new RiskChain(hazard, sequence, situation, harm));
    }
}