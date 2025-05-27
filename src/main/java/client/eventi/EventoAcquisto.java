package eventi;

import dto.BigliettoDTO;

public class EventoAcquisto implements Evento {
    private final BigliettoDTO biglietto;

    public EventoAcquisto(BigliettoDTO biglietto) {
        this.biglietto = biglietto;
    }

    @Override
    public BigliettoDTO getBigliettoNuovo() {
        return biglietto;
    }
}