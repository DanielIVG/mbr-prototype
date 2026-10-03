package org.example.api;

import org.example.model.Device;

public record DeviceDto(
        Long id,
        String name,
        String intendedUse,
        String reasonablyForeseeableMisuse,
        boolean active) {

    public static DeviceDto from(Device d) {
        return new DeviceDto(d.getId(), d.getName(), d.getIntendedUse(),
                d.getReasonablyForeseeableMisuse(), d.isActive());
    }
}