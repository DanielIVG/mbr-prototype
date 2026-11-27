package org.example.api;

import io.javalin.http.Context;
import org.example.service.TraceabilityService;

public class TraceabilityController {

    private final TraceabilityService service;

    public TraceabilityController(TraceabilityService service) {
        this.service = service;
    }

    public void listRequirements(Context ctx) {
        Long deviceId = Params.requiredLong(ctx, "deviceId");
        ctx.json(service.listRequirements(deviceId).stream().map(TraceabilityDtos.RequirementDto::from).toList());
    }

    public void createRequirement(Context ctx) {
        var in = ctx.bodyAsClass(TraceabilityDtos.RequirementRequest.class);
        var created = service.createRequirement(in.deviceId(), in.code(), in.description(), in.type(),
                in.source());
        ctx.status(201).json(TraceabilityDtos.RequirementDto.from(created));
    }

    public void updateRequirement(Context ctx) {
        var in = ctx.bodyAsClass(TraceabilityDtos.RequirementRequest.class);
        var updated = service.updateRequirement(Params.id(ctx), in.code(), in.description(), in.type(),
                in.source());
        ctx.json(TraceabilityDtos.RequirementDto.from(updated));
    }

    public void deactivateRequirement(Context ctx) {
        service.deactivateRequirement(Params.id(ctx));
        ctx.status(204);
    }

    public void link(Context ctx) {
        var in = ctx.bodyAsClass(TraceabilityDtos.LinkRequest.class);
        service.link(Params.id(ctx), in.controlId());
        ctx.status(201);
    }

    public void unlink(Context ctx) {
        service.unlink(Params.id(ctx), Long.parseLong(ctx.pathParam("controlId")));
        ctx.status(204);
    }

    public void controls(Context ctx) {
        ctx.json(service.controls(Params.id(ctx)).stream().map(TraceabilityDtos.ControlDto::from).toList());
    }

    public void matrix(Context ctx) {
        ctx.json(TraceabilityDtos.MatrixDto.from(service.matrix(Params.id(ctx))));
    }

    public void componentsOfHarm(Context ctx) {
        ctx.json(service.componentsOfHarm(Params.id(ctx)).stream().map(TraceabilityDtos.ElementRefDto::from).toList());
    }

    public void harmsOfComponent(Context ctx) {
        ctx.json(service.harmsOfComponent(Params.id(ctx)).stream().map(TraceabilityDtos.HarmRefDto::from).toList());
    }

    /** Indicios del origen de un daño: cadena de riesgo + camino del árbol con modos de falla. */
    public void originTrace(Context ctx) {
        var traceOpt = service.originTrace(Params.id(ctx));
        if (traceOpt.isEmpty()) {
            ctx.status(404).json(java.util.Map.of("error", "El daño no fue generado por un árbol de comportamiento"));
            return;
        }
        var trace = traceOpt.get();
        var steps = trace.path().stream().map(s -> new TraceabilityDtos.OriginStepDto(
                s.nodeId(), s.kind(), s.description(), s.branch(), s.elementId(), s.elementName(),
                s.failureModes().stream().map(fm ->
                        new TraceabilityDtos.FailureModeRefDto(fm.id(), fm.description(), fm.occurrence())).toList()
        )).toList();
        ctx.json(new TraceabilityDtos.OriginTraceDto(trace.harmId(), trace.harmDescription(), trace.harmType(),
                trace.situationDescription(), trace.hazardId(), trace.hazardDescription(), steps));
    }
}
