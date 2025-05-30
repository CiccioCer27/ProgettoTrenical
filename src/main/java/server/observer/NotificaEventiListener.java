package observer;

import eventi.*;
import model.Tratta;
import persistence.MemoriaTratte;

import java.util.UUID;

/**
 * Listener centralizzato per notifiche gRPC.
 * Intercetta eventi rilevanti e invia notifiche ai client registrati.
 */
public class NotificaEventiListener implements EventListener {

    private final NotificaDispatcher dispatcher;
    private final MemoriaTratte memoriaTratte;

    public NotificaEventiListener(NotificaDispatcher dispatcher, MemoriaTratte memoriaTratte) {
        this.dispatcher = dispatcher;
        this.memoriaTratte = memoriaTratte;
    }

    @Override
    public void onEvento(EventoS evento) {
        UUID trattaId = null;
        String messaggio = null;

        if (evento instanceof EventoModificaTratta e) {
            trattaId = e.getIdTratta();
            Tratta tratta = memoriaTratte.getTrattaById(trattaId);
            if (tratta != null) {
                messaggio = "🛠 Tratta modificata: " + tratta.getStazionePartenza() + " → " + tratta.getStazioneArrivo();
            }

        } else if (evento instanceof EventoPromoTratta e) {
            String messaggioPromo = "🎉 Promozione attiva: " + e.getPromozione().getNome();
            for (UUID trattaDestinata : e.getPromozione().getTratteDestinate()) {
                dispatcher.inviaNotifica(trattaDestinata, messaggioPromo);
            }
            return; // Evita l'invio generico sotto

        } else if (evento instanceof EventoGdsModifica e) {
            trattaId = e.getOriginale().getIdTratta();
            messaggio = "📢 Biglietto aggiornato per una tratta.";

        } else if (evento instanceof EventoGdsPrenotaz e) {
            trattaId = e.getBiglietto().getIdTratta();
            messaggio = "📩 Hai prenotato un biglietto per una tratta.";

        } else if (evento instanceof EventoGdsAcquisto e) {
            trattaId = e.getBiglietto().getIdTratta();
            messaggio = "🎫 Hai acquistato un biglietto per una tratta.";
        }

        if (trattaId != null && messaggio != null) {
            dispatcher.inviaNotifica(trattaId, messaggio);
        }
    }
}