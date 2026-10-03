package org.example.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.Javalin;
import org.example.config.Services;
import org.example.service.ConflictException;

import java.util.Map;
import java.util.NoSuchElementException;

public class ApiServer {

    private final Javalin app;

    public ApiServer(Services services) {
        var devices = services.devices();
        var components = new ComponentController(services.components());
        var functions = new FunctionController(services.functions());
        var interactions = new InteractionController(services.interactions());
        var chains = new RiskChainController(services.chains());
        var fmea = new FmeaController(services.fmeaConfig(), services.fmea());

        this.app = Javalin.create(config -> {
            config.jetty.host = "127.0.0.1"; // solo accesible desde esta máquina
            config.jetty.port = 0;           // puerto libre asignado por el sistema

            config.routes.get("/health", ctx -> ctx.result("ok"));

            // Dispositivos
            config.routes.get("/devices", ctx ->
                    ctx.json(devices.listActive().stream().map(DeviceDto::from).toList()));

            config.routes.get("/devices/{id}", ctx -> {
                Long id = Long.parseLong(ctx.pathParam("id"));
                var device = devices.find(id).orElseThrow(
                        () -> new NoSuchElementException("Dispositivo no encontrado: " + id));
                ctx.json(DeviceDto.from(device));
            });

            config.routes.post("/devices", ctx -> {
                var in = ctx.bodyAsClass(DeviceDto.class);
                var created = devices.create(in.name(), in.intendedUse(),
                        in.reasonablyForeseeableMisuse());
                ctx.status(201).json(DeviceDto.from(created));
            });

            config.routes.put("/devices/{id}", ctx -> {
                Long id = Long.parseLong(ctx.pathParam("id"));
                var in = ctx.bodyAsClass(DeviceDto.class);
                var updated = devices.update(id, in.name(), in.intendedUse(),
                        in.reasonablyForeseeableMisuse());
                ctx.json(DeviceDto.from(updated));
            });

            config.routes.post("/devices/{id}/deactivate", ctx -> {
                devices.deactivate(Long.parseLong(ctx.pathParam("id")));
                ctx.status(204);
            });

            // Componentes
            config.routes.get("/components", components::list);
            config.routes.get("/components/{id}", components::get);
            config.routes.post("/components", components::create);
            config.routes.put("/components/{id}", components::update);
            config.routes.post("/components/{id}/deactivate", components::deactivate);
            config.routes.delete("/components/{id}", components::delete);

            // Funciones
            config.routes.get("/functions", functions::list);
            config.routes.get("/functions/{id}", functions::get);
            config.routes.post("/functions", functions::create);
            config.routes.put("/functions/{id}", functions::update);
            config.routes.post("/functions/{id}/deactivate", functions::deactivate);
            config.routes.delete("/functions/{id}", functions::delete);

            // Interacciones
            config.routes.get("/interactions", interactions::list);
            config.routes.get("/interactions/{id}", interactions::get);
            config.routes.post("/interactions", interactions::create);
            config.routes.put("/interactions/{id}", interactions::update);
            config.routes.post("/interactions/{id}/deactivate", interactions::deactivate);
            config.routes.delete("/interactions/{id}", interactions::delete);

            // Cadena de riesgo (la {id} de GET /risk-chains/{id} es el id del daño)
            config.routes.post("/risk-chains", chains::createChain);
            config.routes.get("/risk-chains", chains::listChains);
            config.routes.get("/risk-chains/{id}", chains::getChain);
            config.routes.post("/hazards/{id}/sequences", chains::addSequence);
            config.routes.post("/sequences/{id}/situations", chains::addSituation);
            config.routes.post("/situations/{id}/harms", chains::addHarm);
            config.routes.put("/hazards/{id}", chains::updateHazard);
            config.routes.put("/sequences/{id}", chains::updateSequence);
            config.routes.put("/situations/{id}", chains::updateSituation);
            config.routes.put("/harms/{id}", chains::updateHarm);

            // FMEA: configuración del caso de estudio
            config.routes.get("/fmea/scale-levels", fmea::listScaleLevels);
            config.routes.post("/fmea/scale-levels", fmea::addScaleLevel);
            config.routes.get("/fmea/categories", fmea::listCategories);
            config.routes.post("/fmea/categories", fmea::addCategory);
            config.routes.get("/fmea/config/check", fmea::checkConfig);

            // FMEA: modos de fallo y análisis
            config.routes.post("/failure-modes", fmea::createFailureMode);
            config.routes.get("/failure-modes/{id}", fmea::getFailureMode);
            config.routes.post("/fmea-analyses", fmea::createAnalysis);
            config.routes.get("/fmea-analyses", fmea::listAnalyses);
            config.routes.get("/fmea-analyses/{id}", fmea::getAnalysis);
            config.routes.put("/fmea-analyses/{id}/factors", fmea::reevaluate);
            config.routes.get("/fmea-analyses/{id}/history", fmea::history);

            config.routes.exception(IllegalArgumentException.class, (e, ctx) ->
                    ctx.status(400).json(Map.of("error", String.valueOf(e.getMessage()))));
            config.routes.exception(NoSuchElementException.class, (e, ctx) ->
                    ctx.status(404).json(Map.of("error", String.valueOf(e.getMessage()))));
            config.routes.exception(ConflictException.class, (e, ctx) ->
                    ctx.status(409).json(Map.of("error", String.valueOf(e.getMessage()))));
            // Cuerpo JSON mal formado o con valores no válidos (por ejemplo, un factor inexistente)
            config.routes.exception(JsonProcessingException.class, (e, ctx) ->
                    ctx.status(400).json(Map.of("error",
                            "El cuerpo de la petición no es válido: " + e.getOriginalMessage())));
        });
    }

    public ApiServer start() {
        app.start();
        return this;
    }

    public int port() {
        return app.port();
    }

    public void stop() {
        app.stop();
    }
}