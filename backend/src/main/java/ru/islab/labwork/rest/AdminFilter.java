package ru.islab.labwork.rest;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import ru.islab.labwork.error.ApiException;
import ru.islab.labwork.security.AdminOnly;
import ru.islab.labwork.security.Roles;

@Provider
@AdminOnly
@ApplicationScoped
@Priority(Priorities.AUTHORIZATION)
public class AdminFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext request) {
        if (request.getSecurityContext() == null || !request.getSecurityContext().isUserInRole(Roles.ADMIN)) {
            throw new ApiException(jakarta.ws.rs.core.Response.status(403)
                    .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
                    .entity(ru.islab.labwork.error.ErrorBody.message("Изменение доступно только администратору"))
                    .build());
        }
    }
}
