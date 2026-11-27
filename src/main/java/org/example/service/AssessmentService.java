package org.example.service;

import org.example.iso.RiskAssessor;
import org.example.iso.RiskAssessor.Mode;
import org.example.iso.RiskMatrix;
import org.example.iso.Sensitivity;
import org.example.iso.Amount;
import org.example.repository.AssessmentRepository;
import org.example.repository.AssessmentRepository.CriteriaData;
import org.example.repository.AssessmentRepository.EstimationRow;
import org.example.repository.AssessmentRepository.QuantityRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/** Criterios de aceptabilidad, estimación P1×P2 con procedencia, veredicto, sensibilidad y control. */
public class AssessmentService {

    private final AssessmentRepository repo;
    private final FtaService fta;

    public AssessmentService(AssessmentRepository repo, FtaService fta) {
        this.repo = repo;
        this.fta = fta;
    }

    // ------------------------------------------------------------------ criterios

    public Optional<CriteriaData> criteria() {
        return repo.loadCriteria();
    }

    public boolean criteriaLocked() {
        return repo.estimationCount() > 0;
    }

    /** Valida la matriz completa y la guarda; no se permite cambiarla si ya hay estimaciones. */
    public void saveCriteria(CriteriaData data) {
        if (repo.estimationCount() > 0) {
            throw new ConflictException("Ya existen estimaciones de riesgo: los criterios de aceptabilidad "
                    + "no se pueden cambiar sin invalidar las evaluaciones hechas con ellos");
        }
        toMatrix(data);                               // lanza IllegalArgumentException si es inválida
        for (var c : data.criteria()) {
            String j = data.justifications().get(c.riskLevel());
            if (j == null || j.isBlank()) {
                throw new IllegalArgumentException("El criterio del nivel '" + c.riskLevel()
                        + "' necesita una justificación (ISO 14971, plan de gestión de riesgos)");
            }
        }
        repo.replaceCriteria(data);
    }

    private static RiskMatrix toMatrix(CriteriaData d) {
        return new RiskMatrix(d.exposureUnit(), d.severities(), d.probabilities(), d.cells(), d.criteria());
    }

    private RiskMatrix matrix() {
        return toMatrix(repo.loadCriteria().orElseThrow(() -> new ConflictException(
                "Primero defina los criterios de aceptabilidad (escalas, matriz y criterios)")));
    }

    public List<AssessmentRepository.EvidenceLevel> evidenceLevels() {
        return repo.evidenceLevels();
    }

    // ------------------------------------------------------------------ estimación

    public EstimationRow estimate(long harmId, String modeName, int severity, List<QuantityRow> quantities,
                                  String justification, String reason) {
        if (!repo.harmExists(harmId)) {
            throw new NoSuchElementException("Daño no encontrado: " + harmId);
        }
        Long ftaId = null;
        if ("FTA".equals(modeName)) {
            // P1 = probabilidad del evento superior del árbol de fallas vigente; se guarda como modo P1_P2.
            var tree = fta.current(harmId).orElseThrow(() -> new ConflictException(
                    "Primero construya el árbol de fallas de este daño (sección Análisis)"));
            quantities = withTreeP1(quantities, tree);
            ftaId = tree.saved().analysisId();
            modeName = "P1_P2";
        }
        Mode mode = parseMode(modeName);
        for (QuantityRow q : quantities) {
            requireProvenance(q);
        }
        RiskMatrix matrix = matrix();
        RiskAssessor.Input input = toInput(mode, severity, quantities, justification);
        RiskAssessor.Assessment a = RiskAssessor.assess(matrix, input);
        if (repo.latestEstimation(harmId).isPresent() && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException(
                    "Indique el motivo de la reevaluación (p. ej. medida de control implementada)");
        }
        long id = repo.saveEstimation(harmId, severity, mode.name(), a.estimate().p1(), a.estimate().p2(),
                a.estimate().pTotal(), a.estimate().exposureUnit(), a.verdict().probability().value(),
                a.verdict().riskLevel(), a.estimate().justification(), reason, normalized(quantities), ftaId);
        return repo.latestEstimation(harmId).filter(e -> e.id() == id).orElseThrow();
    }

    /** Sustituye/agrega P1 con el resultado del árbol: unidad del árbol, rango por monotonía y la evidencia más débil. */
    private List<QuantityRow> withTreeP1(List<QuantityRow> given, FtaService.Result tree) {
        List<QuantityRow> p2 = given.stream().filter(q -> q.role().equals(RiskAssessor.P2)).toList();
        if (p2.size() != 1 || given.size() != 1) {
            throw new IllegalArgumentException("Con el árbol de fallas solo se declara P2; P1 sale del árbol");
        }
        var levels = repo.evidenceLevels();
        String weakest = tree.saved().basics().stream().map(b -> b.quantity().evidence())
                .min(java.util.Comparator.comparingInt(code -> levels.stream()
                        .filter(l -> l.code().equals(code)).mapToInt(l -> l.rank()).findFirst().orElse(0)))
                .orElseThrow();
        var e = tree.evaluation();
        List<QuantityRow> out = new ArrayList<>();
        out.add(new QuantityRow(RiskAssessor.P1, 1, e.probability(), e.unit(), e.min(), e.max(),
                "Árbol de fallas del análisis " + tree.saved().analysisId() + " (calculado)", weakest,
                p2.get(0).author()));
        out.add(p2.get(0));
        return out;
    }

    public Optional<EstimationRow> latest(long harmId) {
        return repo.latestEstimation(harmId);
    }

    public List<EstimationRow> history(long harmId) {
        return repo.estimationHistory(harmId);
    }

    public Sensitivity.Report sensitivity(long harmId) {
        EstimationRow e = repo.latestEstimation(harmId).orElseThrow(
                () -> new NoSuchElementException("El daño todavía no tiene estimación"));
        return RiskAssessor.sensitivity(matrix(),
                toInput(parseMode(e.mode()), e.severity(), e.inputs(), e.justification()));
    }

    private static Mode parseMode(String name) {
        try {
            return Mode.valueOf(name);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Modo de estimación desconocido: " + name);
        }
    }

    private static void requireProvenance(QuantityRow q) {
        if (q.source() == null || q.source().isBlank()) {
            throw new IllegalArgumentException("Cada valor necesita su fuente (" + q.role() + ")");
        }
        if (q.evidence() == null || q.evidence().isBlank()) {
            throw new IllegalArgumentException("Cada valor necesita su nivel de evidencia (" + q.role() + ")");
        }
        if (q.author() == null || q.author().isBlank()) {
            throw new IllegalArgumentException("Cada valor necesita su autor (" + q.role() + ")");
        }
        if (q.min() != null && q.max() != null && q.min() > q.max()) {
            throw new IllegalArgumentException("El rango de " + q.role() + " es inválido: mínimo mayor que máximo");
        }
    }

    private static RiskAssessor.Input toInput(Mode mode, int severity, List<QuantityRow> rows, String justification) {
        List<RiskAssessor.Q> qs = new ArrayList<>();
        for (QuantityRow r : rows) {
            qs.add(new RiskAssessor.Q(r.role(), new Amount(r.value(), r.unit()), r.min(), r.max()));
        }
        return new RiskAssessor.Input(mode, severity, qs, justification);
    }

    /** Numera las barreras (ordinal 1, 2, …) y deja los demás roles con ordinal 1. */
    private static List<QuantityRow> normalized(List<QuantityRow> rows) {
        Map<String, Integer> count = new HashMap<>();
        List<QuantityRow> out = new ArrayList<>();
        for (QuantityRow r : rows) {
            int n = count.merge(r.role(), 1, Integer::sum);
            out.add(new QuantityRow(r.role(), n, r.value(), r.unit(), r.min(), r.max(),
                    r.source().trim(), r.evidence(), r.author().trim()));
        }
        return out;
    }

    // ------------------------------------------------------------------ control y verificación

    public long addControl(long harmId, String description, String type) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("La descripción de la medida de control es obligatoria");
        }
        if (!repo.harmExists(harmId)) {
            throw new NoSuchElementException("Daño no encontrado: " + harmId);
        }
        return repo.addControl(harmId, description.trim(), type);
    }

    public List<AssessmentRepository.ControlRow> controls(long harmId) {
        return repo.controls(harmId);
    }

    public long addVerification(long controlId, String method, boolean implemented, Boolean effective,
                                String evidence) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("El método de verificación es obligatorio");
        }
        if (!repo.controlExists(controlId)) {
            throw new NoSuchElementException("Medida de control no encontrada: " + controlId);
        }
        if (effective != null && !implemented) {
            throw new IllegalArgumentException("Una medida no implementada no puede declararse eficaz o ineficaz");
        }
        return repo.addVerification(controlId, method.trim(), implemented, effective, evidence);
    }

    public List<AssessmentRepository.VerificationRow> verifications(long controlId) {
        return repo.verifications(controlId);
    }
}
