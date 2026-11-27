package org.example.repository;

import org.hibernate.SessionFactory;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** Biblioteca de componentes (V6), de solo lectura. */
public class ComponentLibraryRepository {

    public record Entry(String code, String name, String definition, String parentCode) {}

    private final SessionFactory sf;

    public ComponentLibraryRepository(SessionFactory sf) {
        this.sf = sf;
    }

    public List<Entry> all() {
        return sf.fromTransaction(s -> s.doReturningWork(c -> {
            List<Entry> out = new ArrayList<>();
            try (var st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                         "select code, name, definition, parent_code from component_library order by name")) {
                while (rs.next()) {
                    out.add(new Entry(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
                }
            }
            return out;
        }));
    }
}
