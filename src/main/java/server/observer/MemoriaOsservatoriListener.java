package observer;

import eventi.EventoModificaTratta;
import eventi.EventoS;
import persistence.MemoriaOsservatori;

public class MemoriaOsservatoriListener implements EventListener {

    private final MemoriaOsservatori memoria;

    public MemoriaOsservatoriListener(MemoriaOsservatori memoria) {
        this.memoria = memoria;
    }

    @Override
    public void onEvento(EventoS evento) {
        if (evento instanceof EventoModificaTratta e) {
            // Esempio: notifica gli utenti che seguono la tratta
            var utenti = memoria.getOsservatori(e.getIdTratta());
            utenti.forEach(u -> System.out.println("👀 Utente " + u + " ha ricevuto notifica su modifica tratta"));
        }
    }
}