package org.example.api;

import org.example.repository.DashboardRepository.RpnRow;
import org.example.service.RpnDashboardService;

import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {}

    public record RpnRowDto(Long analysisId, Long harmId, String harm, String hazard, String failureMode,
                            String component, int severity, int occurrence, int detection, int rpn,
                            String category, String isoRiskLevel, Boolean isoAcceptable) {
        public static RpnRowDto from(RpnRow r) {
            return new RpnRowDto(r.analysisId(), r.harmId(), r.harm(), r.hazard(), r.failureMode(),
                    r.component(), r.severity(), r.occurrence(), r.detection(), r.rpn(), r.category(),
                    r.isoRiskLevel(), r.isoAcceptable());
        }
    }

    public record CategoryCountDto(String name, int minRpn, int maxRpn, int count) {}

    public record DashboardDto(List<RpnRowDto> rows, List<CategoryCountDto> categories, int uncategorized,
                               Integer maxRpn, Double averageRpn) {
        public static DashboardDto from(RpnDashboardService.Dashboard d) {
            return new DashboardDto(d.rows().stream().map(RpnRowDto::from).toList(),
                    d.categories().stream()
                            .map(c -> new CategoryCountDto(c.name(), c.minRpn(), c.maxRpn(), c.count())).toList(),
                    d.uncategorized(), d.maxRpn(), d.averageRpn());
        }
    }
}
