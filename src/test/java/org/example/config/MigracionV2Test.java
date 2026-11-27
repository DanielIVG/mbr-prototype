package org.example.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class MigracionV2Test {

    @TempDir
    Path tmp;

    private Connection abrir() throws SQLException {
        String url = Database.jdbcUrl(tmp.resolve("v2.db"));
        Database.migrate(url);
        Connection c = DriverManager.getConnection(url);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
        }
        return c;
    }

    private static final String INSERT_QUANTITY =
            "INSERT INTO quantity(value,unit,source,evidence_level_id,author,created_date) VALUES ";

    @Test
    void sembraLosCincoNivelesDeEvidencia() throws SQLException {
        try (Connection c = abrir(); Statement s = c.createStatement();
             var rs = s.executeQuery("SELECT COUNT(*) FROM evidence_level")) {
            assertTrue(rs.next());
            assertEquals(5, rs.getInt(1));
        }
    }

    @Test
    void rechazaCantidadSinFuente() throws SQLException {
        try (Connection c = abrir(); Statement s = c.createStatement()) {
            assertThrows(SQLException.class, () ->
                    s.execute(INSERT_QUANTITY + "(1e-6,'1/h','  ',1,'Daniel','2026-11-18')"));
        }
    }

    @Test
    void rechazaCantidadSinNivelDeEvidencia() throws SQLException {
        try (Connection c = abrir(); Statement s = c.createStatement()) {
            assertThrows(SQLException.class, () ->
                    s.execute(INSERT_QUANTITY + "(1e-6,'1/h','datasheet',NULL,'Daniel','2026-11-18')"));
            assertThrows(SQLException.class, () ->
                    s.execute(INSERT_QUANTITY + "(1e-6,'1/h','datasheet',999,'Daniel','2026-11-18')"));
        }
    }

    @Test
    void rechazaRangoInvertido() throws SQLException {
        try (Connection c = abrir(); Statement s = c.createStatement()) {
            assertThrows(SQLException.class, () -> s.execute(
                    "INSERT INTO quantity(value,unit,range_min,range_max,source,evidence_level_id,author,created_date) "
                            + "VALUES (0.2,'adimensional',0.5,0.05,'juicio',2,'Daniel','2026-11-18')"));
        }
    }

    @Test
    void unaCantidadSoloPuedeSerSustituidaUnaVez() throws SQLException {
        try (Connection c = abrir(); Statement s = c.createStatement()) {
            s.execute(INSERT_QUANTITY + "(1e-6,'1/h','datasheet',4,'Daniel','2026-11-18')");
            s.execute("INSERT INTO quantity(value,unit,source,evidence_level_id,author,created_date,supersedes_id) "
                    + "VALUES (2e-6,'1/h','ensayo',5,'Daniel','2026-11-19',1)");
            assertThrows(SQLException.class, () -> s.execute(
                    "INSERT INTO quantity(value,unit,source,evidence_level_id,author,created_date,supersedes_id) "
                            + "VALUES (3e-6,'1/h','otro',5,'Daniel','2026-11-20',1)"));
        }
    }

    @Test
    void rechazaTipoDeNodoInvalido() throws SQLException {
        try (Connection c = abrir(); Statement s = c.createStatement()) {
            assertThrows(SQLException.class, () -> s.execute(
                    "INSERT INTO fta_event(fta_analysis_id,description,event_type) VALUES (1,'x','RARO')"));
        }
    }
}
