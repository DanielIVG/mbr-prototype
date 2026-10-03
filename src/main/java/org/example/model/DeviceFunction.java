// DeviceFunction.java
package org.example.model;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "function")
public class DeviceFunction extends ModelElement {

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private String name;

    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "function_component", joinColumns = @JoinColumn(name = "function_id"))
    @Column(name = "component_id")
    private Set<Long> componentIds = new HashSet<>();

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Set<Long> getComponentIds() { return componentIds; }
    public void setComponentIds(Set<Long> componentIds) { this.componentIds = componentIds; }
}