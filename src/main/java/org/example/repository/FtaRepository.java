package org.example.repository;

import org.example.iso.Amount;
import org.example.iso.FaultTree.Gate;
import org.example.iso.FtaModel;
import org.example.repository.AssessmentRepository.QuantityRow;
import org.hibernate.SessionFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Árbol de fallas por daño: analysis (tipo FTA) + fta_analysis + fta_event (V1/V2).
 * Cada evento básico es una cantidad con procedencia; si el mismo evento aparece en varias ramas,
 * todas las hojas comparten la misma fila de quantity (así se reconoce como una sola variable).
 * El nombre del evento es su clave. Solo hay un árbol vigente por daño; guardar otro desactiva el anterior.
 */
public class FtaRepository {

    public record BasicRow(String name, QuantityRow quantity, Long modelElementId) {}

    public record Saved(long analysisId, long harmId, String topEvent, List<BasicRow> basics,
                        FtaModel.Node root, String date) {}

    @FunctionalInterface
    private interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    private record Row(long id, Long parent, String description, String gate, boolean top, String type,
                       String justification, Long probabilityId, Long elementId) {}

    private final SessionFactory sf;

    public FtaRepository(SessionFactory sf) {
        this.sf = sf;
    }

    private <T> T work(Work<T> w) {
        return sf.fromTransaction(s -> s.doReturningWork(w::run));
    }

    public boolean harmExists(long harmId) {
        return work(c -> scalar(c, "select count(*) from harm where id = " + harmId) > 0);
    }

    public long save(long harmId, String topEvent, List<BasicRow> basics, FtaModel.Node root) {
        return work(c -> {
            try (var st = c.createStatement()) {
                st.executeUpdate("update analysis set active = 0 where harm_id = " + harmId
                        + " and analysis_type = 'FTA' and active = 1");
            }
            String now = Instant.now().toString();
            long analysisId;
            try (var ps = c.prepareStatement(
                    "insert into analysis(harm_id, analysis_type, created_date, active) values (?, 'FTA', ?, 1)")) {
                ps.setLong(1, harmId);
                ps.setString(2, now);
                ps.executeUpdate();
            }
            analysisId = scalar(c, "select last_insert_rowid()");
            try (var ps = c.prepareStatement(
                    "insert into fta_analysis(id, top_event_description) values (?, ?)")) {
                ps.setLong(1, analysisId);
                ps.setString(2, topEvent);
                ps.executeUpdate();
            }
            Map<String, Long> quantityIds = new HashMap<>();
            Map<String, Long> elementIds = new HashMap<>();
            for (BasicRow b : basics) {
                quantityIds.put(b.name(), insertQuantity(c, b.quantity()));
                if (b.modelElementId() != null) {
                    elementIds.put(b.name(), b.modelElementId());
                }
            }
            insertNode(c, analysisId, null, root, true, quantityIds, elementIds);
            return analysisId;
        });
    }

    private static void insertNode(Connection c, long analysisId, Long parentId, FtaModel.Node node, boolean top,
                                   Map<String, Long> quantityIds, Map<String, Long> elementIds) throws SQLException {
        if (node instanceof FtaModel.Ref r) {
            try (var ps = c.prepareStatement("insert into fta_event(fta_analysis_id, parent_event_id, description, "
                    + "is_top_event, event_type, probability_id, model_element_id) values (?, ?, ?, 0, 'BASIC', ?, ?)")) {
                ps.setLong(1, analysisId);
                setLong(ps, 2, parentId);
                ps.setString(3, r.key());
                ps.setLong(4, quantityIds.get(r.key()));
                setLong(ps, 5, elementIds.get(r.key()));
                ps.executeUpdate();
            }
            return;
        }
        FtaModel.GateSpec g = (FtaModel.GateSpec) node;
        try (var ps = c.prepareStatement("insert into fta_event(fta_analysis_id, parent_event_id, description, "
                + "logical_gate_type, is_top_event, event_type, independence_justification) "
                + "values (?, ?, ?, ?, ?, 'INTERMEDIATE', ?)")) {
            ps.setLong(1, analysisId);
            setLong(ps, 2, parentId);
            ps.setString(3, g.label());
            ps.setString(4, g.type().name());
            ps.setInt(5, top ? 1 : 0);
            ps.setString(6, g.independenceJustification());
            ps.executeUpdate();
        }
        long id = scalar(c, "select last_insert_rowid()");
        for (FtaModel.Node child : g.children()) {
            insertNode(c, analysisId, id, child, false, quantityIds, elementIds);
        }
    }

    /** Árbol vigente del daño, si existe. */
    public Optional<Saved> current(long harmId) {
        return work(c -> {
            long analysisId;
            String topEvent;
            String date;
            try (var ps = c.prepareStatement("select a.id, f.top_event_description, a.created_date from analysis a "
                    + "join fta_analysis f on f.id = a.id where a.harm_id = ? and a.analysis_type = 'FTA' "
                    + "and a.active = 1 order by a.id desc limit 1")) {
                ps.setLong(1, harmId);
                try (var rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.<Saved>empty();
                    }
                    analysisId = rs.getLong(1);
                    topEvent = rs.getString(2);
                    date = rs.getString(3);
                }
            }
            return Optional.of(load(c, harmId, analysisId, topEvent, date));
        });
    }

    private static Saved load(Connection c, long harmId, long analysisId, String topEvent, String date)
            throws SQLException {
        List<Row> rows = new ArrayList<>();
        try (var ps = c.prepareStatement("select id, parent_event_id, description, logical_gate_type, is_top_event, "
                + "event_type, independence_justification, probability_id, model_element_id "
                + "from fta_event where fta_analysis_id = ? order by id")) {
            ps.setLong(1, analysisId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Row(rs.getLong(1), longOrNull(rs, 2), rs.getString(3), rs.getString(4),
                            rs.getInt(5) == 1, rs.getString(6), rs.getString(7), longOrNull(rs, 8), longOrNull(rs, 9)));
                }
            }
        }
        Map<Long, List<Row>> children = new LinkedHashMap<>();
        Row top = null;
        for (Row r : rows) {
            if (r.top()) {
                top = r;
            }
            if (r.parent() != null) {
                children.computeIfAbsent(r.parent(), k -> new ArrayList<>()).add(r);
            }
        }
        if (top == null) {
            throw new IllegalStateException("El árbol de fallas " + analysisId + " no tiene evento superior");
        }
        Map<String, BasicRow> basics = new LinkedHashMap<>();
        for (Row r : rows) {
            if ("BASIC".equals(r.type()) && !basics.containsKey(r.description())) {
                basics.put(r.description(),
                        new BasicRow(r.description(), quantity(c, r.probabilityId()), r.elementId()));
            }
        }
        return new Saved(analysisId, harmId, topEvent, List.copyOf(basics.values()), toNode(top, children), date);
    }

    private static FtaModel.Node toNode(Row row, Map<Long, List<Row>> children) {
        if ("BASIC".equals(row.type())) {
            return new FtaModel.Ref(row.description());
        }
        List<FtaModel.Node> kids = new ArrayList<>();
        for (Row child : children.getOrDefault(row.id(), List.of())) {
            kids.add(toNode(child, children));
        }
        return new FtaModel.GateSpec(row.description(), Gate.valueOf(row.gate()), row.justification(), kids);
    }

    private static QuantityRow quantity(Connection c, Long id) throws SQLException {
        try (var ps = c.prepareStatement("select q.value, q.unit, q.range_min, q.range_max, q.source, e.code, q.author "
                + "from quantity q join evidence_level e on e.id = q.evidence_level_id where q.id = ?")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return new QuantityRow("P", 1, rs.getDouble(1), rs.getString(2), doubleOrNull(rs, 3),
                        doubleOrNull(rs, 4), rs.getString(5), rs.getString(6), rs.getString(7));
            }
        }
    }

    private static long insertQuantity(Connection c, QuantityRow q) throws SQLException {
        long evidenceId;
        try (var ps = c.prepareStatement("select id from evidence_level where code = ?")) {
            ps.setString(1, q.evidence());
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Nivel de evidencia desconocido: " + q.evidence());
                }
                evidenceId = rs.getLong(1);
            }
        }
        try (var ps = c.prepareStatement("insert into quantity(value, unit, range_min, range_max, source, "
                + "evidence_level_id, author, created_date, is_current) values (?, ?, ?, ?, ?, ?, ?, ?, 1)")) {
            ps.setDouble(1, q.value());
            ps.setString(2, q.unit());
            if (q.min() == null) {
                ps.setNull(3, Types.REAL);
                ps.setNull(4, Types.REAL);
            } else {
                ps.setDouble(3, q.min());
                ps.setDouble(4, q.max());
            }
            ps.setString(5, q.source());
            ps.setLong(6, evidenceId);
            ps.setString(7, q.author());
            ps.setString(8, Instant.now().toString());
            ps.executeUpdate();
        }
        return scalar(c, "select last_insert_rowid()");
    }

    private static long scalar(Connection c, String sql) throws SQLException {
        try (var st = c.createStatement(); var rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static void setLong(java.sql.PreparedStatement ps, int i, Long v) throws SQLException {
        if (v == null) {
            ps.setNull(i, Types.INTEGER);
        } else {
            ps.setLong(i, v);
        }
    }

    private static Long longOrNull(ResultSet rs, int i) throws SQLException {
        long v = rs.getLong(i);
        return rs.wasNull() ? null : v;
    }

    private static Double doubleOrNull(ResultSet rs, int i) throws SQLException {
        double v = rs.getDouble(i);
        return rs.wasNull() ? null : v;
    }
}
