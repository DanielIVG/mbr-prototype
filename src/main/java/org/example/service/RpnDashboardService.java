package org.example.service;

import org.example.repository.DashboardRepository;
import org.example.repository.DashboardRepository.CategoryRow;
import org.example.repository.DashboardRepository.RpnRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumen de RPN de un dispositivo para el dashboard. El RPN solo prioriza modos de fallo (FMEA):
 * la aceptabilidad del riesgo la decide la evaluación ISO 14971, que se muestra aparte.
 */
public class RpnDashboardService {

    public record CategoryCount(String name, int minRpn, int maxRpn, int count) {}

    public record Dashboard(List<RpnRow> rows, List<CategoryCount> categories, int uncategorized,
                            Integer maxRpn, Double averageRpn) {}

    private final DashboardRepository repository;
    private final DeviceService devices;

    public RpnDashboardService(DashboardRepository repository, DeviceService devices) {
        this.repository = repository;
        this.devices = devices;
    }

    public Dashboard dashboard(Long deviceId) {
        Validation.requireDevice(devices, deviceId);
        List<RpnRow> rows = repository.rows(deviceId);
        List<CategoryCount> counts = new ArrayList<>();
        for (CategoryRow c : repository.categories()) {
            int n = (int) rows.stream().filter(r -> c.name().equals(r.category())).count();
            counts.add(new CategoryCount(c.name(), c.minRpn(), c.maxRpn(), n));
        }
        int uncategorized = (int) rows.stream().filter(r -> r.category() == null).count();
        Integer max = rows.isEmpty() ? null : rows.get(0).rpn();   // ya vienen ordenadas por RPN descendente
        Double average = rows.isEmpty() ? null
                : Math.round(rows.stream().mapToInt(RpnRow::rpn).average().orElse(0) * 10.0) / 10.0;
        return new Dashboard(rows, counts, uncategorized, max, average);
    }
}
