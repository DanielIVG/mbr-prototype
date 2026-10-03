// RpnFactor.java
package org.example.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rpn_factor")
public class RpnFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fmea_analysis_id", nullable = false)
    private Long fmeaAnalysisId;

    @Column(nullable = false)
    private int severity;

    @Column(nullable = false)
    private int occurrence;

    @Column(nullable = false)
    private int detection;

    @Column(nullable = false)
    private int rpn;

    @Column(name = "severity_level_id", nullable = false)
    private Long severityLevelId;

    @Column(name = "occurrence_level_id", nullable = false)
    private Long occurrenceLevelId;

    @Column(name = "detection_level_id", nullable = false)
    private Long detectionLevelId;

    @Column(name = "priority_category_id")
    private Long priorityCategoryId;

    /** Fila vigente del análisis (columna is_current). Las demás son historial (RF-29). */
    @Column(name = "is_current", nullable = false)
    private boolean latest;

    public Long getId() { return id; }
    public Long getFmeaAnalysisId() { return fmeaAnalysisId; }
    public void setFmeaAnalysisId(Long v) { this.fmeaAnalysisId = v; }
    public int getSeverity() { return severity; }
    public void setSeverity(int severity) { this.severity = severity; }
    public int getOccurrence() { return occurrence; }
    public void setOccurrence(int occurrence) { this.occurrence = occurrence; }
    public int getDetection() { return detection; }
    public void setDetection(int detection) { this.detection = detection; }
    public int getRpn() { return rpn; }
    public void setRpn(int rpn) { this.rpn = rpn; }
    public Long getSeverityLevelId() { return severityLevelId; }
    public void setSeverityLevelId(Long v) { this.severityLevelId = v; }
    public Long getOccurrenceLevelId() { return occurrenceLevelId; }
    public void setOccurrenceLevelId(Long v) { this.occurrenceLevelId = v; }
    public Long getDetectionLevelId() { return detectionLevelId; }
    public void setDetectionLevelId(Long v) { this.detectionLevelId = v; }
    public Long getPriorityCategoryId() { return priorityCategoryId; }
    public void setPriorityCategoryId(Long v) { this.priorityCategoryId = v; }
    public boolean isLatest() { return latest; }
    public void setLatest(boolean latest) { this.latest = latest; }
}