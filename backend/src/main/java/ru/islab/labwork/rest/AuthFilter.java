package ru.islab.labwork.rest;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import ru.islab.labwork.error.ErrorBody;
import ru.islab.labwork.security.RequestUser;
import ru.islab.labwork.security.TokenSecurityContext;
import ru.islab.labwork.service.AuthService;

@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    @Inject
    AuthService authService;

    @Override
    public void filter(ContainerRequestContext request) {
        if (isPublic(request)) {
            return;
        }
        String header = request.getHeaderString(HttpHeaders.AUTHORIZATION);
        String token = null;
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = header.substring(7).trim();
        }
        RequestUser user = authService.authenticate(token);
        if (user == null) {
            request.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(ErrorBody.message("Требуется авторизация"))
                    .build());
            return;
        }
        request.setSecurityContext(new TokenSecurityContext(user));
    }

    private static boolean isPublic(ContainerRequestContext request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getUriInfo().getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path.equals("health") || path.equals("auth/login") || path.equals("auth/register");
    }
}
