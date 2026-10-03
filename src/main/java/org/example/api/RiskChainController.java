// RiskChainController.java
package org.example.api;

import io.javalin.http.Context;
import org.example.service.RiskChainService;

import java.util.NoSuchElementException;

public class RiskChainController {

    private final RiskChainService service;

    public RiskChainController(RiskChainService service) {
        this.service = service;
    }

    public void createChain(Context ctx) {
        var in = ctx.bodyAsClass(CreateChainRequest.class);
        var chain = service.createChain(in.modelElementId(), in.hazardDescription(), in.source(),
                in.sequenceDescription(), in.situationDescription(),
                in.harmDescription(), in.harmType());
        ctx.status(201).json(RiskChainDto.from(chain));
    }

    public void listChains(Context ctx) {
        ctx.json(service.listChains().stream().map(RiskChainDto::from).toList());
    }

    public void getChain(Context ctx) {
        Long harmId = Params.id(ctx);
        var chain = service.findChain(harmId).orElseThrow(
                () -> new NoSuchElementException("Cadena de riesgo no encontrada para el daño: " + harmId));
        ctx.json(RiskChainDto.from(chain));
    }

    public void addSequence(Context ctx) {
        var in = ctx.bodyAsClass(RiskChainDto.SequenceDto.class);
        var created = service.addSequence(Params.id(ctx), in.description());
        ctx.status(201).json(RiskChainDto.SequenceDto.from(created));
    }

    public void addSituation(Context ctx) {
        var in = ctx.bodyAsClass(RiskChainDto.SituationDto.class);
        var created = service.addSituation(Params.id(ctx), in.description());
        ctx.status(201).json(RiskChainDto.SituationDto.from(created));
    }

    public void addHarm(Context ctx) {
        var in = ctx.bodyAsClass(RiskChainDto.HarmDto.class);
        var created = service.addHarm(Params.id(ctx), in.description(), in.type());
        ctx.status(201).json(RiskChainDto.HarmDto.from(created));
    }

    public void updateHazard(Context ctx) {
        var in = ctx.bodyAsClass(RiskChainDto.HazardDto.class);
        var updated = service.updateHazard(Params.id(ctx), in.modelElementId(),
                in.description(), in.source());
        ctx.json(RiskChainDto.HazardDto.from(updated));
    }

    public void updateSequence(Context ctx) {
        var in = ctx.bodyAsClass(RiskChainDto.SequenceDto.class);
        var updated = service.updateSequence(Params.id(ctx), in.description());
        ctx.json(RiskChainDto.SequenceDto.from(updated));
    }

    public void updateSituation(Context ctx) {
        var in = ctx.bodyAsClass(RiskChainDto.SituationDto.class);
        var updated = service.updateSituation(Params.id(ctx), in.description());
        ctx.json(RiskChainDto.SituationDto.from(updated));
    }

    public void updateHarm(Context ctx) {
        var in = ctx.bodyAsClass(RiskChainDto.HarmDto.class);
        var updated = service.updateHarm(Params.id(ctx), in.description(), in.type());
        ctx.json(RiskChainDto.HarmDto.from(updated));
    }
}