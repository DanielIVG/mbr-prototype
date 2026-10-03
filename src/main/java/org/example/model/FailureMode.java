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

    public Long getId() { return id; }
    public Long getModelElementId() { return modelElementId; }
    public void setModelElementId(Long modelElementId) { this.modelElementId = modelElementId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}