// FmeaDtos.java
package org.example.api;

import org.example.model.FailureMode;
import org.example.model.FmeaScaleLevel;
import org.example.model.FmeaView;
import org.example.model.RpnFactor;
import org.example.model.RpnPriorityCategory;
import org.example.rpn.FactorType;
import org.example.rpn.PriorityCategory;
import org.example.rpn.RpnResult;

public final class FmeaDtos {

    private FmeaDtos() {}

    // --- Configuración del caso de estudio ---

    public record ScaleLevelRequest(FactorType factorType, Integer value, String descriptor) {}

    public record ScaleLevelDto(Long id, FactorType factorType, int value, String descriptor) {
        public static ScaleLevelDto from(FmeaScaleLevel l) {
            return new ScaleLevelDto(l.getId(), l.getFactorType(), l.getLevelValue(), l.getDescriptor());
        }
    }

    public record CategoryRequest(String name, Integer minRpn, Integer maxRpn, String justification) {}

    public record CategoryDto(Long id, String name, int minRpn, int maxRpn, String justification) {
        public static CategoryDto from(RpnPriorityCategory c) {
            return new CategoryDto(c.getId(), c.getName(), c.getMinRpn(), c.getMaxRpn(), c.getJustification());
        }

        public static CategoryDto from(PriorityCategory c) {
            return c == null ? null
                    : new CategoryDto(null, c.name(), c.minRpn(), c.maxRpn(), c.justification());
        }
    }

    // --- Modos de fallo ---

    public record FailureModeRequest(Long modelElementId, String description) {}

    public record FailureModeDto(Long id, Long modelElementId, String description) {
        public static FailureModeDto from(FailureMode m) {
            return new FailureModeDto(m.getId(), m.getModelElementId(), m.getDescription());
        }
    }

    // --- Análisis FMEA ---

    public record CreateFmeaRequest(Long harmId, Long failureModeId, String effect, String cause,
                                    String detectionMethod, String existingControls, String source,
                                    Integer severity, Integer occurrence, Integer detection) {}

    public record FactorsRequest(Integer severity, Integer occurrence, Integer detection) {}

    /** Resultado explicado (RU-02, RNF-06): entradas, descriptores, fórmula y categoría con su rango. */
    public record RpnDto(int severity, int occurrence, int detection, int rpn,
                         String severityDescriptor, String occurrenceDescriptor,
                         String detectionDescriptor, CategoryDto category, String rule) {
        public static RpnDto from(RpnResult r) {
            return new RpnDto(r.severity(), r.occurrence(), r.detection(), r.rpn(),
                    r.severityDescriptor(), r.occurrenceDescriptor(), r.detectionDescriptor(),
                    CategoryDto.from(r.category()), r.rule());
        }
    }

    public record FmeaViewDto(Long id, Long harmId, Long failureModeId, String effect, String cause,
                              String detectionMethod, String existingControls, String source,
                              String createdDate, RpnDto rpn) {
        public static FmeaViewDto from(FmeaView v) {
            var a = v.analysis();
            return new FmeaViewDto(a.getId(), a.getHarmId(), a.getFailureModeId(), a.getEffect(),
                    a.getCause(), a.getDetectionMethod(), a.getExistingControls(), a.getSource(),
                    a.getCreatedDate(), RpnDto.from(v.result()));
        }
    }

    public record RpnFactorDto(Long id, int severity, int occurrence, int detection, int rpn,
                               boolean current) {
        public static RpnFactorDto from(RpnFactor f) {
            return new RpnFactorDto(f.getId(), f.getSeverity(), f.getOccurrence(), f.getDetection(),
                    f.getRpn(), f.isLatest());
        }
    }
}