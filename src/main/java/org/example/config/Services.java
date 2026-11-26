package org.example.config;

import org.example.model.Component;
import org.example.model.DeviceFunction;
import org.example.model.Interaction;
import org.example.repository.AssessmentRepository;
import org.example.repository.BehaviorRepository;
import org.example.repository.ComponentLibraryRepository;
import org.example.repository.DashboardRepository;
import org.example.repository.DeviceRepository;
import org.example.repository.FtaRepository;
import org.example.repository.FmeaRepository;
import org.example.repository.ModelElementRepository;
import org.example.repository.ModelLinksRepository;
import org.example.repository.RiskChainRepository;
import org.example.repository.TraceabilityRepository;
import org.example.service.AssessmentService;
import org.example.service.BehaviorService;
import org.example.service.ComponentLibraryService;
import org.example.service.ComponentService;
import org.example.service.DeviceFunctionService;
import org.example.service.DeviceService;
import org.example.service.FmeaConfigService;
import org.example.service.FmeaService;
import org.example.service.FtaService;
import org.example.service.InteractionService;
import org.example.service.RiskChainService;
import org.example.service.RpnDashboardService;
import org.example.service.TraceabilityService;
import org.hibernate.SessionFactory;

public record Services(DeviceService devices, ComponentService components,
                       DeviceFunctionService functions, InteractionService interactions,
                       RiskChainService chains, FmeaConfigService fmeaConfig,
                       FmeaService fmea, AssessmentService assessment, FtaService fta,
                       BehaviorService behavior, ComponentLibraryService library,
                       TraceabilityService traceability, RpnDashboardService dashboard) {

    public static Services create(SessionFactory sf) {
        var links = new ModelLinksRepository(sf);
        var devices = new DeviceService(new DeviceRepository(sf));
        var library = new ComponentLibraryService(new ComponentLibraryRepository(sf));
        var components = new ComponentService(
                new ModelElementRepository<>(sf, Component.class), links, devices, library);
        var functions = new DeviceFunctionService(
                new ModelElementRepository<>(sf, DeviceFunction.class), links, devices, components);
        var interactions = new InteractionService(
                new ModelElementRepository<>(sf, Interaction.class), links, devices, components);
        var chains = new RiskChainService(new RiskChainRepository(sf));
        var fmeaRepository = new FmeaRepository(sf);
        var fmeaConfig = new FmeaConfigService(fmeaRepository);
        var fmea = new FmeaService(fmeaRepository, fmeaConfig);
        var fta = new FtaService(new FtaRepository(sf));
        var assessment = new AssessmentService(new AssessmentRepository(sf), fta);
        return new Services(devices, components, functions, interactions, chains, fmeaConfig, fmea,
                assessment, fta, new BehaviorService(new BehaviorRepository(sf)),
                library, new TraceabilityService(new TraceabilityRepository(sf), devices),
                new RpnDashboardService(new DashboardRepository(sf), devices));
    }
}