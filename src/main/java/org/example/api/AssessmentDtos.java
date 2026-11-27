package org.example.api;

import java.util.List;
import java.util.Map;

/** Cuerpos de la API de criterios, estimación, sensibilidad y control. */
public final class AssessmentDtos {

    private AssessmentDtos() {}

    public record SeverityDto(int value, String descriptor) {}

    public record ProbabilityDto(int value, String descriptor, double min, double max) {}

    public record CellDto(int severity, int probability, String riskLevel) {}

    public record CriterionDto(String riskLevel, boolean acceptable, String justification) {}

    /** {@code locked}: ya existen estimaciones, los criterios no se pueden cambiar. */
    public record CriteriaDto(String exposureUnit, List<SeverityDto> severities,
                              List<ProbabilityDto> probabilities, List<CellDto> cells,
                              List<CriterionDto> criteria, boolean locked) {}

    /** Cantidad con procedencia. {@code role}: P1, P2, RATE, FRACTION, BARRIER, TOTAL. */
    public record QuantityDto(String role, double value, String unit, Double min, Double max,
                              String source, String evidence, String author) {}

    /** {@code mode}: P1_P2, FAILURE_MODE, TOTAL_ONLY o FTA (P1 sale del árbol de fallas vigente). */
    public record EstimationRequest(String mode, int severity, List<QuantityDto> quantities,
                                    String justification, String reason) {}

    public record EstimationDto(Long id, String mode, int severity, String severityDescriptor,
                                Double p1, Double p2, double pTotal, String unit,
                                int probabilityLevel, String probabilityDescriptor, String riskLevel,
                                boolean acceptable, String justification, String date,
                                List<QuantityDto> quantities, Long ftaAnalysisId) {}

    public record ChangeDto(double value, String riskLevel) {}

    public record ParamDto(String name, boolean decides, List<ChangeDto> changes) {}

    public record CornerDto(Map<String, Double> values, String riskLevel) {}

    public record SensitivityDto(String baselineLevel, List<ParamDto> params, List<CornerDto> corners,
                                 boolean cornersExplored, boolean changesOnlyJointly) {}

    public record EvidenceLevelDto(String code, String descriptor, int rank) {}

    public record ControlDto(Long id, Long harmId, String description, String type) {}

    public record VerificationDto(Long id, Long controlId, String method, boolean implemented,
                                  Boolean effective, String date, String evidence) {}
}
