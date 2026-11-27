package org.example.integration;

import org.example.api.ApiServer;
import org.example.api.ComponentDto;
import org.example.client.ApiClient;
import org.example.client.ApiException;
import org.example.config.Database;
import org.example.config.HibernateConfig;
import org.example.config.Services;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/** Biblioteca de componentes importada de FDA CDRH / NCIt (IMDRF Anexo G). */
class BibliotecaComponentesIntegracionTest {

    @TempDir
    Path tmp;

    SessionFactory sf;
    ApiServer server;
    ApiClient client;

    @BeforeEach
    void up() {
        String url = Database.jdbcUrl(tmp.resolve("biblioteca.db"));
        Database.migrate(url);
        sf = HibernateConfig.build(url);
        server = new ApiServer(Services.create(sf)).start();
        client = new ApiClient("http://127.0.0.1:" + server.port());
    }

    @AfterEach
    void down() {
        server.stop();
        sf.close();
    }

    @Test
    void laBibliotecaTiene299TerminosConIdUnicoYJerarquiaDeDosNiveles() {
        var todos = client.listComponentLibrary();
        assertEquals(299, todos.size());
        assertEquals(299, new HashSet<>(todos.stream().map(e -> e.code()).toList()).size());
        assertEquals(74, todos.stream().filter(e -> e.parentCode() != null).count());
        var cable = todos.stream().filter(e -> e.code().equals("G0200401")).findFirst().orElseThrow();
        assertEquals("Cable Grip/Tie", cable.name());
        assertEquals("G02004", cable.parentCode());
    }

    @Test
    void laBusquedaIgnoraMayusculasYToleraErrores() {
        var exacto = client.searchComponentLibrary("cable grip/tie", 5);
        assertEquals("G0200401", exacto.get(0).entry().code());
        assertEquals(1.0, exacto.get(0).score());

        var typo = client.searchComponentLibrary("compresor", 5);
        assertEquals("Compressor", typo.get(0).entry().name());
        assertTrue(typo.get(0).score() < 1.0);
    }

    @Test
    void busquedaSinTextoSeRechaza() {
        assertEquals(400, assertThrows(ApiException.class, () -> client.searchComponentLibrary(" ", 5)).status());
    }

    // ------------------------------------------------------------------ los componentes salen de la biblioteca

    private ComponentDto nuevo(long device, Long parent, String name) {
        return client.createComponent(new ComponentDto(null, device, parent, name, null, false, null, true));
    }

    @Test
    void unComponenteFueraDeLaBibliotecaSeRechaza() {
        long device = client.createDevice("Bomba", "Infundir", null).id();
        var e = assertThrows(ApiException.class, () -> nuevo(device, null, "Componente inventado"));
        assertEquals(400, e.status());
        assertTrue(e.getMessage().contains("biblioteca"));
        assertEquals(0, client.listComponents(device).size());
    }

    @Test
    void elNombreSeTomaDeLaBaseDeDatosAunqueElUsuarioEscribaOtraGrafia() {
        long device = client.createDevice("Bomba", "Infundir", null).id();
        assertEquals("Pump", nuevo(device, null, "  pump ").name());
        assertEquals("Motor(s)", nuevo(device, null, "MOTOR").name());
        assertEquals("Alarm, Audible", nuevo(device, null, "alarm audible").name());
    }

    @Test
    void renombrarUnComponenteTambienExigeLaBiblioteca() {
        long device = client.createDevice("Bomba", "Infundir", null).id();
        var c = nuevo(device, null, "Pump");
        var igual = new ComponentDto(c.id(), device, null, "Pump", "con descripción", false, null, true);
        assertEquals("con descripción", client.updateComponent(c.id(), igual).description());

        var otro = new ComponentDto(c.id(), device, null, "xyz", null, false, null, true);
        assertEquals(400, assertThrows(ApiException.class, () -> client.updateComponent(c.id(), otro)).status());

        var valido = new ComponentDto(c.id(), device, null, "battery", null, false, null, true);
        assertEquals("Battery", client.updateComponent(c.id(), valido).name());
    }
}
