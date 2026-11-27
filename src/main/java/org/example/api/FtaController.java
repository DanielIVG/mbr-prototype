package org.example.api;

import io.javalin.http.Context;
import org.example.api.AssessmentDtos.QuantityDto;
import org.example.api.FtaDtos.*;
import org.example.iso.FaultTree;
import org.example.iso.FtaModel;
import org.example.repository.AssessmentRepository.QuantityRow;
import org.example.repository.FtaRepository.BasicRow;
import org.example.service.FtaService;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class FtaController {

    private final FtaService service;

    public FtaController(FtaService service) {
        this.service = service;
    }

    public void save(Context ctx) {
        var in = ctx.bodyAsClass(FtaDto.class);
        List<BasicRow> basics = new ArrayList<>();
        for (BasicDto b : in.basics()) {
            QuantityDto q = b.quantity();
            if (q == null) {
                throw new IllegalArgumentException("El evento '" + b.name() + "' necesita su probabilidad");
            }
            basics.add(new BasicRow(b.name(), new QuantityRow("P", 1, q.value(), q.unit(), q.min(), q.max(),
                    q.source(), q.evidence(), q.author()), b.modelElementId()));
        }
        var result = service.save(Params.id(ctx), in.topEvent(), basics, toNode(in.root()));
        ctx.status(201).json(toDto(result));
    }

    public void get(Context ctx) {
        var result = service.current(Params.id(ctx)).orElseThrow(
                () -> new NoSuchElementException("Este daño todavía no tiene árbol de fallas"));
        ctx.json(toDto(result));
    }

    private static FtaModel.Node toNode(NodeDto n) {
        if (n == null) {
            throw new IllegalArgumentException("El árbol está vacío");
        }
        if ("BASIC".equals(n.kind())) {
            return new FtaModel.Ref(n.ref());
        }
        if (!"GATE".equals(n.kind())) {
            throw new IllegalArgumentException("Tipo de nodo desconocido: " + n.kind());
        }
        FaultTree.Gate gate;
        try {
            gate = FaultTree.Gate.valueOf(n.gate());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Tipo de compuerta desconocido: " + n.gate() + " (use AND u OR)");
        }
        List<FtaModel.Node> kids = new ArrayList<>();
        for (NodeDto c : n.children() == null ? List.<NodeDto>of() : n.children()) {
            kids.add(toNode(c));
        }
        return new FtaModel.GateSpec(n.label(), gate, n.independence(), kids);
    }

    private static NodeDto toDto(FtaModel.Node node) {
        if (node instanceof FtaModel.Ref r) {
            return new NodeDto("BASIC", null, null, null, null, r.key());
        }
        FtaModel.GateSpec g = (FtaModel.GateSpec) node;
        return new NodeDto("GATE", g.label(), g.type().name(), g.independenceJustification(),
                g.children().stream().map(FtaController::toDto).toList(), null);
    }

    private static FtaDto toDto(FtaService.Result r) {
        var e = r.evaluation();
        return new FtaDto(r.saved().analysisId(), r.saved().topEvent(),
                r.saved().basics().stream().map(b -> new BasicDto(b.name(),
                        new QuantityDto("P", b.quantity().value(), b.quantity().unit(), b.quantity().min(),
                                b.quantity().max(), b.quantity().source(), b.quantity().evidence(),
                                b.quantity().author()), b.modelElementId())).toList(),
                toDto(r.saved().root()),
                new ResultDto(e.probability(), e.min(), e.max(), e.unit(), e.exact(),
                        e.cutSets().stream().map(c -> new CutSetDto(c.events(), c.probability())).toList(),
                        e.singlePoints()));
    }
}
