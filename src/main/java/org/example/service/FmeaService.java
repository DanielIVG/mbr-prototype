// FmeaService.java
package org.example.service;

import org.example.model.FailureMode;
import org.example.model.FmeaAnalysis;
import org.example.model.FmeaScaleLevel;
import org.example.model.FmeaView;
import org.example.model.Harm;
import org.example.model.ModelElement;
import org.example.model.RpnFactor;
import org.example.model.RpnPriorityCategory;
import org.example.repository.FmeaRepository;
import org.example.rpn.FactorType;
import org.example.rpn.PriorityCategory;
import org.example.rpn.RpnCalculator;
import org.example.rpn.RpnResult;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class FmeaService {

    private final FmeaRepository repo;
    private final FmeaConfigService config;

    public FmeaService(FmeaRepository repo, FmeaConfigService config) {
        this.repo = repo;
        this.config = config;
    }

    public FailureMode createFailureMode(Long modelElementId, String description) {
        Validation.requireText(description, "La descripción del modo de fallo es obligatoria");
        if (modelElementId != null && repo.find(ModelElement.class, modelElementId).isEmpty()) {
            throw new IllegalArgumentException(
                    "El elemento del modelo asociado no existe: " + modelElementId);
        }
        FailureMode mode = new FailureMode();
        mode.setModelElementId(modelElementId);
        mode.setDescription(description.trim());
        return repo.save(mode);
    }

    public Optional<FailureMode> findFailureMode(Long id) {
        return repo.find(FailureMode.class, id);
    }

    /** Crea un análisis FMEA y calcula su RPN (RF-12, RF-21, RF-22, RF-25). */
    public FmeaView createAnalysis(Long harmId, Long failureModeId, String effect, String cause,
                                   String detectionMethod, String existingControls, String source,
                                   int severity, int occurrence, int detection) {
        Validation.requireText(effect, "El efecto es obligatorio");
        require(Harm.class, harmId, "Daño");
        require(FailureMode.class, failureModeId, "Modo de fallo");
        RpnResult result = config.calculator().evaluate(severity, occurrence, detection);

        FmeaAnalysis analysis = new FmeaAnalysis();
        analysis.setHarmId(harmId);
        analysis.setFailureModeId(failureModeId);
        analysis.setEffect(effect.trim());
        analysis.setCause(cause);
        analysis.setDetectionMethod(detectionMethod);
        analysis.setExistingControls(existingControls);
        analysis.setSource(source);
        analysis.setCreatedDate(LocalDate.now().toString());

        RpnFactor factor = factorFrom(result);
        repo.createAnalysis(analysis, factor);
        return view(analysis, factor);
    }

    /**
     * Cambia S, O o D y recalcula el RPN (RF-24). La fila anterior se conserva como
     * historial (RF-29). Si los valores no cambian, no se crea una fila nueva.
     */
    public FmeaView reevaluate(Long analysisId, int severity, int occurrence, int detection) {
        FmeaAnalysis analysis = require(FmeaAnalysis.class, analysisId, "Análisis FMEA");
        RpnFactor current = repo.currentFactor(analysisId).orElseThrow(
                () -> new NoSuchElementException("El análisis no tiene valores de RPN: " + analysisId));
        if (current.getSeverity() == severity && current.getOccurrence() == occurrence
                && current.getDetection() == detection) {
            return view(analysis, current);
        }
        RpnResult result = config.calculator().evaluate(severity, occurrence, detection);
        RpnFactor next = factorFrom(result);
        next.setFmeaAnalysisId(analysisId);
        repo.replaceCurrent(analysisId, next);
        return view(analysis, next);
    }

    public Optional<FmeaView> find(Long analysisId) {
        return repo.find(FmeaAnalysis.class, analysisId).map(a -> {
            RpnFactor current = repo.currentFactor(a.getId()).orElseThrow();
            return view(a, current);
        });
    }

    public List<FmeaView> listByHarm(Long harmId) {
        return repo.analysesByHarm(harmId).stream()
                .map(a -> view(a, repo.currentFactor(a.getId()).orElseThrow()))
                .toList();
    }

    /** Historial de valores S, O, D y RPN, del más antiguo al vigente. */
    public List<RpnFactor> history(Long analysisId) {
        require(FmeaAnalysis.class, analysisId, "Análisis FMEA");
        return repo.history(analysisId);
    }

    private RpnFactor factorFrom(RpnResult r) {
        RpnFactor f = new RpnFactor();
        f.setSeverity(r.severity());
        f.setOccurrence(r.occurrence());
        f.setDetection(r.detection());
        f.setRpn(r.rpn());
        f.setSeverityLevelId(levelId(FactorType.SEVERIDAD, r.severity()));
        f.setOccurrenceLevelId(levelId(FactorType.OCURRENCIA, r.occurrence()));
        f.setDetectionLevelId(levelId(FactorType.DETECCION, r.detection()));
        f.setPriorityCategoryId(categoryId(r.category()));
        f.setLatest(true);
        return f;
    }

    private Long levelId(FactorType type, int value) {
        return config.scaleLevels().stream()
                .filter(l -> l.getFactorType() == type && l.getLevelValue() == value)
                .map(FmeaScaleLevel::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Nivel no encontrado: " + type + " " + value));
    }

    private Long categoryId(PriorityCategory category) {
        if (category == null) {
            return null;
        }
        return config.categories().stream()
                .filter(c -> c.getMinRpn() == category.minRpn() && c.getMaxRpn() == category.maxRpn())
                .map(RpnPriorityCategory::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Categoría no encontrada: " + category.name()));
    }

    /** Reconstruye el resultado desde lo GUARDADO, no desde la configuración actual (RNF-04, RNF-06). */
    private FmeaView view(FmeaAnalysis analysis, RpnFactor f) {
        FmeaScaleLevel s = require(FmeaScaleLevel.class, f.getSeverityLevelId(), "Nivel de severidad");
        FmeaScaleLevel o = require(FmeaScaleLevel.class, f.getOccurrenceLevelId(), "Nivel de ocurrencia");
        FmeaScaleLevel d = require(FmeaScaleLevel.class, f.getDetectionLevelId(), "Nivel de detección");
        PriorityCategory category = null;
        if (f.getPriorityCategoryId() != null) {
            RpnPriorityCategory c = require(RpnPriorityCategory.class, f.getPriorityCategoryId(), "Categoría");
            category = new PriorityCategory(c.getName(), c.getMinRpn(), c.getMaxRpn(), c.getJustification());
        }
        RpnResult result = new RpnResult(f.getSeverity(), f.getOccurrence(), f.getDetection(), f.getRpn(),
                s.getDescriptor(), o.getDescriptor(), d.getDescriptor(), category,
                RpnCalculator.rule(f.getSeverity(), f.getOccurrence(), f.getDetection()));
        return new FmeaView(analysis, f, result);
    }

    private <E> E require(Class<E> type, Long id, String label) {
        return repo.find(type, id).orElseThrow(
                () -> new NoSuchElementException(label + " no encontrado: " + id));
    }
}