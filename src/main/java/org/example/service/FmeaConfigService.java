// FmeaConfigService.java
package org.example.service;

import org.example.model.FmeaScaleLevel;
import org.example.model.RpnPriorityCategory;
import org.example.repository.FmeaRepository;
import org.example.rpn.FactorType;
import org.example.rpn.PriorityCategory;
import org.example.rpn.RpnCalculator;
import org.example.rpn.ScaleLevel;

import java.util.List;

/** Escalas S/O/D y categorías de prioridad definidas para el caso de estudio. */
public class FmeaConfigService {

    private final FmeaRepository repo;

    public FmeaConfigService(FmeaRepository repo) {
        this.repo = repo;
    }

    public FmeaScaleLevel addScaleLevel(FactorType type, int value, String descriptor) {
        if (type == null) {
            throw new IllegalArgumentException("El tipo de factor es obligatorio");
        }
        new ScaleLevel(type, value, descriptor); // valida valor y descriptor
        if (repo.scaleLevelExists(type, value)) {
            throw new ConflictException("Ya existe el nivel " + value + " en la escala de " + type);
        }
        FmeaScaleLevel level = new FmeaScaleLevel();
        level.setFactorType(type);
        level.setLevelValue(value);
        level.setDescriptor(descriptor.trim());
        return repo.save(level);
    }

    public RpnPriorityCategory addCategory(String name, int minRpn, int maxRpn, String justification) {
        new PriorityCategory(name, minRpn, maxRpn, justification); // valida rango y justificación
        RpnPriorityCategory category = new RpnPriorityCategory();
        category.setName(name.trim());
        category.setMinRpn(minRpn);
        category.setMaxRpn(maxRpn);
        category.setJustification(justification.trim());
        return repo.save(category);
    }

    public List<FmeaScaleLevel> scaleLevels() {
        return repo.scaleLevels();
    }

    public List<RpnPriorityCategory> categories() {
        return repo.categories();
    }

    /** Arma el motor con la configuración actual; falla si está incompleta o es inconsistente. */
    public RpnCalculator calculator() {
        List<ScaleLevel> levels = repo.scaleLevels().stream()
                .map(l -> new ScaleLevel(l.getFactorType(), l.getLevelValue(), l.getDescriptor()))
                .toList();
        List<PriorityCategory> categories = repo.categories().stream()
                .map(c -> new PriorityCategory(c.getName(), c.getMinRpn(), c.getMaxRpn(),
                        c.getJustification()))
                .toList();
        try {
            return new RpnCalculator(levels, categories);
        } catch (IllegalArgumentException e) {
            throw new ConflictException("La configuración de escalas y categorías del FMEA "
                    + "está incompleta o es inconsistente: " + e.getMessage());
        }
    }
}