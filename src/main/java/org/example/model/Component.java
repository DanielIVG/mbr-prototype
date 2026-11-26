// Component.java
package org.example.model;

import jakarta.persistence.*;

@Entity
@Table(name = "component")
public class Component extends ModelElement {

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "parent_component_id")
    private Long parentComponentId;

    @Column(nullable = false)
    private String name;

    private String description;

    /** Término de la biblioteca de componentes (V6); null solo en componentes anteriores a V7. */
    @Column(name = "library_code")
    private String libraryCode;

    @Column(nullable = false)
    private boolean essential;

    @Column(name = "essential_justification")
    private String essentialJustification;

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public Long getParentComponentId() { return parentComponentId; }
    public void setParentComponentId(Long parentComponentId) { this.parentComponentId = parentComponentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLibraryCode() { return libraryCode; }
    public void setLibraryCode(String libraryCode) { this.libraryCode = libraryCode; }
    public boolean isEssential() { return essential; }
    public void setEssential(boolean essential) { this.essential = essential; }
    public String getEssentialJustification() { return essentialJustification; }
    public void setEssentialJustification(String v) { this.essentialJustification = v; }
}