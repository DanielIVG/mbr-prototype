// ComponentService.java
package org.example.service;

import org.example.model.Component;
import org.example.repository.ModelElementRepository;
import org.example.repository.ModelLinksRepository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public class ComponentService {

    private final ModelElementRepository<Component> repo;
    private final ModelLinksRepository links;
    private final DeviceService devices;

    public ComponentService(ModelElementRepository<Component> repo,
                            ModelLinksRepository links, DeviceService devices) {
        this.repo = repo;
        this.links = links;
        this.devices = devices;
    }

    public Component create(Long deviceId, Long parentId, String name, String description,
                            boolean essential, String justification) {
        Validation.requireDevice(devices, deviceId);
        validate(deviceId, null, parentId, name, essential, justification);
        Component c = new Component();
        c.setDeviceId(deviceId);
        apply(c, parentId, name, description, essential, justification);
        return repo.save(c);
    }

    public Component update(Long id, Long parentId, String name, String description,
                            boolean essential, String justification) {
        Component c = get(id);
        validate(c.getDeviceId(), id, parentId, name, essential, justification);
        apply(c, parentId, name, description, essential, justification);
        return repo.update(c);
    }

    /** Desactivación lógica (RF-08): conserva las relaciones con el riesgo. */
    public void deactivate(Long id) {
        Component c = get(id);
        c.setActive(false);
        repo.update(c);
    }

    /** Eliminación física solo si no hay riesgo asociado ni es esencial (RF-07). */
    public void delete(Long id) {
        Component c = get(id);
        if (c.isEssential()) {
            throw new ConflictException(
                    "El componente es esencial: no se puede eliminar, solo desactivar");
        }
        if (links.hasRiskInformation(id)) {
            throw new ConflictException(
                    "El componente tiene información de riesgo asociada: no se puede eliminar, solo desactivar");
        }
        if (links.isComponentLinked(id)) {
            throw new ConflictException(
                    "El componente está vinculado a funciones, interacciones o subcomponentes: desvincúlelo o desactívelo");
        }
        repo.delete(c);
    }

    public Optional<Component> find(Long id) {
        return repo.findById(id);
    }

    public List<Component> listActive(Long deviceId) {
        return repo.findActiveByDevice(deviceId);
    }

    /** Verifica que todos los componentes existan y sean del mismo dispositivo. */
    public Set<Long> requireSameDevice(Long deviceId, Collection<Long> ids) {
        Set<Long> result = new HashSet<>();
        if (ids == null) {
            return result;
        }
        for (Long id : ids) {
            Component c = find(id).orElseThrow(
                    () -> new IllegalArgumentException("Componente inexistente: " + id));
            if (!deviceId.equals(c.getDeviceId())) {
                throw new IllegalArgumentException("El componente " + id + " pertenece a otro dispositivo");
            }
            result.add(id);
        }
        return result;
    }

    private Component get(Long id) {
        return repo.findById(id).orElseThrow(
                () -> new NoSuchElementException("Componente no encontrado: " + id));
    }

    private void validate(Long deviceId, Long selfId, Long parentId, String name,
                          boolean essential, String justification) {
        Validation.requireText(name, "El nombre es obligatorio");
        if (essential) {
            Validation.requireText(justification,
                    "Un componente esencial requiere justificación (RF-03)");
        }
        if (parentId == null) {
            return;
        }
        Component parent = find(parentId).orElseThrow(
                () -> new IllegalArgumentException("Componente padre inexistente: " + parentId));
        if (!deviceId.equals(parent.getDeviceId())) {
            throw new IllegalArgumentException("El componente padre pertenece a otro dispositivo");
        }
        Long cursor = parentId;
        while (cursor != null) {
            if (cursor.equals(selfId)) {
                throw new IllegalArgumentException("La jerarquía de componentes no puede ser circular");
            }
            cursor = find(cursor).map(Component::getParentComponentId).orElse(null);
        }
    }

    private static void apply(Component c, Long parentId, String name, String description,
                              boolean essential, String justification) {
        c.setParentComponentId(parentId);
        c.setName(name.trim());
        c.setDescription(description);
        c.setEssential(essential);
        c.setEssentialJustification(essential ? justification.trim() : null);
    }
}