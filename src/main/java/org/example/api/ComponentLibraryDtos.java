package org.example.api;

/** Cuerpos de la API de la biblioteca de componentes. */
public final class ComponentLibraryDtos {

    private ComponentLibraryDtos() {}

    public record EntryDto(String code, String name, String definition, String parentCode) {}

    /** Resultado de búsqueda: {@code score} entre 0 y 1 (1 = coincidencia exacta). */
    public record MatchDto(EntryDto entry, double score) {}
}
