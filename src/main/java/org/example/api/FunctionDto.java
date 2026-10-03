// FunctionDto.java
package org.example.api;

import org.example.model.DeviceFunction;

import java.util.Set;
import java.util.TreeSet;

public record FunctionDto(Long id, Long deviceId, String name, String description,
                          Set<Long> componentIds, boolean active) {

    public static FunctionDto from(DeviceFunction f) {
        return new FunctionDto(f.getId(), f.getDeviceId(), f.getName(), f.getDescription(),
                new TreeSet<>(f.getComponentIds()), f.isActive());
    }
}