// ScaleLevel.java
package org.example.rpn;

import java.util.Objects;

/** Un nivel de la escala definida para el caso de estudio (RF-21). */
public record ScaleLevel(FactorType factor, int value, String descriptor) {

    public ScaleLevel {
        Objects.requireNonNull(factor, "El tipo de factor es obligatorio");
        if (value < 1) {
            throw new IllegalArgumentException("El valor de un nivel debe ser un entero positivo");
        }
        if (descriptor == null || descriptor.isBlank()) {
            throw new IllegalArgumentException("El descriptor del nivel es obligatorio");
        }
    }
}