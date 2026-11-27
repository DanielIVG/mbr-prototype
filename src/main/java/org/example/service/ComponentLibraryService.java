package org.example.service;

import org.example.repository.ComponentLibraryRepository;
import org.example.repository.ComponentLibraryRepository.Entry;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ComponentLibraryService {

    /** Umbral mínimo de similitud para que un término aparezca como resultado de búsqueda. */
    static final double MIN_SCORE = 0.45;

    public record Match(Entry entry, double score) {}

    private final ComponentLibraryRepository repo;

    public ComponentLibraryService(ComponentLibraryRepository repo) {
        this.repo = repo;
    }

    public List<Entry> all() {
        return repo.all();
    }

    /** Términos más parecidos a la consulta, de mayor a menor similitud. */
    public List<Match> search(String query, int limit) {
        Validation.requireText(query, "Escriba qué componente busca");
        int max = Math.max(1, Math.min(limit, 50));
        return repo.all().stream()
                .map(e -> new Match(e, TextMatch.similarity(query, e.name())))
                .filter(m -> m.score() >= MIN_SCORE)
                .sorted(Comparator.comparingDouble(Match::score).reversed()
                        .thenComparing(m -> m.entry().name()))
                .limit(max)
                .toList();
    }

    /** El término de la biblioteca cuyo nombre coincide exactamente (sin mayúsculas, tildes ni signos). */
    public Optional<Entry> resolve(String name) {
        String n = TextMatch.normalize(name);
        if (n.isEmpty()) {
            return Optional.empty();
        }
        return repo.all().stream().filter(e -> TextMatch.normalize(e.name()).equals(n)).findFirst();
    }
}
