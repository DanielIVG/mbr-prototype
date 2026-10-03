// RiskChainDto.java
package org.example.api;

import org.example.model.Harm;
import org.example.model.Hazard;
import org.example.model.HazardousSituation;
import org.example.model.RiskChain;
import org.example.model.SequenceOfEvents;

public record RiskChainDto(HazardDto hazard, SequenceDto sequence,
                           SituationDto situation, HarmDto harm) {

    public record HazardDto(Long id, Long modelElementId, String description,
                            String source, String reviewStatus) {
        public static HazardDto from(Hazard h) {
            return new HazardDto(h.getId(), h.getModelElementId(), h.getDescription(),
                    h.getSource(), h.getReviewStatus());
        }
    }

    public record SequenceDto(Long id, Long hazardId, String description, int stepOrder) {
        public static SequenceDto from(SequenceOfEvents s) {
            return new SequenceDto(s.getId(), s.getHazardId(), s.getDescription(), s.getStepOrder());
        }
    }

    public record SituationDto(Long id, Long sequenceOfEventsId, String description) {
        public static SituationDto from(HazardousSituation s) {
            return new SituationDto(s.getId(), s.getSequenceOfEventsId(), s.getDescription());
        }
    }

    public record HarmDto(Long id, Long hazardousSituationId, String description, String type) {
        public static HarmDto from(Harm h) {
            return new HarmDto(h.getId(), h.getHazardousSituationId(), h.getDescription(), h.getType());
        }
    }

    public static RiskChainDto from(RiskChain c) {
        return new RiskChainDto(HazardDto.from(c.hazard()), SequenceDto.from(c.sequence()),
                SituationDto.from(c.situation()), HarmDto.from(c.harm()));
    }
}