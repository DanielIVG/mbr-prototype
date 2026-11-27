package org.example.service;

import org.example.repository.BehaviorRepository;
import org.example.repository.BehaviorRepository.ChainStep;
import org.example.repository.BehaviorRepository.NewNode;
import org.example.repository.BehaviorRepository.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Árbol de comportamiento por componente. Reglas:
 * raíz = peligro (acción o condición); una condición tiene ramas SÍ y NO (una de cada una);
 * una acción tiene una sola salida; los resultados son hojas; un resultado peligroso genera la cadena
 * de riesgo. Un camino que ya generó una cadena no se edita ni se elimina (se crea una rama nueva).
 */
public class BehaviorService {

    private static final Set<String> KINDS = Set.of("ACTION", "CONDITION", "OUTCOME_SAFE", "OUTCOME_HAZARD");

    public record View(Node node, boolean locked) {}

    private final BehaviorRepository repo;

    public BehaviorService(BehaviorRepository repo) {
        this.repo = repo;
    }

    /** Árboles que pasan por el componente (raíz o algún nodo en él); {@code locked} = forma parte de una cadena generada. */
    public List<View> list(long elementId) {
        if (!repo.elementExists(elementId)) {
            throw new NoSuchElementException("Componente no encontrado: " + elementId);
        }
        return withLock(repo.listTreesThrough(elementId));
    }

    /** Nodos de todos los árboles del dispositivo. */
    public List<View> listByDevice(long deviceId) {
        return withLock(repo.listByDevice(deviceId));
    }

    private static List<View> withLock(List<Node> nodes) {
        Map<Long, List<Node>> kids = new HashMap<>();
        for (Node n : nodes) {
            if (n.parentId() != null) {
                kids.computeIfAbsent(n.parentId(), k -> new ArrayList<>()).add(n);
            }
        }
        List<View> out = new ArrayList<>();
        for (Node n : nodes) {
            out.add(new View(n, hasHazard(n, kids)));
        }
        return out;
    }

    private static boolean hasHazard(Node n, Map<Long, List<Node>> kids) {
        if ("OUTCOME_HAZARD".equals(n.kind())) {
            return true;
        }
        for (Node c : kids.getOrDefault(n.id(), List.of())) {
            if (hasHazard(c, kids)) {
                return true;
            }
        }
        return false;
    }

    public Node create(Long elementId, Long parentId, String kind, String description, String branch,
                       String situation, String harm, String harmType) {
        if (kind == null || !KINDS.contains(kind)) {
            throw new IllegalArgumentException("Tipo de nodo desconocido: " + kind);
        }
        Validation.requireText(description, "La descripción del nodo es obligatoria");
        boolean hazardLeaf = "OUTCOME_HAZARD".equals(kind);
        if (hazardLeaf) {
            Validation.requireText(situation, "El resultado peligroso necesita la situación peligrosa");
            Validation.requireText(harm, "El resultado peligroso necesita el daño");
        }
        String cleanBranch = branch == null || branch.isBlank() ? null : branch.trim().toUpperCase();
        if (cleanBranch != null && !cleanBranch.equals("YES") && !cleanBranch.equals("NO")) {
            throw new IllegalArgumentException("La rama debe ser YES o NO");
        }

        Node parent = null;
        Node root = null;
        long element;
        if (parentId == null) {
            if (!kind.equals("ACTION") && !kind.equals("CONDITION")) {
                throw new IllegalArgumentException("La raíz del árbol (el peligro) debe ser una acción o una condición");
            }
            if (cleanBranch != null) {
                throw new IllegalArgumentException("Una raíz no tiene rama");
            }
            if (elementId == null) {
                throw new IllegalArgumentException("Indique el componente de la raíz");
            }
            element = elementId;
        } else {
            parent = repo.find(parentId).orElseThrow(
                    () -> new NoSuchElementException("Nodo padre no encontrado: " + parentId));
            if (parent.kind().startsWith("OUTCOME")) {
                throw new IllegalArgumentException("Un resultado es una hoja: no puede tener nodos debajo");
            }
            List<Node> siblings = repo.children(parent.id());
            if (parent.kind().equals("ACTION")) {
                if (cleanBranch != null) {
                    throw new IllegalArgumentException("Una acción tiene una sola salida, sin rama SÍ/NO");
                }
                if (!siblings.isEmpty()) {
                    throw new ConflictException("Esta acción ya tiene su salida. Use una condición para ramificar.");
                }
            } else {
                if (cleanBranch == null) {
                    throw new IllegalArgumentException("Debajo de una condición indique la rama: YES o NO");
                }
                for (Node s : siblings) {
                    if (cleanBranch.equals(s.branch())) {
                        throw new ConflictException("La rama " + cleanBranch + " de esta condición ya existe");
                    }
                }
            }
            element = elementId != null ? elementId : parent.elementId();
            long rootId = parent.rootId() != null ? parent.rootId() : parent.id();
            root = repo.find(rootId).orElseThrow(() -> new NoSuchElementException("Raíz no encontrada"));
        }
        if (!repo.elementExists(element)) {
            throw new IllegalArgumentException("El componente no existe: " + element);
        }

        NewNode node = new NewNode(element, parentId, kind, description.trim(), cleanBranch,
                hazardLeaf ? situation.trim() : null, hazardLeaf ? harm.trim() : null,
                hazardLeaf ? harmType : null);
        List<ChainStep> steps = hazardLeaf ? pathSteps(parent, cleanBranch) : List.of();
        return repo.insert(node, root == null ? null : root.id(), root, steps);
    }

    /** Pasos de raíz a padre; cada condición dice qué rama se tomó. */
    private List<ChainStep> pathSteps(Node parent, String leafBranch) {
        List<Node> path = new ArrayList<>();
        for (Node n = parent; n != null; n = n.parentId() == null ? null : repo.find(n.parentId()).orElse(null)) {
            path.add(n);
        }
        Collections.reverse(path);
        List<ChainStep> steps = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            Node n = path.get(i);
            String taken = i + 1 < path.size() ? path.get(i + 1).branch() : leafBranch;
            String text = n.kind().equals("CONDITION") && taken != null
                    ? n.description() + " → " + (taken.equals("YES") ? "SÍ" : "NO")
                    : n.description();
            steps.add(new ChainStep(text, n.elementId()));
        }
        return steps;
    }

    public Node updateDescription(long id, String description) {
        Validation.requireText(description, "La descripción del nodo es obligatoria");
        Node n = repo.find(id).orElseThrow(() -> new NoSuchElementException("Nodo no encontrado: " + id));
        requireNotInChain(n);
        repo.updateDescription(id, description.trim());
        return repo.find(id).orElseThrow();
    }

    /** Desactiva el nodo y todo lo que cuelga de él. */
    public void deactivate(long id) {
        Node n = repo.find(id).orElseThrow(() -> new NoSuchElementException("Nodo no encontrado: " + id));
        requireNotInChain(n);
        repo.deactivate(subtree(n));
    }

    private void requireNotInChain(Node n) {
        for (long id : subtree(n)) {
            Node x = repo.find(id).orElseThrow();
            if ("OUTCOME_HAZARD".equals(x.kind())) {
                throw new ConflictException("Este camino ya generó una cadena de riesgo; no se modifica. "
                        + "Cree una rama nueva para representar el cambio.");
            }
        }
    }

    private List<Long> subtree(Node n) {
        List<Long> ids = new ArrayList<>();
        ids.add(n.id());
        for (Node c : repo.children(n.id())) {
            ids.addAll(subtree(c));
        }
        return ids;
    }
}
