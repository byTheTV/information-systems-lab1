package ru.islab.labwork.ws;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.websocket.server.ServerEndpointConfig;

public class CdiEndpointConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) {
        return CDI.current().select(endpointClass).get();
    }
}
