// InteractionDto.java
package org.example.api;

import org.example.model.Interaction;

import java.util.Set;
import java.util.TreeSet;

/**
 * DTO de interacción.
 * @param functionIds funciones que participan en la interacción (V9+)
 * @param componentIds componentes derivados de las funciones (solo lectura; para compatibilidad)
 */
public record InteractionDto(Long id, Long deviceId, String description,
                             String relevanceJustification, Set<Long> functionIds,
                             Set<Long> componentIds, boolean active) {

    public static InteractionDto from(Interaction i) {
        return new InteractionDto(i.getId(), i.getDeviceId(), i.getDescription(),
                i.getRelevanceJustification(), new TreeSet<>(i.getFunctionIds()),
                new TreeSet<>(i.getComponentIds()), i.isActive());
    }

    /** Constructor de compatibilidad para clientes que aún envían componentIds. */
    public static InteractionDto fromLegacy(Long id, Long deviceId, String description,
                                            String relevanceJustification, Set<Long> componentIds,
                                            boolean active) {
        return new InteractionDto(id, deviceId, description, relevanceJustification,
                Set.of(), componentIds, active);
    }
}