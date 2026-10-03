// SequenceOfEvents.java
package org.example.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sequence_of_events")
public class SequenceOfEvents {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hazard_id", nullable = false)
    private Long hazardId;

    @Column(nullable = false)
    private String description;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    public Long getId() { return id; }
    public Long getHazardId() { return hazardId; }
    public void setHazardId(Long hazardId) { this.hazardId = hazardId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }
}