// FunctionController.java
package org.example.api;

import io.javalin.http.Context;
import org.example.service.DeviceFunctionService;

import java.util.NoSuchElementException;

public class FunctionController {

    private final DeviceFunctionService service;

    public FunctionController(DeviceFunctionService service) {
        this.service = service;
    }

    public void list(Context ctx) {
        Long deviceId = Params.requiredLong(ctx, "deviceId");
        ctx.json(service.listActive(deviceId).stream().map(FunctionDto::from).toList());
    }

    public void get(Context ctx) {
        Long id = Params.id(ctx);
        var f = service.find(id).orElseThrow(
                () -> new NoSuchElementException("Función no encontrada: " + id));
        ctx.json(FunctionDto.from(f));
    }

    public void create(Context ctx) {
        var in = ctx.bodyAsClass(FunctionDto.class);
        var f = service.create(in.deviceId(), in.name(), in.description(), in.componentIds());
        ctx.status(201).json(FunctionDto.from(f));
    }

    public void update(Context ctx) {
        var in = ctx.bodyAsClass(FunctionDto.class);
        var f = service.update(Params.id(ctx), in.name(), in.description(), in.componentIds());
        ctx.json(FunctionDto.from(f));
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