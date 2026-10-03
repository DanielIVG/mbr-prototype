// DeviceFunctionService.java
package org.example.service;

import org.example.model.DeviceFunction;
import org.example.repository.ModelElementRepository;
import org.example.repository.ModelLinksRepository;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public class DeviceFunctionService {

    private final ModelElementRepository<DeviceFunction> repo;
    private final ModelLinksRepository links;
    private final DeviceService devices;
    private final ComponentService components;

    public DeviceFunctionService(ModelElementRepository<DeviceFunction> repo,
                                 ModelLinksRepository links, DeviceService devices,
                                 ComponentService components) {
        this.repo = repo;
        this.links = links;
        this.devices = devices;
        this.components = components;
    }

    public DeviceFunction create(Long deviceId, String name, String description,
                                 Set<Long> componentIds) {
        Validation.requireDevice(devices, deviceId);
        Validation.requireText(name, "El nombre es obligatorio");
        Set<Long> ids = components.requireSameDevice(deviceId, componentIds);
        DeviceFunction f = new DeviceFunction();
        f.setDeviceId(deviceId);
        f.setName(name.trim());
        f.setDescription(description);
        f.setComponentIds(new HashSet<>(ids));
        return repo.save(f);
    }

    public DeviceFunction update(Long id, String name, String description,
                                 Set<Long> componentIds) {
        DeviceFunction f = get(id);
        Validation.requireText(name, "El nombre es obligatorio");
        Set<Long> ids = components.requireSameDevice(f.getDeviceId(), componentIds);
        f.setName(name.trim());
        f.setDescription(description);
        f.setComponentIds(new HashSet<>(ids));
        return repo.update(f);
    }

    public void deactivate(Long id) {
        DeviceFunction f = get(id);
        f.setActive(false);
        repo.update(f);
    }

    public void delete(Long id) {
        DeviceFunction f = get(id);
        if (links.hasRiskInformation(id)) {
            throw new ConflictException(
                    "La función tiene información de riesgo asociada: no se puede eliminar, solo desactivar");
        }
        repo.delete(f);
    }

    public Optional<DeviceFunction> find(Long id) {
        return repo.findById(id);
    }

    public List<DeviceFunction> listActive(Long deviceId) {
        return repo.findActiveByDevice(deviceId);
    }

    private DeviceFunction get(Long id) {
        return repo.findById(id).orElseThrow(
                () -> new NoSuchElementException("Función no encontrada: " + id));
    }
}