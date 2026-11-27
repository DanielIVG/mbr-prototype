package org.example.repository;

import org.example.iso.RiskMatrix.Cell;
import org.example.iso.RiskMatrix.Criterion;
import org.example.iso.RiskMatrix.ProbabilityLevel;
import org.example.iso.RiskMatrix.SeverityLevel;
import org.hibernate.SessionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Criterios de aceptabilidad, estimaciones con procedencia, medidas de control y verificación.
 * Usa SQL directo sobre la conexión de la sesión (las tablas vienen de V1–V4) para no multiplicar
 * entidades; cada método es una transacción.
 */
public class AssessmentRepository {

    @FunctionalInterface
    private interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    public record CriteriaData(String exposureUnit, List<SeverityLevel> severities,
                               List<ProbabilityLevel> probabilities, List<Cell> cells,
                               List<Criterion> criteria, Map<String, String> justifications) {}

    public record QuantityRow(String role, int ordinal, double value, String unit, Double min, Double max,
                              String source, String evidence, String author) {}

    public record EstimationRow(long id, long harmId, String mode, int severity, String severityDescriptor,
                                Double p1, Double p2, double pTotal, String unit, int probabilityLevel,
                                String probabilityDescriptor, String riskLevel, boolean acceptable,
                                String justification, String date, List<QuantityRow> inputs,
                                Long ftaAnalysisId) {}

    public record EvidenceLevel(String code, String descriptor, int rank) {}

    public record ControlRow(long id, long harmId, String description, String type) {}

    public record VerificationRow(long id, long controlId, String method, boolean implemented,
                                  Boolean effective, String date, String evidence) {}

    private final SessionFactory sf;

    public AssessmentRepository(SessionFactory sf) {
        this.sf = sf;
    }

    private <T> T work(Work<T> w) {
        return sf.fromTransaction(s -> s.doReturningWork(w::run));
    }

    // ------------------------------------------------------------------ criterios

    public Optional<CriteriaData> loadCriteria() {
        return work(c -> {
            List<SeverityLevel> sev = new ArrayList<>();
            try (var st = c.createStatement(); var rs = st.executeQuery(
                    "select value, descriptor from severity_scale_level order by value")) {
                while (rs.next()) {
                    sev.add(new SeverityLevel(rs.getInt(1), rs.getString(2)));
                }
            }
            if (sev.isEmpty()) {
                return Optional.<CriteriaData>empty();
            }
            List<ProbabilityLevel> prob = new ArrayList<>();
            String unit = null;
            try (var st = c.createStatement(); var rs = st.executeQuery(
                    "select value, descriptor, min_probability, max_probability, exposure_unit "
                            + "from probability_scale_level order by value")) {
                while (rs.next()) {
                    prob.add(new ProbabilityLevel(rs.getInt(1), rs.getString(2), rs.getDouble(3), rs.getDouble(4)));
                    unit = rs.getString(5);
                }
            }
            List<Cell> cells = new ArrayList<>();
            try (var st = c.createStatement(); var rs = st.executeQuery(
                    "select sl.value, pl.value, m.risk_level from risk_matrix_cell m "
                            + "join severity_scale_level sl on sl.id = m.severity_level_id "
                            + "join probability_scale_level pl on pl.id = m.probability_level_id")) {
                while (rs.next()) {
                    cells.add(new Cell(rs.getInt(1), rs.getInt(2), rs.getString(3)));
                }
            }
            List<Criterion> criteria = new ArrayList<>();
            Map<String, String> just = new LinkedHashMap<>();
            try (var st = c.createStatement(); var rs = st.executeQuery(
                    "select risk_level, acceptable, justification from acceptability_criterion order by id")) {
                while (rs.next()) {
                    criteria.add(new Criterion(rs.getString(1), rs.getInt(2) == 1));
                    just.put(rs.getString(1), rs.getString(3));
                }
            }
            return Optional.of(new CriteriaData(unit, sev, prob, cells, criteria, just));
        });
    }

    public long estimationCount() {
        return work(c -> scalarLong(c, "select count(*) from risk_estimation"));
    }

    /** Reemplaza todo el conjunto de criterios (solo válido mientras no existan estimaciones). */
    public void replaceCriteria(CriteriaData d) {
        work(c -> {
            for (String table : List.of("risk_matrix_cell", "acceptability_criterion",
                    "severity_scale_level", "probability_scale_level")) {
                try (var st = c.createStatement()) {
                    st.executeUpdate("delete from " + table);
                }
            }
            Map<Integer, Long> sevIds = new LinkedHashMap<>();
            for (SeverityLevel s : d.severities()) {
                try (var ps = c.prepareStatement(
                        "insert into severity_scale_level(value, descriptor) values (?, ?)")) {
                    ps.setInt(1, s.value());
                    ps.setString(2, s.descriptor());
                    ps.executeUpdate();
                }
                sevIds.put(s.value(), lastId(c));
            }
            Map<Integer, Long> probIds = new LinkedHashMap<>();
            for (ProbabilityLevel p : d.probabilities()) {
                try (var ps = c.prepareStatement("insert into probability_scale_level"
                        + "(value, descriptor, min_probability, max_probability, exposure_unit) "
                        + "values (?, ?, ?, ?, ?)")) {
                    ps.setInt(1, p.value());
                    ps.setString(2, p.descriptor());
                    ps.setDouble(3, p.min());
                    ps.setDouble(4, p.max());
                    ps.setString(5, d.exposureUnit());
                    ps.executeUpdate();
                }
                probIds.put(p.value(), lastId(c));
            }
            for (Criterion k : d.criteria()) {
                try (var ps = c.prepareStatement(
                        "insert into acceptability_criterion(risk_level, acceptable, justification) values (?, ?, ?)")) {
                    ps.setString(1, k.riskLevel());
                    ps.setInt(2, k.acceptable() ? 1 : 0);
                    ps.setString(3, d.justifications().get(k.riskLevel()));
                    ps.executeUpdate();
                }
            }
            for (Cell cell : d.cells()) {
                try (var ps = c.prepareStatement("insert into risk_matrix_cell"
                        + "(severity_level_id, probability_level_id, risk_level) values (?, ?, ?)")) {
                    ps.setLong(1, sevIds.get(cell.severity()));
                    ps.setLong(2, probIds.get(cell.probability()));
                    ps.setString(3, cell.riskLevel());
                    ps.executeUpdate();
                }
            }
            return null;
        });
    }

    public List<EvidenceLevel> evidenceLevels() {
        return work(c -> {
            List<EvidenceLevel> out = new ArrayList<>();
            try (var st = c.createStatement(); var rs = st.executeQuery(
                    "select code, descriptor, rank from evidence_level order by rank")) {
                while (rs.next()) {
                    out.add(new EvidenceLevel(rs.getString(1), rs.getString(2), rs.getInt(3)));
                }
            }
            return out;
        });
    }

    // ------------------------------------------------------------------ estimaciones

    public boolean harmExists(long harmId) {
        return work(c -> scalarLong(c, "select count(*) from harm where id = " + harmId) > 0);
    }

    /**
     * Guarda una estimación con sus cantidades y su evaluación. Si ya había una, registra la
     * reevaluación (riesgo residual) enlazando antes y después.
     */
    public long saveEstimation(long harmId, int severity, String mode, Double p1, Double p2,
                               double pTotal, String unit, int probabilityLevel, String riskLevel,
                               String justification, String reason, List<QuantityRow> inputs,
                               Long ftaAnalysisId) {
        return work(c -> {
            long sevId = idByValue(c, "severity_scale_level", severity);
            long probId = idByValue(c, "probability_scale_level", probabilityLevel);
            long cellId = scalarLong(c, "select id from risk_matrix_cell where severity_level_id = "
                    + sevId + " and probability_level_id = " + probId);
            long criterionId;
            boolean acceptable;
            try (var ps = c.prepareStatement(
                    "select id, acceptable from acceptability_criterion where risk_level = ?")) {
                ps.setString(1, riskLevel);
                try (var rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("No hay criterio para el nivel " + riskLevel);
                    }
                    criterionId = rs.getLong(1);
                    acceptable = rs.getInt(2) == 1;
                }
            }
            Long prevEst = null;
            Long prevEval = null;
            try (var ps = c.prepareStatement("select re.id, ev.id from risk_estimation re "
                    + "join risk_evaluation ev on ev.risk_estimation_id = re.id "
                    + "where re.harm_id = ? order by re.id desc limit 1")) {
                ps.setLong(1, harmId);
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        prevEst = rs.getLong(1);
                        prevEval = rs.getLong(2);
                    }
                }
            }
            Map<String, Long> quantityIds = new LinkedHashMap<>();
            for (QuantityRow q : inputs) {
                quantityIds.put(q.role() + "#" + q.ordinal(), insertQuantity(c, q));
            }
            String now = Instant.now().toString();
            long estId;
            try (var ps = c.prepareStatement("insert into risk_estimation(harm_id, severity, "
                    + "probability_of_harm, date, severity_level_id, probability_level_id, p1_id, p2_id, "
                    + "p_total, exposure_unit, total_only_justification, estimation_mode, p1_value, p2_value, fta_analysis_id) "
                    + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setLong(1, harmId);
                ps.setInt(2, severity);
                ps.setInt(3, probabilityLevel);
                ps.setString(4, now);
                ps.setLong(5, sevId);
                ps.setLong(6, probId);
                setLongOrNull(ps, 7, "P1_P2".equals(mode) ? quantityIds.get("P1#1") : null);
                setLongOrNull(ps, 8, "P1_P2".equals(mode) ? quantityIds.get("P2#1") : null);
                ps.setDouble(9, pTotal);
                ps.setString(10, unit);
                ps.setString(11, "TOTAL_ONLY".equals(mode) ? justification : null);
                ps.setString(12, mode);
                setDoubleOrNull(ps, 13, p1);
                setDoubleOrNull(ps, 14, p2);
                setLongOrNull(ps, 15, ftaAnalysisId);
                ps.executeUpdate();
            }
            estId = lastId(c);
            for (QuantityRow q : inputs) {
                try (var ps = c.prepareStatement("insert into estimation_input"
                        + "(risk_estimation_id, role, ordinal, quantity_id) values (?, ?, ?, ?)")) {
                    ps.setLong(1, estId);
                    ps.setString(2, q.role());
                    ps.setInt(3, q.ordinal());
                    ps.setLong(4, quantityIds.get(q.role() + "#" + q.ordinal()));
                    ps.executeUpdate();
                }
            }
            long evalId;
            try (var ps = c.prepareStatement("insert into risk_evaluation(risk_estimation_id, matrix_cell_id, "
                    + "acceptability_criterion_id, risk_level, acceptable, date) values (?, ?, ?, ?, ?, ?)")) {
                ps.setLong(1, estId);
                ps.setLong(2, cellId);
                ps.setLong(3, criterionId);
                ps.setString(4, riskLevel);
                ps.setInt(5, acceptable ? 1 : 0);
                ps.setString(6, now);
                ps.executeUpdate();
            }
            evalId = lastId(c);
            if (prevEst != null) {
                try (var ps = c.prepareStatement("insert into reassessment(harm_id, date, trigger_reason, "
                        + "estimation_before_id, estimation_after_id, evaluation_before_id, evaluation_after_id) "
                        + "values (?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setLong(1, harmId);
                    ps.setString(2, now);
                    ps.setString(3, reason);
                    ps.setLong(4, prevEst);
                    ps.setLong(5, estId);
                    ps.setLong(6, prevEval);
                    ps.setLong(7, evalId);
                    ps.executeUpdate();
                }
            }
            return estId;
        });
    }

    public Optional<EstimationRow> latestEstimation(long harmId) {
        return work(c -> {
            List<EstimationRow> rows = estimations(c, harmId, true);
            return rows.isEmpty() ? Optional.<EstimationRow>empty() : Optional.of(rows.get(0));
        });
    }

    /** Historial de estimaciones del daño, de la más reciente a la más antigua (con entradas). */
    public List<EstimationRow> estimationHistory(long harmId) {
        return work(c -> estimations(c, harmId, false));
    }

    private static List<EstimationRow> estimations(Connection c, long harmId, boolean onlyLatest) throws SQLException {
        String sql = "select re.id, re.estimation_mode, re.severity, sl.descriptor, re.p1_value, re.p2_value, "
                + "re.p_total, re.exposure_unit, re.probability_of_harm, pl.descriptor, ev.risk_level, "
                + "ev.acceptable, re.total_only_justification, re.date, re.fta_analysis_id "
                + "from risk_estimation re "
                + "join risk_evaluation ev on ev.risk_estimation_id = re.id "
                + "join severity_scale_level sl on sl.id = re.severity_level_id "
                + "join probability_scale_level pl on pl.id = re.probability_level_id "
                + "where re.harm_id = ? order by re.id desc" + (onlyLatest ? " limit 1" : "");
        List<EstimationRow> out = new ArrayList<>();
        try (var ps = c.prepareStatement(sql)) {
            ps.setLong(1, harmId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong(1);
                    out.add(new EstimationRow(id, harmId, rs.getString(2), rs.getInt(3), rs.getString(4),
                            doubleOrNull(rs, 5), doubleOrNull(rs, 6), rs.getDouble(7), rs.getString(8),
                            rs.getInt(9), rs.getString(10), rs.getString(11), rs.getInt(12) == 1,
                            rs.getString(13), rs.getString(14), inputsOf(c, id), longOrNull(rs, 15)));
                }
            }
        }
        return out;
    }

    private static List<QuantityRow> inputsOf(Connection c, long estimationId) throws SQLException {
        List<QuantityRow> out = new ArrayList<>();
        try (var ps = c.prepareStatement("select i.role, i.ordinal, q.value, q.unit, q.range_min, q.range_max, "
                + "q.source, e.code, q.author from estimation_input i "
                + "join quantity q on q.id = i.quantity_id join evidence_level e on e.id = q.evidence_level_id "
                + "where i.risk_estimation_id = ? order by i.id")) {
            ps.setLong(1, estimationId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new QuantityRow(rs.getString(1), rs.getInt(2), rs.getDouble(3), rs.getString(4),
                            doubleOrNull(rs, 5), doubleOrNull(rs, 6), rs.getString(7), rs.getString(8),
                            rs.getString(9)));
                }
            }
        }
        return out;
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
            setDoubleOrNull(ps, 3, q.min());
            setDoubleOrNull(ps, 4, q.max());
            ps.setString(5, q.source());
            ps.setLong(6, evidenceId);
            ps.setString(7, q.author());
            ps.setString(8, Instant.now().toString());
            ps.executeUpdate();
        }
        return lastId(c);
    }

    // ------------------------------------------------------------------ control y verificación

    public long addControl(long harmId, String description, String type) {
        return work(c -> {
            try (var ps = c.prepareStatement(
                    "insert into risk_control_measure(harm_id, description, type) values (?, ?, ?)")) {
                ps.setLong(1, harmId);
                ps.setString(2, description);
                ps.setString(3, type);
                ps.executeUpdate();
            }
            return lastId(c);
        });
    }

    public List<ControlRow> controls(long harmId) {
        return work(c -> {
            List<ControlRow> out = new ArrayList<>();
            try (var ps = c.prepareStatement(
                    "select id, harm_id, description, type from risk_control_measure where harm_id = ? order by id")) {
                ps.setLong(1, harmId);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new ControlRow(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4)));
                    }
                }
            }
            return out;
        });
    }

    public boolean controlExists(long controlId) {
        return work(c -> scalarLong(c, "select count(*) from risk_control_measure where id = " + controlId) > 0);
    }

    public long addVerification(long controlId, String method, boolean implemented, Boolean effective,
                                String evidence) {
        return work(c -> {
            try (var ps = c.prepareStatement("insert into verification(control_measure_id, method, implemented, "
                    + "effective, date, evidence) values (?, ?, ?, ?, ?, ?)")) {
                ps.setLong(1, controlId);
                ps.setString(2, method);
                ps.setInt(3, implemented ? 1 : 0);
                if (effective == null) {
                    ps.setNull(4, Types.INTEGER);
                } else {
                    ps.setInt(4, effective ? 1 : 0);
                }
                ps.setString(5, Instant.now().toString());
                ps.setString(6, evidence);
                ps.executeUpdate();
            }
            return lastId(c);
        });
    }

    public List<VerificationRow> verifications(long controlId) {
        return work(c -> {
            List<VerificationRow> out = new ArrayList<>();
            try (var ps = c.prepareStatement("select id, control_measure_id, method, implemented, effective, "
                    + "date, evidence from verification where control_measure_id = ? order by id")) {
                ps.setLong(1, controlId);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int eff = rs.getInt(5);
                        Boolean effective = rs.wasNull() ? null : eff == 1;
                        out.add(new VerificationRow(rs.getLong(1), rs.getLong(2), rs.getString(3),
                                rs.getInt(4) == 1, effective, rs.getString(6), rs.getString(7)));
                    }
                }
            }
            return out;
        });
    }

    // ------------------------------------------------------------------ utilidades

    private static long lastId(Connection c) throws SQLException {
        return scalarLong(c, "select last_insert_rowid()");
    }

    private static long scalarLong(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (!rs.next()) {
                throw new IllegalStateException("La consulta no devolvió filas: " + sql);
            }
            return rs.getLong(1);
        }
    }

    private static long idByValue(Connection c, String table, int value) throws SQLException {
        try (var ps = c.prepareStatement("select id from " + table + " where value = ?")) {
            ps.setInt(1, value);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Nivel " + value + " inexistente en " + table);
                }
                return rs.getLong(1);
            }
        }
    }

    private static void setLongOrNull(PreparedStatement ps, int i, Long v) throws SQLException {
        if (v == null) {
            ps.setNull(i, Types.INTEGER);
        } else {
            ps.setLong(i, v);
        }
    }

    private static void setDoubleOrNull(PreparedStatement ps, int i, Double v) throws SQLException {
        if (v == null) {
            ps.setNull(i, Types.REAL);
        } else {
            ps.setDouble(i, v);
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
