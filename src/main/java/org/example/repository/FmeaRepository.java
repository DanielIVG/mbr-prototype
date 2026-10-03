// FmeaRepository.java
package org.example.repository;

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
}