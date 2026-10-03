// Device.java
package org.example.model;

import jakarta.persistence.*;

@Entity
@Table(name = "device")
public class Device extends ModelElement {

    @Column(name = "gmdn_code_id")
    private Integer gmdnCodeId;

    @Column(nullable = false)
    private String name;

    @Column(name = "intended_use", nullable = false)
    private String intendedUse;

    @Column(name = "reasonably_foreseeable_misuse")
    private String reasonablyForeseeableMisuse;

    public Integer getGmdnCodeId() { return gmdnCodeId; }
    public void setGmdnCodeId(Integer gmdnCodeId) { this.gmdnCodeId = gmdnCodeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIntendedUse() { return intendedUse; }
    public void setIntendedUse(String intendedUse) { this.intendedUse = intendedUse; }
    public String getReasonablyForeseeableMisuse() { return reasonablyForeseeableMisuse; }
    public void setReasonablyForeseeableMisuse(String v) { this.reasonablyForeseeableMisuse = v; }
}