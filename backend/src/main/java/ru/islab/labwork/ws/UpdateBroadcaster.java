package ru.islab.labwork.ws;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import ru.islab.labwork.service.ChangeEvent;

@ApplicationScoped
public class UpdateBroadcaster {

    @Inject
    UpdateHub hub;

    public void onChange(@Observes(during = TransactionPhase.AFTER_SUCCESS) ChangeEvent event) {
        String id = event.id() == null ? "null" : event.id().toString();
        hub.broadcast("{\"type\":\"LABWORKS_CHANGED\",\"action\":\"" + event.action() + "\",\"id\":" + id + "}");
    }
}
