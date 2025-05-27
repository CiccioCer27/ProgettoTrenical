package observer;

import eventi.Evento;

public interface Observer {
    void aggiorna(Evento evento);
}