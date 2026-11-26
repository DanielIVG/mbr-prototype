package org.example.api;

import java.util.List;

/**
 * Cuerpo del flujo guiado (RU-01): crea peligro, secuencia, situación y daño de una vez.
 * Si viene {@code steps}, la secuencia tiene varios pasos (cada uno con su componente) y la
 * situación peligrosa se asocia al último; si no, se usa {@code sequenceDescription}.
 */
public record CreateChainRequest(Long modelElementId, String hazardDescription, String source,
                                 String sequenceDescription, String situationDescription,
                                 String harmDescription, String harmType, List<Step> steps) {

    public record Step(String description, Long modelElementId) {}

    /** Constructor de un solo paso (compatibilidad). */
    public CreateChainRequest(Long modelElementId, String hazardDescription, String source,
                              String sequenceDescription, String situationDescription,
                              String harmDescription, String harmType) {
        this(modelElementId, hazardDescription, source, sequenceDescription, situationDescription,
                harmDescription, harmType, null);
    }
}
