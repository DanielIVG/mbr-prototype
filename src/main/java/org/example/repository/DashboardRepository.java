package org.example.repository;

import org.hibernate.SessionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Consultas de solo lectura para el dashboard de RPN de un dispositivo. */
public class DashboardRepository {

    @FunctionalInterface
    private interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    /** Un análisis FMEA con su RPN vigente y, si existe, el veredicto ISO 14971 del daño. */
    public record RpnRow(long analysisId, long harmId, String harm, String hazard, String failureMode,
                         String component, int severity, int occurrence, int detection, int rpn,
                         String category, String isoRiskLevel, Boolean isoAcceptable) {}

    public record CategoryRow(String name, int minRpn, int maxRpn) {}

    private final SessionFactory sf;

    public DashboardRepository(SessionFactory sf) {
        this.sf = sf;
    }

    private <T> T work(Work<T> w) {
        return sf.fromTransaction(s -> s.doReturningWork(w::run));
    }

    public List<RpnRow> rows(long deviceId) {
        return work(c -> {
            List<RpnRow> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "select fa.id, hm.id, hm.description, hz.description, fm.description, kc.name, "
                            + "rf.severity, rf.occurrence, rf.detection, rf.rpn, pc.name, "
                            + "(select ev.risk_level from risk_estimation re join risk_evaluation ev "
                            + "on ev.risk_estimation_id = re.id where re.harm_id = hm.id "
                            + "order by re.id desc limit 1), "
                            + "(select ev.acceptable from risk_estimation re join risk_evaluation ev "
                            + "on ev.risk_estimation_id = re.id where re.harm_id = hm.id "
                            + "order by re.id desc limit 1) "
                            + "from fmea_analysis fa "
                            + "join analysis an on an.id = fa.id and an.active = 1 "
                            + "join harm hm on hm.id = an.harm_id "
                            + "join hazardous_situation hs on hs.id = hm.hazardous_situation_id "
                            + "join sequence_of_events s on s.id = hs.sequence_of_events_id "
                            + "join hazard hz on hz.id = s.hazard_id "
                            + "join failure_mode fm on fm.id = fa.failure_mode_id "
                            + "join rpn_factor rf on rf.fmea_analysis_id = fa.id and rf.is_current = 1 "
                            + "left join rpn_priority_category pc on pc.id = rf.priority_category_id "
                            + "left join component kc on kc.id = fm.model_element_id "
                            + "left join component dc on dc.id = hz.model_element_id "
                            + "left join function df on df.id = hz.model_element_id "
                            + "left join interaction di on di.id = hz.model_element_id "
                            + "left join device dd on dd.id = hz.model_element_id "
                            + "where coalesce(dc.device_id, df.device_id, di.device_id, dd.id) = ? "
                            + "order by rf.rpn desc, fa.id")) {
                ps.setLong(1, deviceId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int acc = rs.getInt(13);
                        Boolean acceptable = rs.wasNull() ? null : acc == 1;
                        out.add(new RpnRow(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                                rs.getString(5), rs.getString(6), rs.getInt(7), rs.getInt(8), rs.getInt(9),
                                rs.getInt(10), rs.getString(11), rs.getString(12), acceptable));
                    }
                }
            }
            return out;
        });
    }

    public List<CategoryRow> categories() {
        return work(c -> {
            List<CategoryRow> out = new ArrayList<>();
            try (var st = c.createStatement(); var rs = st.executeQuery(
                    "select name, min_rpn, max_rpn from rpn_priority_category order by min_rpn")) {
                while (rs.next()) {
                    out.add(new CategoryRow(rs.getString(1), rs.getInt(2), rs.getInt(3)));
                }
            }
            return out;
        });
    }
}
