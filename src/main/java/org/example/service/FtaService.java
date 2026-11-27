package org.example.service;

import org.example.iso.Amount;
import org.example.iso.FtaModel;
import org.example.repository.AssessmentRepository.QuantityRow;
import org.example.repository.FtaRepository;
import org.example.repository.FtaRepository.BasicRow;
import org.example.repository.FtaRepository.Saved;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/** Árbol de fallas de un daño: valida, calcula y guarda. Su resultado puede alimentar P1. */
public class FtaService {

    public record Result(Saved saved, FtaModel.Evaluation evaluation) {}

    private final FtaRepository repo;

    public FtaService(FtaRepository repo) {
        this.repo = repo;
    }

    public Result save(long harmId, String topEvent, List<BasicRow> basics, FtaModel.Node root) {
        if (!repo.harmExists(harmId)) {
            throw new NoSuchElementException("Daño no encontrado: " + harmId);
        }
        for (BasicRow b : basics) {
            requireProvenance(b);
        }
        FtaModel.Evaluation eval = FtaModel.evaluate(spec(topEvent, basics, root));      // valida todo antes de guardar
        if (Amount.ADIMENSIONAL.equals(eval.unit())) {
            throw new IllegalArgumentException("El evento superior no tiene unidad de exposición: al menos un evento "
                    + "debe expresarse por hora, por ciclo o por uso para obtener P1");
        }
        long id = repo.save(harmId, topEvent.trim(), basics, root);
        return new Result(repo.current(harmId).filter(s -> s.analysisId() == id).orElseThrow(), eval);
    }

    public Optional<Result> current(long harmId) {
        return repo.current(harmId).map(s -> new Result(s, FtaModel.evaluate(spec(s.topEvent(), s.basics(), s.root()))));
    }

    public static FtaModel.Spec spec(String topEvent, List<BasicRow> basics, FtaModel.Node root) {
        return new FtaModel.Spec(topEvent, basics.stream().map(b -> new FtaModel.Basic(b.name(), b.name(),
                new Amount(b.quantity().value(), b.quantity().unit()), b.quantity().min(), b.quantity().max())).toList(),
                root);
    }

    private static void requireProvenance(BasicRow b) {
        QuantityRow q = b.quantity();
        if (b.name() == null || b.name().isBlank()) {
            throw new IllegalArgumentException("Todo evento básico necesita un nombre");
        }
        if (q.source() == null || q.source().isBlank()) {
            throw new IllegalArgumentException("El evento '" + b.name() + "' necesita su fuente");
        }
        if (q.evidence() == null || q.evidence().isBlank()) {
            throw new IllegalArgumentException("El evento '" + b.name() + "' necesita su nivel de evidencia");
        }
        if (q.author() == null || q.author().isBlank()) {
            throw new IllegalArgumentException("El evento '" + b.name() + "' necesita su autor");
        }
    }
}
