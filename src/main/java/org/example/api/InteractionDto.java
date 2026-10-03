// InteractionDto.java
package org.example.api;

import org.example.model.Interaction;

import java.util.Set;
import java.util.TreeSet;

public record InteractionDto(Long id, Long deviceId, String description,
                             String relevanceJustification, Set<Long> componentIds,
                             boolean active) {

    public static InteractionDto from(Interaction i) {
        return new InteractionDto(i.getId(), i.getDeviceId(), i.getDescription(),
                i.getRelevanceJustification(), new TreeSet<>(i.getComponentIds()), i.isActive());
    }
}