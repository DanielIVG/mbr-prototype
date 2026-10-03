// FmeaAnalysis.java
package org.example.model;

import jakarta.persistence.*;

@Entity
@Table(name = "fmea_analysis")
public class FmeaAnalysis extends Analysis {

    @Column(name = "failure_mode_id", nullable = false)
    private Long failureModeId;

    @Column(nullable = false)
    private String effect;

    private String cause;

    @Column(name = "detection_method")
    private String detectionMethod;

    @Column(name = "existing_controls")
    private String existingControls;

    public FmeaAnalysis() {
        super("FMEA");
    }

    public Long getFailureModeId() { return failureModeId; }
    public void setFailureModeId(Long failureModeId) { this.failureModeId = failureModeId; }
    public String getEffect() { return effect; }
    public void setEffect(String effect) { this.effect = effect; }
    public String getCause() { return cause; }
    public void setCause(String cause) { this.cause = cause; }
    public String getDetectionMethod() { return detectionMethod; }
    public void setDetectionMethod(String detectionMethod) { this.detectionMethod = detectionMethod; }
    public String getExistingControls() { return existingControls; }
    public void setExistingControls(String existingControls) { this.existingControls = existingControls; }
}