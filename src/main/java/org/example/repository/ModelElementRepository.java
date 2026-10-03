// ModelElementRepository.java
package org.example.repository;

import org.example.model.ModelElement;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

/** Repositorio común de Function, Component e Interaction (no de Device). */
public class ModelElementRepository<T extends ModelElement> {

    public static final String PENDING_REVIEW = "PENDIENTE_REVISION";

    private final SessionFactory sf;
    private final Class<T> type;

    public ModelElementRepository(SessionFactory sf, Class<T> type) {
        this.sf = sf;
        this.type = type;
    }

    public T save(T element) {
        return sf.fromTransaction(s -> {
            s.persist(element);
            return element;
        });
    }

    /** Guarda el cambio y marca como pendiente de revisión el riesgo asociado (RF-06), en una sola transacción. */
    public T update(T element) {
        return sf.fromTransaction(s -> {
            T merged = s.merge(element);
            markRiskPending(s, merged.getId());
            return merged;
        });
    }

    public Optional<T> findById(Long id) {
        return sf.fromTransaction(s -> Optional.ofNullable(s.find(type, id)));
    }

    public List<T> findActiveByDevice(Long deviceId) {
        return sf.fromTransaction(s -> s.createSelectionQuery(
                        "from " + type.getSimpleName()
                                + " e where e.deviceId = :deviceId and e.active = true order by e.id", type)
                .setParameter("deviceId", deviceId)
                .getResultList());
    }

    public void delete(T element) {
        sf.inTransaction(s -> s.remove(s.merge(element)));
    }

    private static void markRiskPending(Session s, Long elementId) {
        s.createMutationQuery(
                        "update Hazard h set h.reviewStatus = :status where h.modelElementId = :id")
                .setParameter("status", PENDING_REVIEW)
                .setParameter("id", elementId)
                .executeUpdate();
    }
}