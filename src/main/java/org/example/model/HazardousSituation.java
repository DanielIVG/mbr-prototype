// HazardousSituation.java
package org.example.model;

import jakarta.persistence.*;

@Entity
@Table(name = "hazardous_situation")
public class HazardousSituation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sequence_of_events_id", nullable = false)
    private Long sequenceOfEventsId;

    @Column(nullable = false)
    private String description;

    public Long getId() { return id; }
    public Long getSequenceOfEventsId() { return sequenceOfEventsId; }
    public void setSequenceOfEventsId(Long v) { this.sequenceOfEventsId = v; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}