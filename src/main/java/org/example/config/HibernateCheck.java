// HibernateCheck.java
package org.example.config;

import org.example.model.Device;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public class HibernateCheck {
    public static void main(String[] args) {
        String url = Database.jdbcUrl(Database.defaultPath());
        Database.migrate(url);

        try (SessionFactory sf = HibernateConfig.build(url);
             Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();

            Device d = new Device();
            d.setName("Dispositivo de prueba");
            d.setIntendedUse("Prueba de mapeo");
            s.persist(d);
            s.flush();

            Long total = s.createSelectionQuery(
                    "select count(d) from Device d", Long.class).getSingleResult();
            System.out.println("Id: " + d.getId() + " | Devices: " + total);

            tx.rollback(); // no deja datos de prueba en la base
        }
    }
}