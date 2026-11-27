package org.example.repository;

import org.hibernate.SessionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Requisitos y su trazabilidad hacia medidas de control, verificaciones, daños y componentes.
 * Usa SQL directo (igual que {@link AssessmentRepository}); cada método es una transacción.
 */
public class TraceabilityRepository {

    @FunctionalInterface
    private interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    public record RequirementRow(long id, long deviceId, String code, String description, String type,
                                 String source, String createdDate) {}

    /** Una fila de la matriz: requisito → medida de control → daño → peligro (+ pruebas de la medida). */
    public record TraceRow(long requirementId, String requirementCode, String requirementDescription,
                           long controlId, String controlDescription, long harmId, String harmDescription,
                           long hazardId, String hazardDescription, Long elementId, String elementName,
                           int verifications, int effectiveVerifications) {}

    public record ControlRef(long controlId, String description, String type, long harmId,
                             String harmDescription, int verifications, int effectiveVerifications,
                             int requirements) {}

    public record HarmRef(long harmId, String harmDescription, long hazardId, String hazardDescription) {}

    public record ElementRef(long id, String name) {}

    private static final String DEVICE_OF_HAZARD =
            "left join component dc on dc.id = hz.model_element_id "
                    + "left join function df on df.id = hz.model_element_id "
                    + "left join interaction di on di.id = hz.model_element_id "
                    + "left join device dd on dd.id = hz.model_element_id ";

    private static final String DEVICE_MATCH = "coalesce(dc.device_id, df.device_id, di.device_id, dd.id) = ? ";

    private static final String CHAIN_JOINS =
            "join harm hm on hm.id = m.harm_id "
                    + "join hazardous_situation hs on hs.id = hm.hazardous_situation_id "
                    + "join sequence_of_events s on s.id = hs.sequence_of_events_id "
                    + "join hazard hz on hz.id = s.hazard_id ";

    private final SessionFactory sf;

    public TraceabilityRepository(SessionFactory sf) {
        this.sf = sf;
    }

    private <T> T work(Work<T> w) {
        return sf.fromTransaction(s -> s.doReturningWork(w::run));
    }

    // ------------------------------------------------------------------ requisitos

    public long addRequirement(long deviceId, String code, String description, String type, String source) {
        return work(c -> {
            try (PreparedStatement ps = c.prepareStatement("insert into requirement(device_id, code, "
                    + "description, type, source, active, created_date) values (?, ?, ?, ?, ?, 1, ?)")) {
                ps.setLong(1, deviceId);
                ps.setString(2, code);
                ps.setString(3, description);
                ps.setString(4, type);
                ps.setString(5, source);
                ps.setString(6, Instant.now().toString());
                ps.executeUpdate();
            }
            return lastId(c);
        });
    }

    public Optional<RequirementRow> findRequirement(long id) {
        return work(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "select id, device_id, code, description, type, source, created_date "
                            + "from requirement where id = ? and active = 1")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(readRequirement(rs)) : Optional.<RequirementRow>empty();
                }
            }
        });
    }

    public List<RequirementRow> listRequirements(long deviceId) {
        return work(c -> {
            List<RequirementRow> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "select id, device_id, code, description, type, source, created_date "
                            + "from requirement where device_id = ? and active = 1 order by code, id")) {
                ps.setLong(1, deviceId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(readRequirement(rs));
                    }
                }
            }
            return out;
        });
    }

    public boolean codeExists(long deviceId, String code, Long excludingId) {
        return work(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "select count(*) from requirement where device_id = ? and active = 1 and code = ? "
                            + "and id <> ?")) {
                ps.setLong(1, deviceId);
                ps.setString(2, code);
                ps.setLong(3, excludingId == null ? -1L : excludingId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1) > 0;
                }
            }
        });
    }

    public void updateRequirement(long id, String code, String description, String type, String source) {
        work(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "update requirement set code = ?, description = ?, type = ?, source = ? where id = ?")) {
                ps.setString(1, code);
                ps.setString(2, description);
                ps.setString(3, type);
                ps.setString(4, source);
                ps.setLong(5, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    /** Baja lógica: el requisito deja de listarse y sale de la matriz; sus enlaces se conservan. */
    public void deactivateRequirement(long id) {
        work(c -> {
            try (PreparedStatement ps = c.prepareStatement("update requirement set active = 0 where id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    // ------------------------------------------------------------------ enlaces

    public boolean linkExists(long requirementId, long controlId) {
        return work(c -> {
            try (PreparedStatement ps = c.prepareStatement("select count(*) from requirement_control "
                    + "where requirement_id = ? and control_measure_id = ?")) {
                ps.setLong(1, requirementId);
                ps.setLong(2, controlId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1) > 0;
                }
            }
        });
    }

    public void link(long requirementId, long controlId) {
        work(c -> {
            try (PreparedStatement ps = c.prepareStatement("insert into requirement_control"
                    + "(requirement_id, control_measure_id) values (?, ?)")) {
                ps.setLong(1, requirementId);
                ps.setLong(2, controlId);
                ps.executeUpdate();
            }
            return null;
        });
    }

    /** @return true si el enlace existía y se quitó. */
    public boolean unlink(long requirementId, long controlId) {
        return work(c -> {
            try (PreparedStatement ps = c.prepareStatement("delete from requirement_control "
                    + "where requirement_id = ? and control_measure_id = ?")) {
                ps.setLong(1, requirementId);
                ps.setLong(2, controlId);
                return ps.executeUpdate() > 0;
            }
        });
    }

    /** Dispositivo al que pertenece una medida de control (vacío si la medida no existe). */
    public Optional<Long> deviceOfControl(long controlId) {
        return work(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "select coalesce(dc.device_id, df.device_id, di.device_id, dd.id) "
                            + "from risk_control_measure m " + CHAIN_JOINS + DEVICE_OF_HAZARD
                            + "where m.id = ?")) {
                ps.setLong(1, controlId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        long v = rs.getLong(1);
                        return rs.wasNull() ? Optional.<Long>empty() : Optional.of(v);
                    }
                    return Optional.<Long>empty();
                }
            }
        });
    }

    // ------------------------------------------------------------------ consultas

    /** Matriz: solo los requisitos vigentes que tienen al menos una medida de control enlazada. */
    public List<TraceRow> matrix(long deviceId) {
        return work(c -> {
            List<TraceRow> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "select r.id, r.code, r.description, m.id, m.description, hm.id, hm.description, "
                            + "hz.id, hz.description, hz.model_element_id, ec.name, "
                            + VERIFICATION_COUNT + ", " + VERIFICATION_EFFECTIVE + " "
                            + "from requirement r "
                            + "join requirement_control rc on rc.requirement_id = r.id "
                            + "join risk_control_measure m on m.id = rc.control_measure_id "
                            + CHAIN_JOINS
                            + "left join component ec on ec.id = hz.model_element_id "
                            + "where r.device_id = ? and r.active = 1 "
                            + "order by r.code, m.id")) {
                ps.setLong(1, deviceId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long el = rs.getLong(10);
                        Long elementId = rs.wasNull() ? null : el;
                        out.add(new TraceRow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4),
                                rs.getString(5), rs.getLong(6), rs.getString(7), rs.getLong(8),
                                rs.getString(9), elementId, rs.getString(11), rs.getInt(12), rs.getInt(13)));
                    }
                }
            }
            return out;
        });
    }

    /** Todas las medidas de control del dispositivo, con cuántos requisitos vigentes las cubren. */
    public List<ControlRef> controlsOfDevice(long deviceId) {
        return work(c -> {
            List<ControlRef> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "select m.id, m.description, m.type, hm.id, hm.description, "
                            + VERIFICATION_COUNT + ", " + VERIFICATION_EFFECTIVE + ", "
                            + "(select count(*) from requirement_control rc join requirement r "
                            + "on r.id = rc.requirement_id where rc.control_measure_id = m.id and r.active = 1) "
                            + "from risk_control_measure m " + CHAIN_JOINS + DEVICE_OF_HAZARD
                            + "where " + DEVICE_MATCH + "order by m.id")) {
                ps.setLong(1, deviceId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new ControlRef(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4),
                                rs.getString(5), rs.getInt(6), rs.getInt(7), rs.getInt(8)));
                    }
                }
            }
            return out;
        });
    }

    /**
     * Hacia atrás: componentes por los que pasó un daño. Primero el origen del peligro y después, en
     * orden, los componentes de cada paso de la secuencia (sin repetir). Solo cuenta componentes.
     */
    public List<ElementRef> componentsOfHarm(long harmId) {
        return work(c -> {
            Map<Long, ElementRef> out = new LinkedHashMap<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "select ids.id, k.name from ("
                            + "select hz.model_element_id as id, 0 as ord from harm hm "
                            + "join hazardous_situation hs on hs.id = hm.hazardous_situation_id "
                            + "join sequence_of_events s on s.id = hs.sequence_of_events_id "
                            + "join hazard hz on hz.id = s.hazard_id where hm.id = ? "
                            + "union all "
                            + "select s2.model_element_id, s2.step_order from harm hm "
                            + "join hazardous_situation hs on hs.id = hm.hazardous_situation_id "
                            + "join sequence_of_events s on s.id = hs.sequence_of_events_id "
                            + "join sequence_of_events s2 on s2.hazard_id = s.hazard_id where hm.id = ?"
                            + ") ids join component k on k.id = ids.id order by ids.ord, ids.id")) {
                ps.setLong(1, harmId);
                ps.setLong(2, harmId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.putIfAbsent(rs.getLong(1), new ElementRef(rs.getLong(1), rs.getString(2)));
                    }
                }
            }
            return new ArrayList<>(out.values());
        });
    }

    /** Hacia delante: daños a los que lleva un componente (como origen del peligro o en un paso). */
    public List<HarmRef> harmsOfComponent(long componentId) {
        return work(c -> {
            List<HarmRef> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "select distinct hm.id, hm.description, hz.id, hz.description "
                            + "from harm hm "
                            + "join hazardous_situation hs on hs.id = hm.hazardous_situation_id "
                            + "join sequence_of_events s on s.id = hs.sequence_of_events_id "
                            + "join hazard hz on hz.id = s.hazard_id "
                            + "where hz.model_element_id = ? or exists (select 1 from sequence_of_events s2 "
                            + "where s2.hazard_id = hz.id and s2.model_element_id = ?) order by hm.id")) {
                ps.setLong(1, componentId);
                ps.setLong(2, componentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new HarmRef(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4)));
                    }
                }
            }
            return out;
        });
    }

    // ------------------------------------------------------------------ utilidades

    private static final String VERIFICATION_COUNT =
            "(select count(*) from verification v where v.control_measure_id = m.id)";
    private static final String VERIFICATION_EFFECTIVE =
            "(select count(*) from verification v where v.control_measure_id = m.id "
                    + "and v.implemented = 1 and v.effective = 1)";

    private static RequirementRow readRequirement(ResultSet rs) throws SQLException {
        return new RequirementRow(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getString(7));
    }

    private static long lastId(Connection c) throws SQLException {
        try (var st = c.createStatement(); var rs = st.executeQuery("select last_insert_rowid()")) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
