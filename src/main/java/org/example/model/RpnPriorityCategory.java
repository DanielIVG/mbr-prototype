// RpnPriorityCategory.java
package org.example.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rpn_priority_category")
public class RpnPriorityCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "min_rpn", nullable = false)
    private int minRpn;

    @Column(name = "max_rpn", nullable = false)
    private int maxRpn;

    @Column(nullable = false)
    private String justification;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getMinRpn() { return minRpn; }
    public void setMinRpn(int minRpn) { this.minRpn = minRpn; }
    public int getMaxRpn() { return maxRpn; }
    public void setMaxRpn(int maxRpn) { this.maxRpn = maxRpn; }
    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
}