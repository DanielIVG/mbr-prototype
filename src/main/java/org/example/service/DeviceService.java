package org.example.service;

import org.example.model.Device;
import org.example.repository.DeviceRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class DeviceService {

    private final DeviceRepository repo;

    public DeviceService(DeviceRepository repo) {
        this.repo = repo;
    }

    public Device create(String name, String intendedUse, String misuse) {
        requireText(name, "El nombre es obligatorio");
        requireText(intendedUse, "El uso previsto es obligatorio");
        Device d = new Device();
        d.setName(name.trim());
        d.setIntendedUse(intendedUse.trim());
        d.setReasonablyForeseeableMisuse(misuse);
        return repo.save(d);
    }

    public Device update(Long id, String name, String intendedUse, String misuse) {
        requireText(name, "El nombre es obligatorio");
        requireText(intendedUse, "El uso previsto es obligatorio");
        Device d = get(id);
        d.setName(name.trim());
        d.setIntendedUse(intendedUse.trim());
        d.setReasonablyForeseeableMisuse(misuse);
        return repo.update(d);
    }

    /** No se borra físicamente: se marca como inactivo. */
    public void deactivate(Long id) {
        Device d = get(id);
        d.setActive(false);
        repo.update(d);
    }

    public Optional<Device> find(Long id) {
        return repo.findById(id);
    }

    public List<Device> listActive() {
        return repo.findAllActive();
    }

    private Device get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Dispositivo no encontrado: " + id));
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}