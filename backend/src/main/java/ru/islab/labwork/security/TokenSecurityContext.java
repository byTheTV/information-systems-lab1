package ru.islab.labwork.security;

import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;

public final class TokenSecurityContext implements SecurityContext {

    private final RequestUser user;

    public TokenSecurityContext(RequestUser user) {
        this.user = user;
    }

    @Override
    public Principal getUserPrincipal() {
        return user;
    }

    @Override
    public boolean isUserInRole(String role) {
        return user != null && user.role().equals(role);
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getAuthenticationScheme() {
        return "Bearer";
    }
}
