package org.example.api;

import io.javalin.http.Context;
import org.example.api.AssessmentDtos.*;
import org.example.iso.RiskMatrix;
import org.example.iso.Sensitivity;
import org.example.repository.AssessmentRepository.CriteriaData;
import org.example.repository.AssessmentRepository.EstimationRow;
import org.example.repository.AssessmentRepository.QuantityRow;
import org.example.service.AssessmentService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class AssessmentController {

    private final AssessmentService service;

    public AssessmentController(AssessmentService service) {
        this.service = service;
    }

    // ------------------------------------------------------------------ criterios

    public void getCriteria(Context ctx) {
        var data = service.criteria().orElseThrow(
                () -> new NoSuchElementException("Los criterios todavía no están definidos"));
        ctx.json(toDto(data, service.criteriaLocked()));
    }

    public void putCriteria(Context ctx) {
        var in = ctx.bodyAsClass(CriteriaDto.class);
        Map<String, String> just = new LinkedHashMap<>();
        in.criteria().forEach(c -> just.put(c.riskLevel(), c.justification()));
        service.saveCriteria(new CriteriaData(in.exposureUnit(),
                in.severities().stream().map(s -> new RiskMatrix.SeverityLevel(s.value(), s.descriptor())).toList(),
                in.probabilities().stream()
                        .map(p -> new RiskMatrix.ProbabilityLevel(p.value(), p.descriptor(), p.min(), p.max())).toList(),
                in.cells().stream().map(c -> new RiskMatrix.Cell(c.severity(), c.probability(), c.riskLevel())).toList(),
                in.criteria().stream().map(c -> new RiskMatrix.Criterion(c.riskLevel(), c.acceptable())).toList(),
                just));
        ctx.json(toDto(service.criteria().orElseThrow(), service.criteriaLocked()));
    }

    private static CriteriaDto toDto(CriteriaData d, boolean locked) {
        return new CriteriaDto(d.exposureUnit(),
                d.severities().stream().map(s -> new SeverityDto(s.value(), s.descriptor())).toList(),
                d.probabilities().stream()
                        .map(p -> new ProbabilityDto(p.value(), p.descriptor(), p.min(), p.max())).toList(),
                d.cells().stream().map(c -> new CellDto(c.severity(), c.probability(), c.riskLevel())).toList(),
                d.criteria().stream()
                        .map(c -> new CriterionDto(c.riskLevel(), c.acceptable(), d.justifications().get(c.riskLevel())))
                        .toList(),
                locked);
    }

    public void evidenceLevels(Context ctx) {
        ctx.json(service.evidenceLevels().stream()
                .map(e -> new EvidenceLevelDto(e.code(), e.descriptor(), e.rank())).toList());
    }

    // ------------------------------------------------------------------ estimación

    public void estimate(Context ctx) {
        var in = ctx.bodyAsClass(EstimationRequest.class);
        List<QuantityRow> rows = in.quantities().stream()
                .map(q -> new QuantityRow(q.role(), 1, q.value(), q.unit(), q.min(), q.max(),
                        q.source(), q.evidence(), q.author())).toList();
        var saved = service.estimate(Params.id(ctx), in.mode(), in.severity(), rows,
                in.justification(), in.reason());
        ctx.status(201).json(toDto(saved));
    }

    public void latest(Context ctx) {
        var e = service.latest(Params.id(ctx)).orElseThrow(
                () -> new NoSuchElementException("El daño todavía no tiene estimación"));
        ctx.json(toDto(e));
    }

    public void history(Context ctx) {
        ctx.json(service.history(Params.id(ctx)).stream().map(AssessmentController::toDto).toList());
    }

    private static EstimationDto toDto(EstimationRow e) {
        return new EstimationDto(e.id(), e.mode(), e.severity(), e.severityDescriptor(), e.p1(), e.p2(),
                e.pTotal(), e.unit(), e.probabilityLevel(), e.probabilityDescriptor(), e.riskLevel(),
                e.acceptable(), e.justification(), e.date(),
                e.inputs().stream().map(q -> new QuantityDto(q.role(), q.value(), q.unit(), q.min(), q.max(),
                        q.source(), q.evidence(), q.author())).toList(), e.ftaAnalysisId());
    }

    public void sensitivity(Context ctx) {
        Sensitivity.Report r = service.sensitivity(Params.id(ctx));
        ctx.json(new SensitivityDto(r.baseline().riskLevel(),
                r.results().stream().map(p -> new ParamDto(p.name(), p.decides(),
                        p.changes().stream().map(c -> new ChangeDto(c.value(), c.outcome().riskLevel())).toList()))
                        .toList(),
                r.cornerChanges().stream().map(c -> new CornerDto(c.values(), c.outcome().riskLevel())).toList(),
                r.cornersExplored(), r.changesOnlyJointly()));
    }

    // ------------------------------------------------------------------ control

    public void addControl(Context ctx) {
        var in = ctx.bodyAsClass(ControlDto.class);
        long harmId = Params.id(ctx);
        long id = service.addControl(harmId, in.description(), in.type());
        ctx.status(201).json(new ControlDto(id, harmId, in.description().trim(), in.type()));
    }

    public void controls(Context ctx) {
        ctx.json(service.controls(Params.id(ctx)).stream()
                .map(c -> new ControlDto(c.id(), c.harmId(), c.description(), c.type())).toList());
    }

    public void addVerification(Context ctx) {
        var in = ctx.bodyAsClass(VerificationDto.class);
        long controlId = Params.id(ctx);
        long id = service.addVerification(controlId, in.method(), in.implemented(), in.effective(), in.evidence());
        ctx.status(201).json(new VerificationDto(id, controlId, in.method().trim(), in.implemented(),
                in.effective(), null, in.evidence()));
    }

    public void verifications(Context ctx) {
        ctx.json(service.verifications(Params.id(ctx)).stream()
                .map(v -> new VerificationDto(v.id(), v.controlId(), v.method(), v.implemented(),
                        v.effective(), v.date(), v.evidence())).toList());
    }
}
