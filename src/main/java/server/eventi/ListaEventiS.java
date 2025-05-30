package eventi;

import observer.EventListener;

import java.util.ArrayList;
import java.util.List;

public class ListaEventiS {

    private static final ListaEventiS instance = new ListaEventiS();
    private final List<EventListener> listeners = new ArrayList<>();

    private ListaEventiS() {}

    public static ListaEventiS getInstance() {
        return instance;
    }

    public void aggiungi(EventListener listener) {
        listeners.add(listener);
    }

    public void notifica(EventoS evento) {
        for (EventListener l : listeners) {
            l.onEvento(evento);
        }
    }
}