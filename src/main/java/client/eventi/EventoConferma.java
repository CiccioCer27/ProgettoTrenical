package eventi;

import dto.BigliettoDTO;

public class EventoConferma implements Evento {
    private final BigliettoDTO biglietto;

    public EventoConferma(BigliettoDTO biglietto) {
        this.biglietto = biglietto;
    }

    @Override
    public BigliettoDTO getBigliettoNuovo() {
        return biglietto;
    }
}