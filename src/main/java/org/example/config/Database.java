package org.example.config;

import org.flywaydb.core.Flyway;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Database {

    private Database() {}

    /** Archivo de base de datos en la carpeta del usuario (~/.mbr/mbr.db). */
    public static Path defaultPath() {
        try {
            Path dir = Path.of(System.getProperty("user.home"), ".mbr");
            Files.createDirectories(dir);
            return dir.resolve("mbr.db");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String jdbcUrl(Path dbFile) {
        return "jdbc:sqlite:" + dbFile.toAbsolutePath() + "?foreign_keys=on";
    }

    /** Aplica las migraciones de src/main/resources/db/migration. */
    public static void migrate(String jdbcUrl) {
        Flyway.configure()
                .dataSource(jdbcUrl, null, null)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}