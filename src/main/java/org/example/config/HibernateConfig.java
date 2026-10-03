package org.example.config;

import org.example.model.Analysis;
import org.example.model.Component;
import org.example.model.Device;
import org.example.model.DeviceFunction;
import org.example.model.FailureMode;
import org.example.model.FmeaAnalysis;
import org.example.model.FmeaScaleLevel;
import org.example.model.Harm;
import org.example.model.Hazard;
import org.example.model.HazardousSituation;
import org.example.model.Interaction;
import org.example.model.ModelElement;
import org.example.model.RpnFactor;
import org.example.model.RpnPriorityCategory;
import org.example.model.SequenceOfEvents;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class HibernateConfig {

    private HibernateConfig() {}

    public static SessionFactory build(String jdbcUrl) {
        return new Configuration()
                .setProperty("hibernate.connection.url", jdbcUrl)
                .setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC")
                .setProperty("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect")
                .setProperty("hibernate.connection.pool_size", "1")
                .setProperty("hibernate.hbm2ddl.auto", "none")
                .setProperty("hibernate.show_sql", "true")
                .setProperty("hibernate.query.mutation_strategy.persistent.create_tables", "false")
                .setProperty("hibernate.query.mutation_strategy.persistent.drop_tables", "false")
                .addAnnotatedClass(ModelElement.class)
                .addAnnotatedClass(Device.class)
                .addAnnotatedClass(DeviceFunction.class)
                .addAnnotatedClass(Component.class)
                .addAnnotatedClass(Interaction.class)
                .addAnnotatedClass(Hazard.class)
                .addAnnotatedClass(SequenceOfEvents.class)
                .addAnnotatedClass(HazardousSituation.class)
                .addAnnotatedClass(Harm.class)
                .addAnnotatedClass(FailureMode.class)
                .addAnnotatedClass(Analysis.class)
                .addAnnotatedClass(FmeaAnalysis.class)
                .addAnnotatedClass(FmeaScaleLevel.class)
                .addAnnotatedClass(RpnPriorityCategory.class)
                .addAnnotatedClass(RpnFactor.class)
                .buildSessionFactory();
    }
}