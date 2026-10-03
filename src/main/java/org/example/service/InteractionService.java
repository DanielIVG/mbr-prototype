// InteractionService.java
package org.example.service;

import org.example.model.Interaction;
import org.example.repository.ModelElementRepository;
import org.example.repository.ModelLinksRepository;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public class InteractionService {

    private final ModelElementRepository<Interaction> repo;
    private final ModelLinksRepository links;
    private final DeviceService devices;
    private final ComponentService components;

    public InteractionService(ModelElementRepository<Interaction> repo,
                              ModelLinksRepository links, DeviceService devices,
                              ComponentService components) {
        this.repo = repo;
        this.links = links;
        this.devices = devices;
        this.components = components;
    }

    public Interaction create(Long deviceId, String description, String relevance,
                              Set<Long> componentIds) {
        Validation.requireDevice(devices, deviceId);
        Validation.requireText(description, "La descripción es obligatoria");
        Validation.requireText(relevance, "La justificación de relevancia es obligatoria");
        Set<Long> ids = requireAtLeastTwo(components.requireSameDevice(deviceId, componentIds));
        Interaction i = new Interaction();
        i.setDeviceId(deviceId);
        i.setDescription(description.trim());
        i.setRelevanceJustification(relevance.trim());
        i.setComponentIds(new HashSet<>(ids));
        return repo.save(i);
    }

    public Interaction update(Long id, String description, String relevance,
                              Set<Long> componentIds) {
        Interaction i = get(id);
        Validation.requireText(description, "La descripción es obligatoria");
        Validation.requireText(relevance, "La justificación de relevancia es obligatoria");
        Set<Long> ids = requireAtLeastTwo(components.requireSameDevice(i.getDeviceId(), componentIds));
        i.setDescription(description.trim());
        i.setRelevanceJustification(relevance.trim());
        i.setComponentIds(new HashSet<>(ids));
        return repo.update(i);
    }

    public void deactivate(Long id) {
        Interaction i = get(id);
        i.setActive(false);
        repo.update(i);
    }

    public void delete(Long id) {
        Interaction i = get(id);
        if (links.hasRiskInformation(id)) {
            throw new ConflictException(
                    "La interacción tiene información de riesgo asociada: no se puede eliminar, solo desactivar");
        }
        repo.delete(i);
    }

    public Optional<Interaction> find(Long id) {
        return repo.findById(id);
    }

    public List<Interaction> listActive(Long deviceId) {
        return repo.findActiveByDevice(deviceId);
    }

    private Interaction get(Long id) {
        return repo.findById(id).orElseThrow(
                () -> new NoSuchElementException("Interacción no encontrada: " + id));
    }

    private static Set<Long> requireAtLeastTwo(Set<Long> ids) {
        if (ids.size() < 2) {
            throw new IllegalArgumentException(
                    "Una interacción requiere al menos dos componentes distintos (RF-04)");
        }
        return ids;
    }
}