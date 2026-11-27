package org.example.api;

import org.example.api.AssessmentDtos.QuantityDto;

import java.util.List;

/** Cuerpos de la API del árbol de fallas. */
public final class FtaDtos {

    private FtaDtos() {}

    /** Evento básico con su probabilidad y procedencia. El nombre es su clave. */
    public record BasicDto(String name, QuantityDto quantity, Long modelElementId) {}

    /**
     * Nodo del árbol. {@code kind}: GATE (usa label, gate, independence, children) o BASIC (usa ref,
     * el nombre de un evento básico; la misma ref en varias ramas es un solo evento).
     */
    public record NodeDto(String kind, String label, String gate, String independence,
                          List<NodeDto> children, String ref) {}

    public record CutSetDto(List<String> events, double probability) {}

    public record ResultDto(double probability, double min, double max, String unit, boolean exact,
                            List<CutSetDto> cutSets, List<String> singlePoints) {}

    public record FtaDto(Long analysisId, String topEvent, List<BasicDto> basics, NodeDto root, ResultDto result) {}
}
