package ru.islab.labwork.service;

public final class ChangeEvent {

    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";
    public static final String RELOAD = "RELOAD";

    private final String action;
    private final Integer id;

    public ChangeEvent(String action, Integer id) {
        this.action = action;
        this.id = id;
    }

    public String action() {
        return action;
    }

    public Integer id() {
        return id;
    }
}
