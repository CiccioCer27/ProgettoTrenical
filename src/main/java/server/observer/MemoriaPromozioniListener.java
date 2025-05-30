package observer;

import eventi.EventoPromoTratta;
import eventi.EventoPromoGen;
import eventi.EventoPromoFedelta;
import eventi.EventoS;
import persistence.MemoriaPromozioni;

public class MemoriaPromozioniListener implements EventListener {

    private final MemoriaPromozioni memoria;

    public MemoriaPromozioniListener(MemoriaPromozioni memoria) {
        this.memoria = memoria;
    }

    @Override
    public void onEvento(EventoS evento) {
        if (evento instanceof EventoPromoTratta e) {
            memoria.aggiungiPromozione(e.getPromozione());
        } else if (evento instanceof EventoPromoGen e) {
            memoria.aggiungiPromozione(e.getPromozione());
        } else if (evento instanceof EventoPromoFedelta e) {
            memoria.aggiungiPromozione(e.getPromozione());
        }
    }
}