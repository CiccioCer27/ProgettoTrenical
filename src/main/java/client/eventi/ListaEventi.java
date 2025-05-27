package eventi;

import observer.Observer;
import java.util.ArrayList;
import java.util.List;

public class ListaEventi {
    private static final ListaEventi instance = new ListaEventi();
    private final List<Observer> observers = new ArrayList<>();

    private ListaEventi() {}

    public static ListaEventi getInstance() {
        return instance;
    }

    public void aggiungiObserver(Observer o) {
        observers.add(o);
    }

    public void notifica(Evento evento) {
        for (Observer o : observers) {
            o.aggiorna(evento);
        }
    }
}