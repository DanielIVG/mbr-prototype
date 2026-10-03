// Validation.java
package org.example.service;

final class Validation {

    private Validation() {}

    static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    static void requireDevice(DeviceService devices, Long deviceId) {
        if (deviceId == null) {
            throw new IllegalArgumentException("El dispositivo es obligatorio");
        }
        if (devices.find(deviceId).isEmpty()) {
            throw new IllegalArgumentException("Dispositivo inexistente: " + deviceId);
        }
    }
}