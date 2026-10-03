package org.example.config;

import org.example.model.Component;
import org.example.model.DeviceFunction;
import org.example.model.Interaction;
import org.example.repository.DeviceRepository;
import org.example.repository.FmeaRepository;
import org.example.repository.ModelElementRepository;
import org.example.repository.ModelLinksRepository;
import org.example.repository.RiskChainRepository;
import org.example.service.ComponentService;
import org.example.service.DeviceFunctionService;
import org.example.service.DeviceService;
import org.example.service.FmeaConfigService;
import org.example.service.FmeaService;
import org.example.service.InteractionService;
import org.example.service.RiskChainService;
import org.hibernate.SessionFactory;

public record Services(DeviceService devices, ComponentService components,
                       DeviceFunctionService functions, InteractionService interactions,
                       RiskChainService chains, FmeaConfigService fmeaConfig,
                       FmeaService fmea) {

    public static Services create(SessionFactory sf) {
        var links = new ModelLinksRepository(sf);
        var devices = new DeviceService(new DeviceRepository(sf));
        var components = new ComponentService(
                new ModelElementRepository<>(sf, Component.class), links, devices);
        var functions = new DeviceFunctionService(
                new ModelElementRepository<>(sf, DeviceFunction.class), links, devices, components);
        var interactions = new InteractionService(
                new ModelElementRepository<>(sf, Interaction.class), links, devices, components);
        var chains = new RiskChainService(new RiskChainRepository(sf));
        var fmeaRepository = new FmeaRepository(sf);
        var fmeaConfig = new FmeaConfigService(fmeaRepository);
        var fmea = new FmeaService(fmeaRepository, fmeaConfig);
        return new Services(devices, components, functions, interactions, chains, fmeaConfig, fmea);
    }
}