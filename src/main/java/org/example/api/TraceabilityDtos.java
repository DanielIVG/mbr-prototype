package org.example.api;

import org.example.repository.TraceabilityRepository.ControlRef;
import org.example.repository.TraceabilityRepository.ElementRef;
import org.example.repository.TraceabilityRepository.HarmRef;
import org.example.repository.TraceabilityRepository.RequirementRow;
import org.example.repository.TraceabilityRepository.TraceRow;
import org.example.service.TraceabilityService;

import java.util.List;

public final class TraceabilityDtos {

    private TraceabilityDtos() {}

    public record RequirementRequest(Long deviceId, String code, String description, String type,
                                     String source) {}

    public record RequirementDto(Long id, Long deviceId, String code, String description, String type,
                                 String source) {
        public static RequirementDto from(RequirementRow r) {
            return new RequirementDto(r.id(), r.deviceId(), r.code(), r.description(), r.type(), r.source());
        }
    }

    public record LinkRequest(Long controlId) {}

    public record TraceRowDto(Long requirementId, String requirementCode, String requirementDescription,
                              Long controlId, String controlDescription, Long harmId, String harmDescription,
                              Long hazardId, String hazardDescription, Long elementId, String elementName,
                              int verifications, int effectiveVerifications) {
        public static TraceRowDto from(TraceRow r) {
            return new TraceRowDto(r.requirementId(), r.requirementCode(), r.requirementDescription(),
                    r.controlId(), r.controlDescription(), r.harmId(), r.harmDescription(), r.hazardId(),
                    r.hazardDescription(), r.elementId(), r.elementName(), r.verifications(),
                    r.effectiveVerifications());
        }
    }

    public record ControlDto(Long controlId, String description, String type, Long harmId,
                             String harmDescription, int verifications, int effectiveVerifications,
                             int requirements) {
        public static ControlDto from(ControlRef c) {
            return new ControlDto(c.controlId(), c.description(), c.type(), c.harmId(), c.harmDescription(),
                    c.verifications(), c.effectiveVerifications(), c.requirements());
        }
    }

    public record SummaryDto(int requirements, int requirementsWithControl, int controls,
                             int controlsWithRequirement, int controlsVerified) {}

    public record MatrixDto(List<TraceRowDto> rows, List<RequirementDto> requirementsWithoutControl,
                            List<ControlDto> controlsWithoutRequirement, SummaryDto summary) {
        public static MatrixDto from(TraceabilityService.Matrix m) {
            var s = m.summary();
            return new MatrixDto(m.rows().stream().map(TraceRowDto::from).toList(),
                    m.requirementsWithoutControl().stream().map(RequirementDto::from).toList(),
                    m.controlsWithoutRequirement().stream().map(ControlDto::from).toList(),
                    new SummaryDto(s.requirements(), s.requirementsWithControl(), s.controls(),
                            s.controlsWithRequirement(), s.controlsVerified()));
        }
    }

    public record HarmRefDto(Long harmId, String harmDescription, Long hazardId, String hazardDescription) {
        public static HarmRefDto from(HarmRef h) {
            return new HarmRefDto(h.harmId(), h.harmDescription(), h.hazardId(), h.hazardDescription());
        }
    }

    public record ElementRefDto(Long id, String name) {
        public static ElementRefDto from(ElementRef e) {
            return new ElementRefDto(e.id(), e.name());
        }
    }

    // ------------------------------------------------------------------ indicios del origen

    /** Un modo de falla vinculado a un paso del camino. */
    public record FailureModeRefDto(Long id, String description, Integer occurrence) {}

    /** Un paso del camino del árbol de comportamiento que llevó al daño. */
    public record OriginStepDto(Long nodeId, String kind, String description, String branch,
                                Long elementId, String elementName, List<FailureModeRefDto> failureModes) {}

    /** Indicios del origen de un daño: cadena de riesgo + camino del árbol con modos de falla. */
    public record OriginTraceDto(Long harmId, String harmDescription, String harmType,
                                 String situationDescription, Long hazardId, String hazardDescription,
                                 List<OriginStepDto> path) {}
}
