// Analysis.java
package org.example.model;

import jakarta.persistence.*;

/**
 * Raíz de la herencia JOINED de análisis. analysis_type se guarda como columna normal,
 * que fija cada subclase en su constructor (no se usa el discriminador de Hibernate).
 */
@Entity
@Table(name = "analysis")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "harm_id", nullable = false)
    private Long harmId;

    @Column(name = "analysis_type", nullable = false, updatable = false)
    private String analysisType;

    @Column(name = "created_date", nullable = false)
    private String createdDate;

    private String source;

    @Column(nullable = false)
    private boolean active = true;

    protected Analysis() {}

    protected Analysis(String analysisType) {
        this.analysisType = analysisType;
    }

    public Long getId() { return id; }
    public Long getHarmId() { return harmId; }
    public void setHarmId(Long harmId) { this.harmId = harmId; }
    public String getAnalysisType() { return analysisType; }
    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}