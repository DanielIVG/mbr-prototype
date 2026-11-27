// InteractionService.java
package org.example.service;

import org.example.model.DeviceFunction;
import org.example.model.Interaction;
import org.example.repository.ModelElementRepository;
import org.example.repository.ModelLinksRepository;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class InteractionService {

    private final ModelElementRepository<Interaction> repo;
    private final ModelLinksRepository links;
    private final DeviceService devices;
    private final DeviceFunctionService functions;

    public InteractionService(ModelElementRepository<Interaction> repo,
                              ModelLinksRepository links, DeviceService devices,
                              DeviceFunctionService functions) {
        this.repo = repo;
        this.links = links;
        this.devices = devices;
        this.functions = functions;
    }

    /**
     * Crea una interacción enlazando funciones.
     * @param functionIds funciones que participan en la interacción (mínimo 2)
     */
    public Interaction create(Long deviceId, String description, String relevance,
                              Set<Long> functionIds) {
        Validation.requireDevice(devices, deviceId);
        Validation.requireText(description, "La descripción es obligatoria");
        Validation.requireText(relevance, "La justificación de relevancia es obligatoria");
        Set<Long> ids = requireAtLeastTwoFunctions(deviceId, functionIds);
        Interaction i = new Interaction();
        i.setDeviceId(deviceId);
        i.setDescription(description.trim());
        i.setRelevanceJustification(relevance.trim());
        i.setFunctionIds(new HashSet<>(ids));
        // componentIds se deriva de las funciones
        i.setComponentIds(deriveComponentIds(ids));
        return repo.save(i);
    }

    /**
     * Actualiza una interacción enlazando funciones.
     * @param functionIds funciones que participan en la interacción (mínimo 2)
     */
    public Interaction update(Long id, String description, String relevance,
                              Set<Long> functionIds) {
        Interaction i = get(id);
        Validation.requireText(description, "La descripción es obligatoria");
        Validation.requireText(relevance, "La justificación de relevancia es obligatoria");
        Set<Long> ids = requireAtLeastTwoFunctions(i.getDeviceId(), functionIds);
        i.setDescription(description.trim());
        i.setRelevanceJustification(relevance.trim());
        i.setFunctionIds(new HashSet<>(ids));
        // componentIds se deriva de las funciones
        i.setComponentIds(deriveComponentIds(ids));
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

    /**
     * Valida que haya al menos 2 funciones y que todas pertenezcan al dispositivo.
     */
    private Set<Long> requireAtLeastTwoFunctions(Long deviceId, Set<Long> functionIds) {
        if (functionIds == null || functionIds.size() < 2) {
            throw new IllegalArgumentException(
                    "Una interacción requiere al menos dos funciones distintas");
        }
        Set<Long> validated = new HashSet<>();
        for (Long fid : functionIds) {
            DeviceFunction f = functions.find(fid).orElseThrow(
                    () -> new NoSuchElementException("Función no encontrada: " + fid));
            if (!f.getDeviceId().equals(deviceId)) {
                throw new IllegalArgumentException(
                        "La función " + fid + " pertenece a otro dispositivo");
            }
            validated.add(fid);
        }
        return validated;
    }

    /**
     * Deriva los IDs de componentes de las funciones involucradas.
     */
    private Set<Long> deriveComponentIds(Set<Long> functionIds) {
        Set<Long> componentIds = new HashSet<>();
        for (Long fid : functionIds) {
            functions.find(fid).ifPresent(f -> componentIds.addAll(f.getComponentIds()));
        }
        return componentIds;
    }
}