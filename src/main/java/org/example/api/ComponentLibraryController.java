package org.example.api;

import io.javalin.http.Context;
import org.example.api.ComponentLibraryDtos.EntryDto;
import org.example.api.ComponentLibraryDtos.MatchDto;
import org.example.repository.ComponentLibraryRepository.Entry;
import org.example.service.ComponentLibraryService;

public class ComponentLibraryController {

    private final ComponentLibraryService service;

    public ComponentLibraryController(ComponentLibraryService service) {
        this.service = service;
    }

    public void list(Context ctx) {
        ctx.json(service.all().stream().map(ComponentLibraryController::dto).toList());
    }

    public void search(Context ctx) {
        String q = ctx.queryParam("q");
        int limit = ctx.queryParam("limit") == null ? 10 : Integer.parseInt(ctx.queryParam("limit"));
        ctx.json(service.search(q, limit).stream()
                .map(m -> new MatchDto(dto(m.entry()), m.score())).toList());
    }

    private static EntryDto dto(Entry e) {
        return new EntryDto(e.code(), e.name(), e.definition(), e.parentCode());
    }
}
