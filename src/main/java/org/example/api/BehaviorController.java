package org.example.api;

import io.javalin.http.Context;
import org.example.api.BehaviorDtos.CreateNodeRequest;
import org.example.api.BehaviorDtos.FailureModeRef;
import org.example.api.BehaviorDtos.LinkFailureModeRequest;
import org.example.api.BehaviorDtos.NodeDto;
import org.example.api.BehaviorDtos.UpdateNodeRequest;
import org.example.model.FailureMode;
import org.example.repository.BehaviorRepository.Node;
import org.example.repository.FmeaRepository;
import org.example.service.BehaviorService;
import org.example.service.FmeaService;

import java.util.List;

public class BehaviorController {

    private final BehaviorService service;
    private final FmeaService fmeaService;
    private final FmeaRepository fmeaRepo;

    public BehaviorController(BehaviorService service, FmeaService fmeaService, FmeaRepository fmeaRepo) {
        this.service = service;
        this.fmeaService = fmeaService;
        this.fmeaRepo = fmeaRepo;
    }

    public void list(Context ctx) {
        ctx.json(service.list(Params.id(ctx)).stream().map(v -> toDto(v.node(), v.locked())).toList());
    }

    public void listByDevice(Context ctx) {
        ctx.json(service.listByDevice(Params.id(ctx)).stream().map(v -> toDto(v.node(), v.locked())).toList());
    }

    public void create(Context ctx) {
        var in = ctx.bodyAsClass(CreateNodeRequest.class);
        Node n = service.create(in.elementId(), in.parentId(), in.kind(), in.description(), in.branch(),
                in.situation(), in.harm(), in.harmType());
        ctx.status(201).json(toDto(n, "OUTCOME_HAZARD".equals(n.kind())));
    }

    public void update(Context ctx) {
        var in = ctx.bodyAsClass(UpdateNodeRequest.class);
        ctx.json(toDto(service.updateDescription(Params.id(ctx), in.description()), false));
    }

    public void deactivate(Context ctx) {
        service.deactivate(Params.id(ctx));
        ctx.status(204);
    }

    /** Vincula un modo de falla a un nodo del árbol de comportamiento. */
    public void linkFailureMode(Context ctx) {
        var in = ctx.bodyAsClass(LinkFailureModeRequest.class);
        FailureMode fm = fmeaService.linkToBehaviorNode(in.failureModeId(), in.behaviorNodeId());
        ctx.json(toRef(fm, null));
    }

    /** Desvincula un modo de falla de su nodo del árbol. */
    public void unlinkFailureMode(Context ctx) {
        Long failureModeId = Params.id(ctx);
        FailureMode fm = fmeaService.unlinkFromBehaviorNode(failureModeId);
        ctx.json(toRef(fm, null));
    }

    private NodeDto toDto(Node n, boolean locked) {
        List<FailureModeRef> fmRefs = fmeaRepo.failureModesByBehaviorNode(n.id()).stream()
                .map(fm -> toRef(fm, currentOccurrence(fm)))
                .toList();
        return new NodeDto(n.id(), n.elementId(), n.parentId(), n.rootId(), n.kind(), n.description(), n.branch(),
                n.situation(), n.harm(), n.harmType(), n.harmId(), locked, fmRefs);
    }

    private static FailureModeRef toRef(FailureMode fm, Integer occurrence) {
        return new FailureModeRef(fm.getId(), fm.getDescription(), fm.getModelElementId(), fm.getBehaviorNodeId(), occurrence);
    }

    /** Obtiene el valor de ocurrencia actual del análisis FMEA asociado al modo de falla. */
    private Integer currentOccurrence(FailureMode fm) {
        // Buscar análisis FMEA que use este modo de falla y obtener su ocurrencia actual
        return fmeaRepo.currentOccurrenceByFailureMode(fm.getId()).orElse(null);
    }
}
