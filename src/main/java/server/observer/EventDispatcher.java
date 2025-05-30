package observer;

import eventi.EventoS;

import java.util.ArrayList;
import java.util.List;

public class EventDispatcher {

    private final List<EventListener> listeners = new ArrayList<>();

    public void registra(EventListener listener) {
        listeners.add(listener);
    }

    public void dispatch(EventoS evento) {
        for (EventListener listener : listeners) {
            listener.onEvento(evento);
        }
    }
}