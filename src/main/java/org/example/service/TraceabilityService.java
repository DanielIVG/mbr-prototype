package org.example.service;

import org.example.repository.TraceabilityRepository;
import org.example.repository.TraceabilityRepository.ControlRef;
import org.example.repository.TraceabilityRepository.ElementRef;
import org.example.repository.TraceabilityRepository.HarmRef;
import org.example.repository.TraceabilityRepository.RequirementRow;
import org.example.repository.TraceabilityRepository.TraceRow;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/** Requisitos y trazabilidad requisito → medida de control → verificación → daño → componente. */
public class TraceabilityService {

    public static final List<String> TYPES =
            List.of("FUNCIONAL", "SEGURIDAD", "USABILIDAD", "DESEMPENO", "REGULATORIO");

    /** Cobertura de la trazabilidad de un dispositivo. */
    public record Summary(int requirements, int requirementsWithControl, int controls,
                          int controlsWithRequirement, int controlsVerified) {}

    public record Matrix(List<TraceRow> rows, List<RequirementRow> requirementsWithoutControl,
                         List<ControlRef> controlsWithoutRequirement, Summary summary) {}

    private final TraceabilityRepository repository;
    private final DeviceService devices;

    public TraceabilityService(TraceabilityRepository repository, DeviceService devices) {
        this.repository = repository;
        this.devices = devices;
    }

    // ------------------------------------------------------------------ requisitos

    public RequirementRow createRequirement(Long deviceId, String code, String description, String type,
                                            String source) {
        Validation.requireDevice(devices, deviceId);
        String c = normalizedCode(code);
        String d = requiredDescription(description);
        String t = normalizedType(type);
        if (repository.codeExists(deviceId, c, null)) {
            throw new ConflictException("Ya existe un requisito con el código " + c);
        }
        long id = repository.addRequirement(deviceId, c, d, t, blankToNull(source));
        return requirement(id);
    }

    public RequirementRow updateRequirement(Long id, String code, String description, String type,
                                            String source) {
        RequirementRow current = requirement(id);
        String c = normalizedCode(code);
        String d = requiredDescription(description);
        String t = normalizedType(type);
        if (repository.codeExists(current.deviceId(), c, id)) {
            throw new ConflictException("Ya existe un requisito con el código " + c);
        }
        repository.updateRequirement(id, c, d, t, blankToNull(source));
        return requirement(id);
    }

    public void deactivateRequirement(Long id) {
        requirement(id);
        repository.deactivateRequirement(id);
    }

    public List<RequirementRow> listRequirements(Long deviceId) {
        Validation.requireDevice(devices, deviceId);
        return repository.listRequirements(deviceId);
    }

    public RequirementRow requirement(Long id) {
        return repository.findRequirement(id).orElseThrow(
                () -> new NoSuchElementException("Requisito no encontrado: " + id));
    }

    // ------------------------------------------------------------------ enlaces

    public void link(Long requirementId, Long controlId) {
        RequirementRow r = requirement(requirementId);
        if (controlId == null) {
            throw new IllegalArgumentException("La medida de control es obligatoria");
        }
        Long controlDevice = repository.deviceOfControl(controlId).orElseThrow(
                () -> new NoSuchElementException("Medida de control no encontrada: " + controlId));
        if (controlDevice != r.deviceId()) {
            throw new IllegalArgumentException(
                    "La medida de control pertenece a otro dispositivo y no puede enlazarse a este requisito");
        }
        if (repository.linkExists(requirementId, controlId)) {
            throw new ConflictException("El requisito ya está enlazado a esa medida de control");
        }
        repository.link(requirementId, controlId);
    }

    public void unlink(Long requirementId, Long controlId) {
        requirement(requirementId);
        if (!repository.unlink(requirementId, controlId)) {
            throw new NoSuchElementException("El requisito no está enlazado a esa medida de control");
        }
    }

    // ------------------------------------------------------------------ consultas

    public List<ControlRef> controls(Long deviceId) {
        Validation.requireDevice(devices, deviceId);
        return repository.controlsOfDevice(deviceId);
    }

    public Matrix matrix(Long deviceId) {
        Validation.requireDevice(devices, deviceId);
        List<TraceRow> rows = repository.matrix(deviceId);
        List<RequirementRow> requirements = repository.listRequirements(deviceId);
        List<ControlRef> controls = repository.controlsOfDevice(deviceId);

        Set<Long> linkedRequirements = new HashSet<>();
        rows.forEach(r -> linkedRequirements.add(r.requirementId()));
        List<RequirementRow> withoutControl = requirements.stream()
                .filter(r -> !linkedRequirements.contains(r.id())).toList();
        List<ControlRef> withoutRequirement = controls.stream().filter(k -> k.requirements() == 0).toList();
        int verified = (int) controls.stream().filter(k -> k.verifications() > 0).count();

        Summary summary = new Summary(requirements.size(), linkedRequirements.size(), controls.size(),
                controls.size() - withoutRequirement.size(), verified);
        return new Matrix(rows, withoutControl, withoutRequirement, summary);
    }

    /** Hacia atrás: componentes por los que pasó el daño. */
    public List<ElementRef> componentsOfHarm(Long harmId) {
        if (harmId == null) {
            throw new IllegalArgumentException("El daño es obligatorio");
        }
        return repository.componentsOfHarm(harmId);
    }

    /** Hacia delante: daños a los que lleva el componente. */
    public List<HarmRef> harmsOfComponent(Long componentId) {
        if (componentId == null) {
            throw new IllegalArgumentException("El componente es obligatorio");
        }
        return repository.harmsOfComponent(componentId);
    }

    // ------------------------------------------------------------------ validación

    private static String normalizedCode(String code) {
        Validation.requireText(code, "El código del requisito es obligatorio");
        String c = code.trim();
        if (c.length() > 30) {
            throw new IllegalArgumentException("El código del requisito no puede pasar de 30 caracteres");
        }
        return c;
    }

    private static String requiredDescription(String description) {
        Validation.requireText(description, "La descripción del requisito es obligatoria");
        return description.trim();
    }

    private static String normalizedType(String type) {
        Validation.requireText(type, "El tipo de requisito es obligatorio");
        String t = type.trim().toUpperCase();
        if (!TYPES.contains(t)) {
            throw new IllegalArgumentException("Tipo de requisito no válido: " + type + ". Use uno de " + TYPES);
        }
        return t;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
