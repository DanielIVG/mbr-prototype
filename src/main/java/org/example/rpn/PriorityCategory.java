// PriorityCategory.java
package org.example.rpn;

/** Categoría de prioridad con rango de RPN inclusivo y justificación (RF-25). */
public record PriorityCategory(String name, int minRpn, int maxRpn, String justification) {

    public PriorityCategory {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }
        if (justification == null || justification.isBlank()) {
            throw new IllegalArgumentException(
                    "La categoría '" + name + "' requiere justificación (RF-25)");
        }
        if (minRpn < 1 || minRpn > maxRpn) {
            throw new IllegalArgumentException(
                    "Rango inválido en la categoría '" + name + "': " + minRpn + "–" + maxRpn);
        }
    }

    public boolean contains(int rpn) {
        return rpn >= minRpn && rpn <= maxRpn;
    }
}