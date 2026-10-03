// CreateChainRequest.java
package org.example.api;

/** Cuerpo del flujo guiado (RU-01): crea peligro, secuencia, situación y daño de una vez. */
public record CreateChainRequest(Long modelElementId, String hazardDescription, String source,
                                 String sequenceDescription, String situationDescription,
                                 String harmDescription, String harmType) {
}