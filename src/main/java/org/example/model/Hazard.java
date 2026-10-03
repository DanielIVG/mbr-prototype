// Hazard.java
package org.example.model;

import jakarta.persistence.*;

@Entity
@Table(name = "hazard")
public class Hazard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_element_id")
    private Long modelElementId;

    @Column(nullable = false)
    private String description;

    private String source;

    @Column(name = "review_status")
    private String reviewStatus;

    public Long getId() { return id; }
    public Long getModelElementId() { return modelElementId; }
    public void setModelElementId(Long modelElementId) { this.modelElementId = modelElementId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
}