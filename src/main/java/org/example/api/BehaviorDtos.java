package org.example.api;

import java.util.List;

/** Cuerpos de la API del árbol de comportamiento. */
public final class BehaviorDtos {

    private BehaviorDtos() {}

    /** {@code kind}: ACTION, CONDITION, OUTCOME_SAFE, OUTCOME_HAZARD. {@code branch}: YES/NO bajo una condición. */
    public record NodeDto(Long id, Long elementId, Long parentId, Long rootId, String kind, String description,
                          String branch, String situation, String harm, String harmType, Long harmId,
                          boolean locked, List<FailureModeRef> failureModes) {}

    public record CreateNodeRequest(Long elementId, Long parentId, String kind, String description, String branch,
                                    String situation, String harm, String harmType) {}

    public record UpdateNodeRequest(String description) {}

    /** Referencia a un modo de falla vinculado a un nodo. */
    public record FailureModeRef(Long id, String description, Long modelElementId, Long behaviorNodeId, Integer occurrence) {}

    /** Solicitud para vincular un modo de falla a un nodo del árbol. */
    public record LinkFailureModeRequest(Long failureModeId, Long behaviorNodeId) {}
}
