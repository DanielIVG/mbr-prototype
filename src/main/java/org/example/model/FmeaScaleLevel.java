// FmeaScaleLevel.java
package org.example.model;

import jakarta.persistence.*;
import org.example.rpn.FactorType;

@Entity
@Table(name = "fmea_scale_level")
public class FmeaScaleLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "factor_type", nullable = false)
    private FactorType factorType;

    @Column(name = "value", nullable = false)
    private int levelValue;

    @Column(nullable = false)
    private String descriptor;

    public Long getId() { return id; }
    public FactorType getFactorType() { return factorType; }
    public void setFactorType(FactorType factorType) { this.factorType = factorType; }
    public int getLevelValue() { return levelValue; }
    public void setLevelValue(int levelValue) { this.levelValue = levelValue; }
    public String getDescriptor() { return descriptor; }
    public void setDescriptor(String descriptor) { this.descriptor = descriptor; }
}