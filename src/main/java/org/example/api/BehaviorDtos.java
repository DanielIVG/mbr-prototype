package org.example.api;

/** Cuerpos de la API del árbol de comportamiento. */
public final class BehaviorDtos {

    private BehaviorDtos() {}

    /** {@code kind}: ACTION, CONDITION, OUTCOME_SAFE, OUTCOME_HAZARD. {@code branch}: YES/NO bajo una condición. */
    public record NodeDto(Long id, Long elementId, Long parentId, Long rootId, String kind, String description,
                          String branch, String situation, String harm, String harmType, Long harmId,
                          boolean locked) {}

    public record CreateNodeRequest(Long elementId, Long parentId, String kind, String description, String branch,
                                    String situation, String harm, String harmType) {}

    public record UpdateNodeRequest(String description) {}
}
