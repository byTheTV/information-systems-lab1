package ru.islab.labwork.rest;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import ru.islab.labwork.dto.HealthDto;
import ru.islab.labwork.service.AuthService;

@Path("/health")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    @Inject
    AuthService authService;

    @GET
    public HealthDto health() {
        authService.ping();
        HealthDto dto = new HealthDto();
        dto.status = "up";
        return dto;
    }
}
