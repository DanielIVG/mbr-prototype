package org.example.api;

import io.javalin.http.Context;
import org.example.service.RpnDashboardService;

public class DashboardController {

    private final RpnDashboardService service;

    public DashboardController(RpnDashboardService service) {
        this.service = service;
    }

    public void rpn(Context ctx) {
        ctx.json(DashboardDtos.DashboardDto.from(service.dashboard(Params.id(ctx))));
    }
}
