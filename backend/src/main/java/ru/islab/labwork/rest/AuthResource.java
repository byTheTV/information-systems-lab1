package ru.islab.labwork.rest;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import ru.islab.labwork.dto.AuthResponse;
import ru.islab.labwork.dto.Credentials;
import ru.islab.labwork.security.RequestUser;
import ru.islab.labwork.service.AuthService;

@Path("/auth")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public AuthResponse register(Credentials credentials) {
        return authService.register(credentials);
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public AuthResponse login(Credentials credentials) {
        return authService.login(credentials);
    }

    @POST
    @Path("/logout")
    public Response logout(@Context HttpHeaders headers) {
        authService.logout(bearer(headers));
        return Response.noContent().build();
    }

    @GET
    @Path("/me")
    public AuthResponse me(@Context SecurityContext securityContext) {
        RequestUser user = (RequestUser) securityContext.getUserPrincipal();
        AuthResponse response = new AuthResponse();
        response.username = user.getName();
        response.role = user.role();
        return response;
    }

    private static String bearer(HttpHeaders headers) {
        String header = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return header.substring(7).trim();
        }
        return null;
    }
}
