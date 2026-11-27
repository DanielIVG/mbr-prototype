// FailureMode.java
package org.example.model;

import jakarta.persistence.*;

@Entity
@Table(name = "failure_mode")
public class FailureMode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_element_id")
    private Long modelElementId;

    @Column(nullable = false)
    private String description;

    /** Nodo del árbol de comportamiento al que está vinculado (opcional). */
    @Column(name = "behavior_node_id")
    private Long behaviorNodeId;

    public Long getId() { return id; }
    public Long getModelElementId() { return modelElementId; }
    public void setModelElementId(Long modelElementId) { this.modelElementId = modelElementId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getBehaviorNodeId() { return behaviorNodeId; }
    public void setBehaviorNodeId(Long behaviorNodeId) { this.behaviorNodeId = behaviorNodeId; }
}