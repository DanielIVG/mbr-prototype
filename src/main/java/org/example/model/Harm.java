// Harm.java
package org.example.model;

import jakarta.persistence.*;

@Entity
@Table(name = "harm")
public class Harm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hazardous_situation_id", nullable = false)
    private Long hazardousSituationId;

    @Column(nullable = false)
    private String description;

    private String type;

    public Long getId() { return id; }
    public Long getHazardousSituationId() { return hazardousSituationId; }
    public void setHazardousSituationId(Long v) { this.hazardousSituationId = v; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}