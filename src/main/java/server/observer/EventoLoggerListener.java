package observer;

import eventi.EventoS;

public class EventoLoggerListener implements EventListener {
    @Override
    public void onEvento(EventoS evento) {
        System.out.println("📩 Ricevuto evento: " + evento.getTipo());
    }
}