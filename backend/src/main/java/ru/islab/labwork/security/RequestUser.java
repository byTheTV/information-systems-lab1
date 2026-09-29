package ru.islab.labwork.security;

import java.security.Principal;

public final class RequestUser implements Principal {

    private final long id;
    private final String username;
    private final String role;

    public RequestUser(long id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public long id() {
        return id;
    }

    @Override
    public String getName() {
        return username;
    }

    public String role() {
        return role;
    }
}
