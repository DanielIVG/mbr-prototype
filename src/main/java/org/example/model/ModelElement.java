// ModelElement.java
package org.example.model;

import jakarta.persistence.*;

@Entity
@Table(name = "model_element")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class ModelElement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}