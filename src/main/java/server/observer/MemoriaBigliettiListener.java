package observer;

import eventi.*;
import persistence.MemoriaBiglietti;

public class MemoriaBigliettiListener implements EventListener {

    private final MemoriaBiglietti memoria;

    public MemoriaBigliettiListener(MemoriaBiglietti memoria) {
        this.memoria = memoria;
    }

    @Override
    public void onEvento(EventoS evento) {
        if (evento instanceof EventoGdsAcquisto e) {
            memoria.aggiungiBiglietto(e.getBiglietto());
        } else if (evento instanceof EventoGdsPrenotaz e) {
            memoria.aggiungiBiglietto(e.getBiglietto());
        } else if (evento instanceof EventoGdsModifica e) {
            memoria.aggiungiBiglietto(e.getModificato()); // oppure gestisci aggiornamento
        }
    }
}