// ModelLinksRepository.java
package org.example.repository;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class ModelLinksRepository {

    private final SessionFactory sf;

    public ModelLinksRepository(SessionFactory sf) {
        this.sf = sf;
    }

    /** Por ahora solo cuenta peligros (hazard). Faltan failure_mode y fha_analysis cuando existan sus entidades. */
    /** Cuenta peligros y modos de fallo. Falta fha_analysis cuando exista su entidad. */
    public boolean hasRiskInformation(Long elementId) {
        return sf.fromTransaction(s ->
                count(s, "select count(h) from Hazard h where h.modelElementId = :id", elementId) > 0
                        || count(s, "select count(m) from FailureMode m where m.modelElementId = :id", elementId) > 0);
    }

    /** Un componente enlazado a funciones, interacciones o subcomponentes no se puede borrar físicamente. */
    public boolean isComponentLinked(Long componentId) {
        return sf.fromTransaction(s ->
                count(s, "select count(f) from DeviceFunction f join f.componentIds c where c = :id", componentId) > 0
                        || count(s, "select count(i) from Interaction i join i.componentIds c where c = :id", componentId) > 0
                        || count(s, "select count(k) from Component k where k.parentComponentId = :id", componentId) > 0);
    }

    private static long count(Session s, String hql, Long id) {
        return s.createSelectionQuery(hql, Long.class).setParameter("id", id).getSingleResult();
    }
}