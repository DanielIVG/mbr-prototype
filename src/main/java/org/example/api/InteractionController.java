// InteractionController.java
package org.example.api;

import io.javalin.http.Context;
import org.example.service.InteractionService;

import java.util.NoSuchElementException;

public class InteractionController {

    private final InteractionService service;

    public InteractionController(InteractionService service) {
        this.service = service;
    }

    public void list(Context ctx) {
        Long deviceId = Params.requiredLong(ctx, "deviceId");
        ctx.json(service.listActive(deviceId).stream().map(InteractionDto::from).toList());
    }

    public void get(Context ctx) {
        Long id = Params.id(ctx);
        var i = service.find(id).orElseThrow(
                () -> new NoSuchElementException("Interacción no encontrada: " + id));
        ctx.json(InteractionDto.from(i));
    }

    public void create(Context ctx) {
        var in = ctx.bodyAsClass(InteractionDto.class);
        var i = service.create(in.deviceId(), in.description(), in.relevanceJustification(),
                in.componentIds());
        ctx.status(201).json(InteractionDto.from(i));
    }

    public void update(Context ctx) {
        var in = ctx.bodyAsClass(InteractionDto.class);
        var i = service.update(Params.id(ctx), in.description(), in.relevanceJustification(),
                in.componentIds());
        ctx.json(InteractionDto.from(i));
    }

    public void deactivate(Context ctx) {
        service.deactivate(Params.id(ctx));
        ctx.status(204);
    }

    public void delete(Context ctx) {
        service.delete(Params.id(ctx));
        ctx.status(204);
    }
}