// FmeaController.java
package org.example.api;

import io.javalin.http.Context;
import org.example.service.FmeaConfigService;
import org.example.service.FmeaService;

import java.util.NoSuchElementException;

public class FmeaController {

    private final FmeaConfigService config;
    private final FmeaService fmea;

    public FmeaController(FmeaConfigService config, FmeaService fmea) {
        this.config = config;
        this.fmea = fmea;
    }

    // --- Configuración ---

    public void addScaleLevel(Context ctx) {
        var in = ctx.bodyAsClass(FmeaDtos.ScaleLevelRequest.class);
        var created = config.addScaleLevel(in.factorType(), required(in.value(), "value"),
                in.descriptor());
        ctx.status(201).json(FmeaDtos.ScaleLevelDto.from(created));
    }

    public void listScaleLevels(Context ctx) {
        ctx.json(config.scaleLevels().stream().map(FmeaDtos.ScaleLevelDto::from).toList());
    }

    public void addCategory(Context ctx) {
        var in = ctx.bodyAsClass(FmeaDtos.CategoryRequest.class);
        var created = config.addCategory(in.name(), required(in.minRpn(), "minRpn"),
                required(in.maxRpn(), "maxRpn"), in.justification());
        ctx.status(201).json(FmeaDtos.CategoryDto.from(created));
    }

    public void listCategories(Context ctx) {
        ctx.json(config.categories().stream().map(FmeaDtos.CategoryDto::from).toList());
    }

    /** 204 si la configuración es completa y consistente; 409 con el motivo si no. */
    public void checkConfig(Context ctx) {
        config.calculator();
        ctx.status(204);
    }

    // --- Modos de fallo ---

    public void createFailureMode(Context ctx) {
        var in = ctx.bodyAsClass(FmeaDtos.FailureModeRequest.class);
        var created = fmea.createFailureMode(in.modelElementId(), in.description());
        ctx.status(201).json(FmeaDtos.FailureModeDto.from(created));
    }

    public void getFailureMode(Context ctx) {
        Long id = Params.id(ctx);
        var mode = fmea.findFailureMode(id).orElseThrow(
                () -> new NoSuchElementException("Modo de fallo no encontrado: " + id));
        ctx.json(FmeaDtos.FailureModeDto.from(mode));
    }

    // --- Análisis FMEA ---

    public void createAnalysis(Context ctx) {
        var in = ctx.bodyAsClass(FmeaDtos.CreateFmeaRequest.class);
        var view = fmea.createAnalysis(required(in.harmId(), "harmId"),
                required(in.failureModeId(), "failureModeId"), in.effect(), in.cause(),
                in.detectionMethod(), in.existingControls(), in.source(),
                required(in.severity(), "severity"), required(in.occurrence(), "occurrence"),
                required(in.detection(), "detection"));
        ctx.status(201).json(FmeaDtos.FmeaViewDto.from(view));
    }

    public void listAnalyses(Context ctx) {
        Long harmId = Params.requiredLong(ctx, "harmId");
        ctx.json(fmea.listByHarm(harmId).stream().map(FmeaDtos.FmeaViewDto::from).toList());
    }

    public void getAnalysis(Context ctx) {
        Long id = Params.id(ctx);
        var view = fmea.find(id).orElseThrow(
                () -> new NoSuchElementException("Análisis FMEA no encontrado: " + id));
        ctx.json(FmeaDtos.FmeaViewDto.from(view));
    }

    /** Cambia S, O o D; el RPN se recalcula y la fila anterior queda en el historial (RF-24, RF-29). */
    public void reevaluate(Context ctx) {
        var in = ctx.bodyAsClass(FmeaDtos.FactorsRequest.class);
        var view = fmea.reevaluate(Params.id(ctx), required(in.severity(), "severity"),
                required(in.occurrence(), "occurrence"), required(in.detection(), "detection"));
        ctx.json(FmeaDtos.FmeaViewDto.from(view));
    }

    public void history(Context ctx) {
        ctx.json(fmea.history(Params.id(ctx)).stream().map(FmeaDtos.RpnFactorDto::from).toList());
    }

    private static <T> T required(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException("El campo " + name + " es obligatorio");
        }
        return value;
    }
}