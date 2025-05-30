package observer;

import eventi.EventoModificaTratta;
import eventi.EventoS;
import model.Tratta;
import persistence.MemoriaTratte;

public class NotificaTrattaListener implements EventListener {

    private final NotificaDispatcher dispatcher;
    private final MemoriaTratte memoriaTratte;

    public NotificaTrattaListener(NotificaDispatcher dispatcher, MemoriaTratte memoriaTratte) {
        this.dispatcher = dispatcher;
        this.memoriaTratte = memoriaTratte;
    }

    @Override
    public void onEvento(EventoS evento) {
        if (evento instanceof EventoModificaTratta e) {
            Tratta tratta = memoriaTratte.getTrattaById(e.getIdTratta());
            if (tratta != null) {
                String messaggio = "Tratta " + tratta.getStazionePartenza() + " → " + tratta.getStazioneArrivo() + " modificata!";
                dispatcher.inviaNotifica(e.getIdTratta(), messaggio);
            }
        }
    }
}