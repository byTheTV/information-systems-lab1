package ru.islab.labwork.ws;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import ru.islab.labwork.security.RequestUser;
import ru.islab.labwork.service.AuthService;

@ApplicationScoped
@ServerEndpoint(value = "/ws/updates", configurator = CdiEndpointConfigurator.class)
public class UpdateEndpoint {

    @Inject
    AuthService authService;

    @Inject
    UpdateHub hub;

    @OnOpen
    public void onOpen(Session session) throws IOException {
        RequestUser user = authService.authenticate(token(session));
        if (user == null) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "unauthorized"));
            return;
        }
        session.setMaxIdleTimeout(0);
        hub.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        hub.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        hub.remove(session);
    }

    @OnMessage
    public void onMessage(String message) {
    }

    private static String token(Session session) {
        String query = session.getRequestURI() == null ? null : session.getRequestURI().getQuery();
        if (query == null) {
            return null;
        }
        for (String part : query.split("&")) {
            int separator = part.indexOf('=');
            if (separator > 0 && "token".equals(part.substring(0, separator))) {
                return URLDecoder.decode(part.substring(separator + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
