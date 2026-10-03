// Interaction.java
package org.example.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "interaction")
public class Interaction extends ModelElement {

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private String description;

    @Column(name = "relevance_justification", nullable = false)
    private String relevanceJustification;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "interaction_component", joinColumns = @JoinColumn(name = "interaction_id"))
    @Column(name = "component_id")
    private Set<Long> componentIds = new HashSet<>();

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRelevanceJustification() { return relevanceJustification; }
    public void setRelevanceJustification(String v) { this.relevanceJustification = v; }
    public Set<Long> getComponentIds() { return componentIds; }
    public void setComponentIds(Set<Long> componentIds) { this.componentIds = componentIds; }
}