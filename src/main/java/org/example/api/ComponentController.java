// ComponentController.java
package org.example.api;

import io.javalin.http.Context;
import org.example.service.ComponentService;

import java.util.NoSuchElementException;

public class ComponentController {

    private final ComponentService service;

    public ComponentController(ComponentService service) {
        this.service = service;
    }

    public void list(Context ctx) {
        Long deviceId = Params.requiredLong(ctx, "deviceId");
        ctx.json(service.listActive(deviceId).stream().map(ComponentDto::from).toList());
    }

    public void get(Context ctx) {
        Long id = Params.id(ctx);
        var c = service.find(id).orElseThrow(
                () -> new NoSuchElementException("Componente no encontrado: " + id));
        ctx.json(ComponentDto.from(c));
    }

    public void create(Context ctx) {
        var in = ctx.bodyAsClass(ComponentDto.class);
        var c = service.create(in.deviceId(), in.parentComponentId(), in.name(),
                in.description(), in.essential(), in.essentialJustification());
        ctx.status(201).json(ComponentDto.from(c));
    }

    public void update(Context ctx) {
        var in = ctx.bodyAsClass(ComponentDto.class);
        var c = service.update(Params.id(ctx), in.parentComponentId(), in.name(),
                in.description(), in.essential(), in.essentialJustification());
        ctx.json(ComponentDto.from(c));
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