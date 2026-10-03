// RpnResult.java
package org.example.rpn;

/**
 * Resultado del cálculo con todo lo necesario para mostrar de dónde sale (RNF-06):
 * entradas, descriptores de cada nivel (RF-21), fórmula aplicada y categoría con su rango.
 * La categoría es null solo si no hay categorías definidas.
 */
public record RpnResult(int severity, int occurrence, int detection, int rpn,
                        String severityDescriptor, String occurrenceDescriptor,
                        String detectionDescriptor, PriorityCategory category, String rule) {
}