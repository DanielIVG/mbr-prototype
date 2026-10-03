package org.example.repository;

import org.example.model.Device;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class DeviceRepository {

    private final SessionFactory sf;

    public DeviceRepository(SessionFactory sf) {
        this.sf = sf;
    }

    public Device save(Device device) {
        return sf.fromTransaction(s -> {
            s.persist(device);
            return device;
        });
    }

    public Device update(Device device) {
        return sf.fromTransaction(s -> s.merge(device));
    }

    public Optional<Device> findById(Long id) {
        return sf.fromTransaction(s -> Optional.ofNullable(s.find(Device.class, id)));
    }

    public List<Device> findAllActive() {
        return sf.fromTransaction(s -> s.createSelectionQuery(
                        "from Device d where d.active = true order by d.name", Device.class)
                .getResultList());
    }
}