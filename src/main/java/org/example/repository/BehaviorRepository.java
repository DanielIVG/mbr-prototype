package org.example.repository;

import org.hibernate.SessionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Árbol de comportamiento (V5). Una hoja de resultado peligroso crea, en la misma transacción,
 * la cadena de riesgo de V1 (peligro → pasos → situación peligrosa → daño).
 */
public class BehaviorRepository {

    public record Node(long id, long elementId, Long parentId, Long rootId, String kind, String description,
                       String branch, String situation, String harm, String harmType, Long harmId) {}

    public record NewNode(long elementId, Long parentId, String kind, String description, String branch,
                          String situation, String harm, String harmType) {}

    @FunctionalInterface
    private interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    private static final String COLS = "id, model_element_id, parent_id, root_id, kind, description, branch, "
            + "situation_description, harm_description, harm_type, harm_id";

    private final SessionFactory sf;

    public BehaviorRepository(SessionFactory sf) {
        this.sf = sf;
    }

    private <T> T work(Work<T> w) {
        return sf.fromTransaction(s -> s.doReturningWork(w::run));
    }

    public boolean elementExists(long id) {
        return work(c -> scalar(c, "select count(*) from model_element where id = ?", id) > 0);
    }

    public Optional<Node> find(long id) {
        return work(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "select " + COLS + " from behavior_node where id = ? and active = 1")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(read(rs)) : Optional.<Node>empty();
                }
            }
        });
    }

    public List<Node> children(long parentId) {
        return work(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "select " + COLS + " from behavior_node where parent_id = ? and active = 1 order by id")) {
                ps.setLong(1, parentId);
                return readAll(ps);
            }
        });
    }

    private static final String TREE_COLS = "n.id, n.model_element_id, n.parent_id, n.root_id, n.kind, "
            + "n.description, n.branch, n.situation_description, n.harm_description, n.harm_type, n.harm_id";

    /** Árboles completos cuya raíz está en el componente o que tienen algún nodo en él. */
    public List<Node> listTreesThrough(long elementId) {
        return work(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "select " + TREE_COLS + " from behavior_node n "
                            + "join behavior_node r on r.id = coalesce(n.root_id, n.id) "
                            + "where n.active = 1 and r.active = 1 and (r.model_element_id = ? or exists ("
                            + "select 1 from behavior_node m where coalesce(m.root_id, m.id) = r.id "
                            + "and m.active = 1 and m.model_element_id = ?)) order by n.id")) {
                ps.setLong(1, elementId);
                ps.setLong(2, elementId);
                return readAll(ps);
            }
        });
    }

    /** Todos los nodos vigentes de los árboles cuya raíz pertenece a un componente del dispositivo. */
    public List<Node> listByDevice(long deviceId) {
        return work(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "select " + TREE_COLS + " from behavior_node n "
                            + "join behavior_node r on r.id = coalesce(n.root_id, n.id) "
                            + "join component k on k.id = r.model_element_id "
                            + "where k.device_id = ? and n.active = 1 and r.active = 1 order by n.id")) {
                ps.setLong(1, deviceId);
                return readAll(ps);
            }
        });
    }

    /** Inserta un nodo; si es resultado peligroso crea también la cadena, con {@code pathSteps} como pasos. */
    public Node insert(NewNode n, Long rootId, Node rootNode, List<ChainStep> pathSteps) {
        return work(c -> {
            Long harmId = null;
            if ("OUTCOME_HAZARD".equals(n.kind())) {
                harmId = createChain(c, rootNode, pathSteps, n);
            }
            long id;
            try (PreparedStatement ps = c.prepareStatement(
                    "insert into behavior_node(model_element_id, parent_id, root_id, kind, description, branch, "
                            + "situation_description, harm_description, harm_type, harm_id, active, created_date) "
                            + "values (?,?,?,?,?,?,?,?,?,?,1,?)")) {
                ps.setLong(1, n.elementId());
                setLong(ps, 2, n.parentId());
                setLong(ps, 3, rootId);
                ps.setString(4, n.kind());
                ps.setString(5, n.description());
                ps.setString(6, n.branch());
                ps.setString(7, n.situation());
                ps.setString(8, n.harm());
                ps.setString(9, n.harmType());
                setLong(ps, 10, harmId);
                ps.setString(11, Instant.now().toString());
                ps.executeUpdate();
            }
            id = scalar(c, "select last_insert_rowid()");
            try (PreparedStatement ps = c.prepareStatement(
                    "select " + COLS + " from behavior_node where id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return read(rs);
                }
            }
        });
    }

    public record ChainStep(String description, long elementId) {}

    private static long createChain(Connection c, Node root, List<ChainStep> steps, NewNode leaf)
            throws SQLException {
        long hazardId;
        try (PreparedStatement ps = c.prepareStatement(
                "insert into hazard(model_element_id, description, source) values (?,?,?)")) {
            ps.setLong(1, root.elementId());
            ps.setString(2, root.description());
            ps.setString(3, "Árbol de comportamiento (nodo " + root.id() + ")");
            ps.executeUpdate();
        }
        hazardId = scalar(c, "select last_insert_rowid()");
        long lastSequence = 0;
        int order = 1;
        for (ChainStep step : steps) {
            try (PreparedStatement ps = c.prepareStatement(
                    "insert into sequence_of_events(hazard_id, description, step_order, model_element_id) "
                            + "values (?,?,?,?)")) {
                ps.setLong(1, hazardId);
                ps.setString(2, step.description());
                ps.setInt(3, order++);
                ps.setLong(4, step.elementId());
                ps.executeUpdate();
            }
            lastSequence = scalar(c, "select last_insert_rowid()");
        }
        try (PreparedStatement ps = c.prepareStatement(
                "insert into hazardous_situation(sequence_of_events_id, description) values (?,?)")) {
            ps.setLong(1, lastSequence);
            ps.setString(2, leaf.situation());
            ps.executeUpdate();
        }
        long situationId = scalar(c, "select last_insert_rowid()");
        try (PreparedStatement ps = c.prepareStatement(
                "insert into harm(hazardous_situation_id, description, type) values (?,?,?)")) {
            ps.setLong(1, situationId);
            ps.setString(2, leaf.harm());
            ps.setString(3, leaf.harmType());
            ps.executeUpdate();
        }
        return scalar(c, "select last_insert_rowid()");
    }

    public void updateDescription(long id, String description) {
        work(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "update behavior_node set description = ? where id = ?")) {
                ps.setString(1, description);
                ps.setLong(2, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void deactivate(List<Long> ids) {
        work(c -> {
            try (PreparedStatement ps = c.prepareStatement("update behavior_node set active = 0 where id = ?")) {
                for (long id : ids) {
                    ps.setLong(1, id);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return null;
        });
    }

    // ------------------------------------------------------------------ utilidades

    private static Node read(ResultSet rs) throws SQLException {
        return new Node(rs.getLong(1), rs.getLong(2), longOrNull(rs, 3), longOrNull(rs, 4), rs.getString(5),
                rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10),
                longOrNull(rs, 11));
    }

    private static List<Node> readAll(PreparedStatement ps) throws SQLException {
        List<Node> out = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(read(rs));
            }
        }
        return out;
    }

    private static Long longOrNull(ResultSet rs, int i) throws SQLException {
        long v = rs.getLong(i);
        return rs.wasNull() ? null : v;
    }

    private static void setLong(PreparedStatement ps, int i, Long v) throws SQLException {
        if (v == null) {
            ps.setNull(i, Types.INTEGER);
        } else {
            ps.setLong(i, v);
        }
    }

    private static long scalar(Connection c, String sql, long... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setLong(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
}
