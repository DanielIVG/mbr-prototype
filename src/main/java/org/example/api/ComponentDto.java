// ComponentDto.java
package org.example.api;

import org.example.model.Component;

public record ComponentDto(Long id, Long deviceId, Long parentComponentId, String name,
                           String description, boolean essential, String essentialJustification,
                           boolean active) {

    public static ComponentDto from(Component c) {
        return new ComponentDto(c.getId(), c.getDeviceId(), c.getParentComponentId(), c.getName(),
                c.getDescription(), c.isEssential(), c.getEssentialJustification(), c.isActive());
    }
}