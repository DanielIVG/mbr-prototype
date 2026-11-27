// FmeaRepository.java
package org.example.repository;

import org.example.model.FailureMode;
import org.example.model.FmeaAnalysis;
import org.example.model.FmeaScaleLevel;
import org.example.model.RpnFactor;
import org.example.model.RpnPriorityCategory;
import org.example.rpn.FactorType;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class FmeaRepository {

    private final SessionFactory sf;

    public FmeaRepository(SessionFactory sf) {
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

    public List<FmeaScaleLevel> scaleLevels() {
        return sf.fromTransaction(s -> s.createSelectionQuery(
                "from FmeaScaleLevel l order by l.factorType, l.levelValue",
                FmeaScaleLevel.class).getResultList());
    }

    public boolean scaleLevelExists(FactorType type, int value) {
        return sf.fromTransaction(s -> s.createSelectionQuery(
                        "select count(l) from FmeaScaleLevel l "
                                + "where l.factorType = :type and l.levelValue = :value", Long.class)
                .setParameter("type", type)
                .setParameter("value", value)
                .getSingleResult() > 0);
    }

    public List<RpnPriorityCategory> categories() {
        return sf.fromTransaction(s -> s.createSelectionQuery(
                "from RpnPriorityCategory c order by c.minRpn",
                RpnPriorityCategory.class).getResultList());
    }

    /** Crea el análisis y su primera fila de RPN en una sola transacción. */
    public FmeaAnalysis createAnalysis(FmeaAnalysis analysis, RpnFactor first) {
        return sf.fromTransaction(s -> {
            s.persist(analysis);
            first.setFmeaAnalysisId(analysis.getId());
            s.persist(first);
            return analysis;
        });
    }

    /**
     * Reevaluación (RF-29): primero se desactiva la fila vigente y después se inserta la nueva.
     * El orden importa: el índice único parcial solo admite una fila vigente por análisis.
     */
    public RpnFactor replaceCurrent(Long analysisId, RpnFactor next) {
        return sf.fromTransaction(s -> {
            s.createMutationQuery(
                            "update RpnFactor f set f.latest = false "
                                    + "where f.fmeaAnalysisId = :id and f.latest = true")
                    .setParameter("id", analysisId)
                    .executeUpdate();
            s.persist(next);
            return next;
        });
    }

    public Optional<RpnFactor> currentFactor(Long analysisId) {
        return sf.fromTransaction(s -> s.createSelectionQuery(
                        "from RpnFactor f where f.fmeaAnalysisId = :id and f.latest = true",
                        RpnFactor.class)
                .setParameter("id", analysisId)
                .getResultList().stream().findFirst());
    }

    public List<RpnFactor> history(Long analysisId) {
        return sf.fromTransaction(s -> s.createSelectionQuery(
                        "from RpnFactor f where f.fmeaAnalysisId = :id order by f.id",
                        RpnFactor.class)
                .setParameter("id", analysisId)
                .getResultList());
    }

    public List<FmeaAnalysis> analysesByHarm(Long harmId) {
        return sf.fromTransaction(s -> s.createSelectionQuery(
                        "from FmeaAnalysis a where a.harmId = :id and a.active = true order by a.id",
                        FmeaAnalysis.class)
                .setParameter("id", harmId)
                .getResultList());
    }

    public <E> E update(E entity) {
        return sf.fromTransaction(s -> s.merge(entity));
    }

    public boolean behaviorNodeExists(Long nodeId) {
        return sf.fromTransaction(s -> s.createNativeQuery(
                        "SELECT COUNT(*) FROM behavior_node WHERE id = :id AND active = 1", Long.class)
                .setParameter("id", nodeId)
                .getSingleResult() > 0);
    }

    public List<FailureMode> failureModesByElement(Long modelElementId) {
        return sf.fromTransaction(s -> s.createSelectionQuery(
                        "from FailureMode f where f.modelElementId = :id order by f.id",
                        FailureMode.class)
                .setParameter("id", modelElementId)
                .getResultList());
    }

    /** Modos de falla vinculados a un nodo del árbol de comportamiento. */
    public List<FailureMode> failureModesByBehaviorNode(Long behaviorNodeId) {
        return sf.fromTransaction(s -> s.createSelectionQuery(
                        "from FailureMode f where f.behaviorNodeId = :id order by f.id",
                        FailureMode.class)
                .setParameter("id", behaviorNodeId)
                .getResultList());
    }

    /** Obtiene el valor de ocurrencia actual del análisis FMEA más reciente de un modo de falla. */
    public Optional<Integer> currentOccurrenceByFailureMode(Long failureModeId) {
        return sf.fromTransaction(s -> {
            var results = s.createNativeQuery(
                            "SELECT rf.occurrence FROM rpn_factor rf " +
                                    "JOIN fmea_analysis fa ON rf.fmea_analysis_id = fa.id " +
                                    "WHERE fa.failure_mode_id = :fmId AND rf.is_current = 1 " +
                                    "ORDER BY rf.id DESC LIMIT 1", Integer.class)
                    .setParameter("fmId", failureModeId)
                    .getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        });
    }
}