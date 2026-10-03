package org.example.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbCheck {
    public static void main(String[] args) throws Exception {
        String url = Database.jdbcUrl(Database.defaultPath());
        Database.migrate(url);

        try (Connection c = DriverManager.getConnection(url);
             Statement s = c.createStatement()) {
            ResultSet r = s.executeQuery(
                    "SELECT count(*) FROM sqlite_master WHERE type='table'");
            System.out.println("Tablas: " + r.getInt(1));
            r = s.executeQuery("PRAGMA foreign_keys");
            System.out.println("foreign_keys: " + r.getInt(1));
        }
    }
}