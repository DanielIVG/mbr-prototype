package org.example.api;

import io.javalin.http.Context;
import org.example.api.BehaviorDtos.CreateNodeRequest;
import org.example.api.BehaviorDtos.NodeDto;
import org.example.api.BehaviorDtos.UpdateNodeRequest;
import org.example.repository.BehaviorRepository.Node;
import org.example.service.BehaviorService;

public class BehaviorController {

    private final BehaviorService service;

    public BehaviorController(BehaviorService service) {
        this.service = service;
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

    private static NodeDto toDto(Node n, boolean locked) {
        return new NodeDto(n.id(), n.elementId(), n.parentId(), n.rootId(), n.kind(), n.description(), n.branch(),
                n.situation(), n.harm(), n.harmType(), n.harmId(), locked);
    }
}
