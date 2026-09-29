package ru.islab.labwork.ws;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@ApplicationScoped
public class UpdateHub {

    private final Set<Session> sessions = new CopyOnWriteArraySet<>();

    public void add(Session session) {
        sessions.add(session);
    }

    public void remove(Session session) {
        sessions.remove(session);
    }

    public void broadcast(String json) {
        for (Session session : sessions) {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(json);
            }
        }
    }
}
