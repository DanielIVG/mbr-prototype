package org.example.config;

import org.example.api.ApiServer;
import org.hibernate.SessionFactory;

import java.nio.file.Path;

public final class AppContext implements AutoCloseable {

    private final SessionFactory sessionFactory;
    private final ApiServer api;

    private AppContext(SessionFactory sessionFactory, ApiServer api) {
        this.sessionFactory = sessionFactory;
        this.api = api;
    }

    public static AppContext start(Path dbFile) {
        String url = Database.jdbcUrl(dbFile);
        Database.migrate(url);
        SessionFactory sf = HibernateConfig.build(url);
        try {
            ApiServer api = new ApiServer(Services.create(sf)).start();
            return new AppContext(sf, api);
        } catch (RuntimeException e) {
            sf.close();
            throw e;
        }
    }

    public String apiBaseUrl() {
        return "http://127.0.0.1:" + api.port();
    }

    @Override
    public void close() {
        api.stop();
        sessionFactory.close();
    }
}